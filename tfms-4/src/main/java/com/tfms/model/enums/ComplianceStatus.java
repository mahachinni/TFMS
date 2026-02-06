package com.tfms.model.enums;

/**
 * Compliance Status Enum
 */
public enum ComplianceStatus {
    PENDING("Pending Review"),
    COMPLIANT("Compliant"),
    NON_COMPLIANT("Non-Compliant"),
    UNDER_REVIEW("Under Review"),
    ESCALATED("Escalated");
    
    private final String displayName;
    
    ComplianceStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
