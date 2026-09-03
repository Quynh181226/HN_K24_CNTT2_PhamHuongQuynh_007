package vn.rikkei.exam.restaurantreservation.service.rag;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final ResourceLoader resourceLoader;

    @PostConstruct
    public void ingest() {
        try {
            Resource resource = resourceLoader.getResource("classpath:tai_lieu_noi_bo.md");
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            // Chunk theo mục (header ## )
            List<Document> chunks = new ArrayList<>();
            String currentSection = "general";
            StringBuilder currentContent = new StringBuilder();

            for (String line : lines) {
                if (line.startsWith("## ")) {
                    // Lưu chunk trước nếu có nội dung
                    if (currentContent.length() > 0) {
                        chunks.add(buildChunk(currentSection, currentContent.toString().trim()));
                    }
                    currentSection = line.substring(3).trim();
                    currentContent = new StringBuilder();
                    currentContent.append(line).append("\n");
                } else {
                    currentContent.append(line).append("\n");
                }
            }
            // Lưu chunk cuối
            if (currentContent.length() > 0) {
                chunks.add(buildChunk(currentSection, currentContent.toString().trim()));
            }

            if (chunks.isEmpty()) {
                log.warn("Không có chunk nào được tạo từ tai_lieu_noi_bo.md");
                return;
            }

            // Chống nạp trùng bằng ID deterministic (content hash)
            List<Document> toAdd = new ArrayList<>();
            for (Document doc : chunks) {
                String contentHash = String.valueOf(doc.getText().hashCode() & 0xFFFFFFFFL);
                String docId = "tai-lieu-" + contentHash;
                doc.getMetadata().put("id", docId);
                toAdd.add(doc);
            }

            vectorStore.add(toAdd);
            log.info("[RAG] Ingested {} chunks from tai_lieu_noi_bo.md", toAdd.size());
            for (Document d : toAdd) {
                log.info("[RAG] Chunk: section={} length={}", d.getMetadata().get("section"), d.getText().length());
            }

        } catch (Exception e) {
            log.error("[RAG] Ingest lỗi: {}", e.getMessage(), e);
        }
    }

    private Document buildChunk(String section, String content) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "tai_lieu_noi_bo.md");
        metadata.put("section", section);
        return new Document(content, metadata);
    }

    public String getContext(String query) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(3).build());
            if (docs.isEmpty()) return "";
            return docs.stream().map(Document::getText).collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.warn("[RAG] getContext lỗi: {}", e.getMessage());
            return "";
        }
    }

    public List<String> getSources(String query) {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(3).build());
            return docs.stream()
                    .map(d -> {
                        String section = (String) d.getMetadata().getOrDefault("section", "");
                        String source = (String) d.getMetadata().getOrDefault("source", "tai_lieu_noi_bo.md");
                        if (section != null && !section.isBlank() && !section.equals("general")) {
                            return source + "#" + section;
                        }
                        return source;
                    })
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[RAG] getSources lỗi: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}