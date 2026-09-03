package vn.rikkei.exam.restaurantreservation.service.chat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LangfuseService {

    @Value("${langfuse.public-key:}")
    private String publicKey;

    @Value("${langfuse.secret-key:}")
    private String secretKey;

    @Value("${langfuse.host:http://localhost:3000}")
    private String host;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void trace(String conversationId, String userMessage, String answer,
                      List<String> sources, List<String> toolsUsed) {
        if (publicKey == null || publicKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.info("[Langfuse] Key chưa cấu hình, bỏ qua trace");
            return;
        }
        try {
            String traceId = UUID.randomUUID().toString();
            String sourcesJson = toJsonArray(sources);
            String toolsJson = toJsonArray(toolsUsed);

            // Build JSON body thủ công (không dùng thư viện nặng)
            String body = "{"
                    + "\"id\":\"" + traceId + "\","
                    + "\"name\":\"restaurant-chat\","
                    + "\"metadata\":{"
                    + "\"conversationId\":\"" + conversationId + "\","
                    + "\"examCode\":\"DE-007\","
                    + "\"toolsUsed\":" + toolsJson + ","
                    + "\"sources\":" + sourcesJson
                    + "},"
                    + "\"input\":\"" + escapeJson(userMessage) + "\","
                    + "\"output\":\"" + escapeJson(answer) + "\""
                    + "}";

            // Basic auth: base64(publicKey:secretKey)
            String credentials = Base64.getEncoder().encodeToString(
                    (publicKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(host + "/api/public/traces"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + credentials)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[Langfuse] Trace gửi thành công - traceId={} status={}", traceId, response.statusCode());

        } catch (Exception e) {
            // Không log secret key, chỉ log lỗi chung
            log.warn("[Langfuse] Gửi trace thất bại: {}", e.getMessage());
        }
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
