package com.tfms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_change_audit")
public class StatusChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType; // e.g., "LC" or "BG"
    private String referenceNumber;
    private String fromStatus;
    private String toStatus;
    private String changedBy;
    private LocalDateTime changedAt;

    public StatusChangeAudit() {}

    public StatusChangeAudit(String entityType, String referenceNumber, String fromStatus, String toStatus, String changedBy) {
        this.entityType = entityType;
        this.referenceNumber = referenceNumber;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
