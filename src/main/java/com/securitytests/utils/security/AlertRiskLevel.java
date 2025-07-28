package com.securitytests.utils.security;

/**
 * Enum representing security alert risk levels
 */
public enum AlertRiskLevel {
    INFORMATIONAL(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);
    
    private final int level;
    
    AlertRiskLevel(int level) {
        this.level = level;
    }
    
    /**
     * Get the numeric level value
     * @return Numeric level (higher is more severe)
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Convert a string risk level to enum
     * @param riskString Risk level as string
     * @return Matching enum value, defaults to INFORMATIONAL if no match
     */
    public static AlertRiskLevel fromString(String riskString) {
        if (riskString == null) return INFORMATIONAL;
        
        switch (riskString.toLowerCase()) {
            case "informational":
                return INFORMATIONAL;
            case "low":
                return LOW;
            case "medium":
                return MEDIUM;
            case "high":
                return HIGH;
            case "critical":
                return CRITICAL;
            default:
                return INFORMATIONAL;
        }
    }
    
    @Override
    public String toString() {
        return name();
    }
}
