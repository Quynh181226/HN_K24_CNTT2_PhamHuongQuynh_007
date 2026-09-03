package vn.rikkei.exam.restaurantreservation.dto;

import java.util.List;

public class ChatResponse {
    private String answer;
    private String conversationId;
    private List<String> sources;
    private List<String> toolsUsed;

    public ChatResponse() {}

    public ChatResponse(String answer, String conversationId, List<String> sources, List<String> toolsUsed) {
        this.answer = answer;
        this.conversationId = conversationId;
        this.sources = sources;
        this.toolsUsed = toolsUsed;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public List<String> getToolsUsed() { return toolsUsed; }
    public void setToolsUsed(List<String> toolsUsed) { this.toolsUsed = toolsUsed; }
}
