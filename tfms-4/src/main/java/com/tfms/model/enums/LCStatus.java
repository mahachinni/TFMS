package com.tfms.model.enums;

/**
 * Letter of Credit Status Enum
 */
public enum LCStatus {
    DRAFT("Draft"),
    OPEN("Open"),
    SUBMITTED("Submitted"),
    UNDER_VERIFICATION("Under Verification"),
    SENT_TO_RISK("Sent to Risk"),
    AMENDED("Amended"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    CLOSED("Closed");
    
    private final String displayName;
    
    LCStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
