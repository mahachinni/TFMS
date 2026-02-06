package com.tfms.model.enums;

/**
 * Business Role - represents the actual business function
 * This is used for display purposes and business logic differentiation
 */
public enum BusinessRole {
    IMPORTER("Importer", "Buyer who requests LC and uploads purchase orders"),
    EXPORTER("Exporter", "Seller who uploads shipping documents and tracks payment"),
    BANK_STAFF("Bank Staff", "Officer who approves requests, issues LC, manages guarantees"),
    RISK_ANALYST("Risk Analyst", "Focuses on risk assessment and scoring");
    
    private final String displayName;
    private final String description;
    
    BusinessRole(String displayName, String description) {
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
