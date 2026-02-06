package com.tfms.model.enums;

/**
 * System Role Enum
 * CUSTOMER - Covers both Importer and Exporter (bank customers)
 * OFFICER - Bank staff who manage approvals, issuance, compliance
 * RISK - Risk analysts who use the Risk Assessment module
 */
public enum Role {
    CUSTOMER("Customer", "Importer/Exporter - Bank Customer"),
    OFFICER("Officer", "Bank Staff - Approvals, Issuance, Compliance"),
    RISK("Risk Analyst", "Risk Assessment Specialist");
    
    private final String displayName;
    private final String description;
    
    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}
