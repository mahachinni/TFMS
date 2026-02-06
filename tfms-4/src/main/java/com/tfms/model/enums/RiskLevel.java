package com.tfms.model.enums;

/**
 * Risk Level Enum
 */
public enum RiskLevel {
    LOW("Low", "#28a745"),
    MEDIUM("Medium", "#ffc107"),
    HIGH("High", "#fd7e14"),
    CRITICAL("Critical", "#dc3545");
    
    private final String displayName;
    private final String colorCode;
    
    RiskLevel(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
}
