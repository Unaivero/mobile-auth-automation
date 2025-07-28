package com.securitytests.utils.biometric;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Result class for biometric enrollment operations
 */
public class BiometricEnrollmentResult {
    private final boolean success;
    private final String message;
    private UUID biometricId;
    private final LocalDateTime timestamp;
    private BiometricType biometricType;
    private int enrollmentProgress;
    private Map<String, Object> enrollmentData;
    
    public BiometricEnrollmentResult(boolean success, String message, UUID biometricId) {
        this.success = success;
        this.message = message;
        this.biometricId = biometricId;
        this.timestamp = LocalDateTime.now();
        this.enrollmentProgress = success ? 100 : 0;
        this.enrollmentData = new HashMap<>();
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public UUID getBiometricId() {
        return biometricId;
    }
    
    public void setBiometricId(UUID biometricId) {
        this.biometricId = biometricId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public BiometricType getBiometricType() {
        return biometricType;
    }
    
    public void setBiometricType(BiometricType biometricType) {
        this.biometricType = biometricType;
    }
    
    public int getEnrollmentProgress() {
        return enrollmentProgress;
    }
    
    public void setEnrollmentProgress(int enrollmentProgress) {
        this.enrollmentProgress = Math.max(0, Math.min(100, enrollmentProgress));
    }
    
    public Map<String, Object> getEnrollmentData() {
        return enrollmentData;
    }
    
    public void addEnrollmentData(String key, Object value) {
        this.enrollmentData.put(key, value);
    }
    
    @Override
    public String toString() {
        return String.format("BiometricEnrollmentResult{success=%s, message='%s', biometricId=%s, progress=%d%%}",
            success, message, biometricId, enrollmentProgress);
    }
}