package com.tfms.model.enums;

/**
 * Document Status Enum
 */
public enum DocumentStatus {
    ACTIVE("Active"),
    PENDING_REVIEW("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    ARCHIVED("Archived");
    
    private final String displayName;
    
    DocumentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
