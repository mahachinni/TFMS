package com.tfms.model.enums;

/**
 * Bank Guarantee Status Enum
 */
public enum GuaranteeStatus {
    DRAFT("Draft"),
    PENDING("Pending"),
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    SENT_TO_RISK("Sent to Risk"),
    ISSUED("Issued"),
    ACTIVE("Active"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled"),
    CLAIMED("Claimed");
    
    private final String displayName;
    
    GuaranteeStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
