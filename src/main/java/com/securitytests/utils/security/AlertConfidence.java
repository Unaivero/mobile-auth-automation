package com.securitytests.utils.security;

/**
 * Enum representing security alert confidence levels
 */
public enum AlertConfidence {
    FALSE_POSITIVE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CONFIRMED(4);
    
    private final int level;
    
    AlertConfidence(int level) {
        this.level = level;
    }
    
    /**
     * Get the numeric confidence value
     * @return Numeric level (higher is more confident)
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Convert a string confidence level to enum
     * @param confidenceString Confidence level as string
     * @return Matching enum value, defaults to LOW if no match
     */
    public static AlertConfidence fromString(String confidenceString) {
        if (confidenceString == null) return LOW;
        
        switch (confidenceString.toLowerCase()) {
            case "false positive":
            case "falsepositive":
                return FALSE_POSITIVE;
            case "low":
                return LOW;
            case "medium":
                return MEDIUM;
            case "high":
                return HIGH;
            case "confirmed":
                return CONFIRMED;
            default:
                return LOW;
        }
    }
    
    @Override
    public String toString() {
        return name();
    }
}
