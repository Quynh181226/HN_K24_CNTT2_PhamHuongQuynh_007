package vn.rikkei.exam.restaurantreservation.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.rikkei.exam.restaurantreservation.tool.ReservationTools;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder,
                                 ChatMemory chatMemory,
                                 ReservationTools reservationTools) {
        return builder
                .defaultSystem("""
                        Bạn là trợ lý đặt bàn nhà hàng thông minh.
                        Chỉ trả lời dựa trên tài liệu nội bộ và Tool được cung cấp.
                        Dữ liệu nghiệp vụ (kiểm tra slot, tạo đặt bàn) phải gọi Tool - không được tự suy đoán.
                        Nếu không có căn cứ trong tài liệu nội bộ và không có Tool phù hợp thì trả lời: Không đủ căn cứ trong tài liệu nội bộ.
                        Trả lời bằng tiếng Việt, ngắn gọn và rõ ràng.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(reservationTools)
                .build();
    }
}
