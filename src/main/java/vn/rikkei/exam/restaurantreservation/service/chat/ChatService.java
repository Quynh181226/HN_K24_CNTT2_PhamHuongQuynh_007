package vn.rikkei.exam.restaurantreservation.service.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import vn.rikkei.exam.restaurantreservation.service.rag.RagService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final LangfuseService langfuseService;
    private final ChatMemory chatMemory;

    public vn.rikkei.exam.restaurantreservation.dto.ChatResponse ask(String message, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // Lấy context RAG
        String context = ragService.getContext(message);
        List<String> sources = ragService.getSources(message);

        // Xây system prompt có context RAG
        String systemWithContext;
        if (context != null && !context.isBlank()) {
            systemWithContext = """
                    Bạn là trợ lý đặt bàn nhà hàng thông minh.
                    Chỉ trả lời dựa trên tài liệu nội bộ và Tool được cung cấp.
                    Dữ liệu nghiệp vụ (kiểm tra slot, tạo đặt bàn) phải gọi Tool - không được tự suy đoán.
                    Nếu không có căn cứ trong tài liệu nội bộ và không có Tool phù hợp thì trả lời: Không đủ căn cứ trong tài liệu nội bộ.
                    Trả lời bằng tiếng Việt, ngắn gọn và rõ ràng.
                    
                    Tài liệu nội bộ liên quan:
                    """ + context;
        } else {
            systemWithContext = """
                    Bạn là trợ lý đặt bàn nhà hàng thông minh.
                    Chỉ trả lời dựa trên tài liệu nội bộ và Tool được cung cấp.
                    Dữ liệu nghiệp vụ (kiểm tra slot, tạo đặt bàn) phải gọi Tool - không được tự suy đoán.
                    Nếu không có căn cứ trong tài liệu nội bộ và không có Tool phù hợp thì trả lời: Không đủ căn cứ trong tài liệu nội bộ.
                    Trả lời bằng tiếng Việt, ngắn gọn và rõ ràng.
                    """;
        }

        String finalConversationId = conversationId;

        // Gọi ChatClient với memory per conversationId
        ChatResponse chatResponse = chatClient.prompt()
                .system(systemWithContext)
                .user(message)
                .advisors(a -> a.param("chat_memory_conversation_id", finalConversationId))
                .call()
                .chatResponse();

        String answer = chatResponse != null && chatResponse.getResult() != null
                ? chatResponse.getResult().getOutput().getText()
                : "Không đủ căn cứ trong tài liệu nội bộ.";

        // Lấy toolsUsed từ metadata của response
        List<String> toolsUsed = new ArrayList<>();
        try {
            if (chatResponse != null && chatResponse.getMetadata() != null) {
                Object toolsObj = chatResponse.getMetadata().get("toolsUsed");
                if (toolsObj instanceof List<?> list) {
                    for (Object item : list) {
                        toolsUsed.add(item.toString());
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: kiểm tra text answer có tool result không
        }

        // Fallback answer khi không có context và không dùng tool
        if (answer == null || answer.isBlank()) {
            answer = "Không đủ căn cứ trong tài liệu nội bộ.";
        }

        log.info("[Chat] conversationId={} toolsUsed={} sources={}", conversationId, toolsUsed, sources);

        // Gửi trace Langfuse
        langfuseService.trace(conversationId, message, answer, sources, toolsUsed);

        return new vn.rikkei.exam.restaurantreservation.dto.ChatResponse(
                answer, conversationId, sources, toolsUsed);
    }
}