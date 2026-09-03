package vn.rikkei.exam.restaurantreservation.dto;

public class OperationsRequest {
    private String requestId;
    private String decision;
    private String note;

    public OperationsRequest() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
