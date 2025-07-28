package com.securitytests.utils.biometric;

/**
 * Enumeration of supported biometric authentication types
 */
public enum BiometricType {
    FINGERPRINT("fingerprint", "Fingerprint Authentication"),
    FACE_ID("face_id", "Face ID Authentication"),
    VOICE("voice", "Voice Authentication");
    
    private final String identifier;
    private final String displayName;
    
    BiometricType(String identifier, String displayName) {
        this.identifier = identifier;
        this.displayName = displayName;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return identifier;
    }
    
    public static BiometricType fromString(String identifier) {
        for (BiometricType type : values()) {
            if (type.identifier.equals(identifier)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown biometric type: " + identifier);
    }
}