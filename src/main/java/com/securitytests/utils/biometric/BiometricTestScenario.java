package com.securitytests.utils.biometric;

/**
 * Enumeration of biometric test scenarios for comprehensive testing
 */
public enum BiometricTestScenario {
    // Positive scenarios  
    VALID_BIOMETRIC("Valid biometric authentication"),
    
    // Authentication failure scenarios
    INVALID_BIOMETRIC("Invalid or unrecognized biometric"),
    SENSOR_ERROR("Biometric sensor error"),
    TIMEOUT("Authentication timeout"),
    TOO_MANY_ATTEMPTS("Too many failed attempts"),
    HARDWARE_UNAVAILABLE("Biometric hardware unavailable"),
    
    // Fingerprint-specific scenarios
    DIRTY_SENSOR("Dirty fingerprint sensor"),
    PARTIAL_FINGERPRINT("Partial fingerprint scan"),
    WET_FINGER("Wet finger detection"),
    
    // Face ID-specific scenarios
    POOR_LIGHTING("Poor lighting conditions"),
    FACE_OBSTRUCTION("Face partially obstructed"),
    MULTIPLE_FACES("Multiple faces detected"),
    CAMERA_BLOCKED("Camera blocked or dirty"),
    FACE_TOO_FAR("Face too far from camera"),
    FACE_TOO_CLOSE("Face too close to camera"),
    EYES_CLOSED("Eyes closed during scan"),
    
    // Voice-specific scenarios
    BACKGROUND_NOISE("Background noise interference"),
    MICROPHONE_UNAVAILABLE("Microphone unavailable"),
    VOICE_TOO_QUIET("Voice input too quiet"),
    VOICE_TOO_LOUD("Voice input too loud"),
    WRONG_LANGUAGE("Unsupported language"),
    VOICE_DISTORTION("Voice distortion detected"),
    
    // Security testing scenarios
    PRESENTATION_ATTACK("Presentation attack (spoofing)"),
    TEMPLATE_INJECTION("Template injection attack"),
    REPLAY_ATTACK("Replay attack"),
    BRUTE_FORCE_ATTACK("Brute force attack"),
    
    // Environmental scenarios
    DEVICE_MOVEMENT("Device movement during scan"),
    LOW_BATTERY("Low battery affecting sensors"),
    HIGH_TEMPERATURE("High temperature affecting hardware"),
    NETWORK_INTERRUPTION("Network interruption during cloud verification");
    
    private final String description;
    
    BiometricTestScenario(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this scenario is applicable to the given biometric type
     */
    public boolean isApplicableTo(BiometricType biometricType) {
        return switch (this) {
            // Universal scenarios
            case VALID_BIOMETRIC, INVALID_BIOMETRIC, SENSOR_ERROR, TIMEOUT, 
                 TOO_MANY_ATTEMPTS, HARDWARE_UNAVAILABLE, PRESENTATION_ATTACK,
                 TEMPLATE_INJECTION, REPLAY_ATTACK, BRUTE_FORCE_ATTACK,
                 DEVICE_MOVEMENT, LOW_BATTERY, HIGH_TEMPERATURE, NETWORK_INTERRUPTION -> true;
                 
            // Fingerprint-specific
            case DIRTY_SENSOR, PARTIAL_FINGERPRINT, WET_FINGER -> 
                biometricType == BiometricType.FINGERPRINT;
                
            // Face ID-specific  
            case POOR_LIGHTING, FACE_OBSTRUCTION, MULTIPLE_FACES, CAMERA_BLOCKED,
                 FACE_TOO_FAR, FACE_TOO_CLOSE, EYES_CLOSED ->
                biometricType == BiometricType.FACE_ID;
                
            // Voice-specific
            case BACKGROUND_NOISE, MICROPHONE_UNAVAILABLE, VOICE_TOO_QUIET,
                 VOICE_TOO_LOUD, WRONG_LANGUAGE, VOICE_DISTORTION ->
                biometricType == BiometricType.VOICE;
        };
    }
    
    /**
     * Get expected result type for this scenario
     */
    public BiometricAuthResult.ErrorType getExpectedErrorType() {
        return switch (this) {
            case VALID_BIOMETRIC -> null; // Success case
            
            case INVALID_BIOMETRIC, PARTIAL_FINGERPRINT, FACE_OBSTRUCTION,
                 MULTIPLE_FACES, VOICE_TOO_QUIET, VOICE_TOO_LOUD, EYES_CLOSED ->
                BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED;
                
            case SENSOR_ERROR, HARDWARE_UNAVAILABLE, CAMERA_BLOCKED,
                 MICROPHONE_UNAVAILABLE, LOW_BATTERY ->
                BiometricAuthResult.ErrorType.HARDWARE_ERROR;
                
            case TIMEOUT -> BiometricAuthResult.ErrorType.TIMEOUT;
            
            case TOO_MANY_ATTEMPTS -> BiometricAuthResult.ErrorType.RATE_LIMITED;
            
            case DIRTY_SENSOR, WET_FINGER -> BiometricAuthResult.ErrorType.SENSOR_DIRTY;
            
            case POOR_LIGHTING, BACKGROUND_NOISE, FACE_TOO_FAR, FACE_TOO_CLOSE,
                 DEVICE_MOVEMENT, HIGH_TEMPERATURE, VOICE_DISTORTION ->
                BiometricAuthResult.ErrorType.ENVIRONMENTAL_ERROR;
                
            case PRESENTATION_ATTACK, TEMPLATE_INJECTION, REPLAY_ATTACK,
                 BRUTE_FORCE_ATTACK -> BiometricAuthResult.ErrorType.SECURITY_BLOCKED;
                
            case WRONG_LANGUAGE, NETWORK_INTERRUPTION ->
                BiometricAuthResult.ErrorType.CONFIGURATION_ERROR;
        };
    }
    
    /**
     * Get scenarios applicable to a specific biometric type
     */
    public static BiometricTestScenario[] getScenariosFor(BiometricType biometricType) {
        return java.util.Arrays.stream(values())
            .filter(scenario -> scenario.isApplicableTo(biometricType))
            .toArray(BiometricTestScenario[]::new);
    }
    
    /**
     * Get security-focused test scenarios
     */
    public static BiometricTestScenario[] getSecurityScenarios() {
        return new BiometricTestScenario[] {
            PRESENTATION_ATTACK,
            TEMPLATE_INJECTION,
            REPLAY_ATTACK,
            BRUTE_FORCE_ATTACK,
            TOO_MANY_ATTEMPTS
        };
    }
    
    /**
     * Get environmental/edge case scenarios
     */
    public static BiometricTestScenario[] getEnvironmentalScenarios() {
        return new BiometricTestScenario[] {
            POOR_LIGHTING,
            BACKGROUND_NOISE,
            DEVICE_MOVEMENT,
            LOW_BATTERY,
            HIGH_TEMPERATURE,
            NETWORK_INTERRUPTION
        };
    }
}