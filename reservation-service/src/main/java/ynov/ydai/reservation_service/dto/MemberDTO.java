package ynov.ydai.reservation_service.dto;

public class MemberDTO {
    private Long id;
    private String fullName;
    private boolean suspended;
    private Integer maxConcurrentBookings;

    public MemberDTO() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
    public Integer getMaxConcurrentBookings() { return maxConcurrentBookings; }
    public void setMaxConcurrentBookings(Integer maxConcurrentBookings) { this.maxConcurrentBookings = maxConcurrentBookings; }
}
