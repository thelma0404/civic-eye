package com.civic.admin;

import com.civic.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminChatbotController {

    private final AdminChatbotService chatbotService;

    public AdminChatbotController(AdminChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    public record ChatRequest(String message) {
    }

    @PostMapping("/api/admin/chatbot")
    public ApiResponse<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        return ApiResponse.ok(chatbotService.reply(request == null ? null : request.message()));
    }
}
