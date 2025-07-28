package com.securitytests.utils.biometric;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Result class for biometric authentication test attempts
 */
public class BiometricAuthResult {
    private final boolean success;
    private final String message;
    private final ErrorType errorType;
    private final LocalDateTime timestamp;
    private BiometricTestScenario scenario;
    private BiometricType biometricType;
    private long responseTimeMs;
    private Map<String, Object> additionalData;
    
    public BiometricAuthResult(boolean success, String message, ErrorType errorType) {
        this.success = success;
        this.message = message;
        this.errorType = errorType;
        this.timestamp = LocalDateTime.now();
        this.additionalData = new HashMap<>();
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public BiometricTestScenario getScenario() {
        return scenario;
    }
    
    public void setScenario(BiometricTestScenario scenario) {
        this.scenario = scenario;
    }
    
    public BiometricType getBiometricType() {
        return biometricType;
    }
    
    public void setBiometricType(BiometricType biometricType) {
        this.biometricType = biometricType;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }
    
    public void addAdditionalData(String key, Object value) {
        this.additionalData.put(key, value);
    }
    
    /**
     * Check if the result matches the expected outcome for a scenario
     */
    public boolean matchesExpectedOutcome(BiometricTestScenario expectedScenario) {
        if (expectedScenario == BiometricTestScenario.VALID_BIOMETRIC) {
            return success;
        } else {
            ErrorType expectedErrorType = expectedScenario.getExpectedErrorType();
            return !success && (errorType == expectedErrorType || expectedErrorType == null);
        }
    }
    
    /**
     * Get a detailed description of the result
     */
    public String getDetailedDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Biometric Authentication Result: ");
        description.append(success ? "SUCCESS" : "FAILURE");
        
        if (biometricType != null) {
            description.append(" [").append(biometricType.getDisplayName()).append("]");
        }
        
        if (scenario != null) {
            description.append(" - Scenario: ").append(scenario.getDescription());
        }
        
        description.append(" - Message: ").append(message);
        
        if (errorType != null) {
            description.append(" - Error Type: ").append(errorType);
        }
        
        if (responseTimeMs > 0) {
            description.append(" - Response Time: ").append(responseTimeMs).append("ms");
        }
        
        description.append(" - Timestamp: ").append(timestamp);
        
        return description.toString();
    }
    
    /**
     * Convert to a map for database storage or reporting
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("error_type", errorType != null ? errorType.toString() : null);
        map.put("timestamp", timestamp.toString());
        map.put("scenario", scenario != null ? scenario.toString() : null);
        map.put("biometric_type", biometricType != null ? biometricType.toString() : null);
        map.put("response_time_ms", responseTimeMs);
        map.put("additional_data", additionalData);
        return map;
    }
    
    @Override
    public String toString() {
        return String.format("BiometricAuthResult{success=%s, message='%s', errorType=%s, scenario=%s, responseTime=%dms}", 
            success, message, errorType, scenario, responseTimeMs);
    }
    
    /**
     * Enumeration of biometric authentication error types
     */
    public enum ErrorType {
        AUTHENTICATION_FAILED("Authentication failed - biometric not recognized"),
        HARDWARE_ERROR("Hardware error - sensor or device issue"),
        TIMEOUT("Timeout - authentication took too long"),
        RATE_LIMITED("Rate limited - too many attempts"),
        SENSOR_DIRTY("Sensor dirty - needs cleaning"),
        ENVIRONMENTAL_ERROR("Environmental error - conditions not suitable"),
        SECURITY_BLOCKED("Security blocked - potential attack detected"),
        CONFIGURATION_ERROR("Configuration error - system misconfiguration"),
        UI_ERROR("UI error - interface problem"),
        EXECUTION_ERROR("Execution error - test framework issue");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * Get severity level for reporting and alerting
         */
        public SeverityLevel getSeverityLevel() {
            return switch (this) {
                case SECURITY_BLOCKED -> SeverityLevel.CRITICAL;
                case HARDWARE_ERROR, EXECUTION_ERROR -> SeverityLevel.HIGH;
                case AUTHENTICATION_FAILED, RATE_LIMITED, TIMEOUT -> SeverityLevel.MEDIUM;
                case SENSOR_DIRTY, ENVIRONMENTAL_ERROR, UI_ERROR -> SeverityLevel.LOW;
                case CONFIGURATION_ERROR -> SeverityLevel.MEDIUM;
            };
        }
    }
    
    /**
     * Severity levels for error classification
     */
    public enum SeverityLevel {
        CRITICAL(4, "Critical"),
        HIGH(3, "High"), 
        MEDIUM(2, "Medium"),
        LOW(1, "Low");
        
        private final int level;
        private final String name;
        
        SeverityLevel(int level, String name) {
            this.level = level;
            this.name = name;
        }
        
        public int getLevel() {
            return level;
        }
        
        public String getName() {
            return name;
        }
    }
}