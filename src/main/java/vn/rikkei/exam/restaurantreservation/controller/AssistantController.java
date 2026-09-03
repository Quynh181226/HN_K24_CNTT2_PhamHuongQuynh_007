package vn.rikkei.exam.restaurantreservation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.rikkei.exam.restaurantreservation.dto.ChatRequest;
import vn.rikkei.exam.restaurantreservation.dto.ChatResponse;
import vn.rikkei.exam.restaurantreservation.service.chat.ChatService;

@Slf4j
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestBody ChatRequest request) {
        log.info("[AssistantController] Nhận câu hỏi: conversationId={}", request.getConversationId());
        ChatResponse response = chatService.ask(request.getMessage(), request.getConversationId());
        return ResponseEntity.ok(response);
    }
}
