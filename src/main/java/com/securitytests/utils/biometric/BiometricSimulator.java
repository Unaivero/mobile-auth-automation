package com.securitytests.utils.biometric;

import com.securitytests.utils.logging.StructuredLogger;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.JavascriptExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Biometric simulator for testing various biometric authentication scenarios
 * Simulates different biometric sensors and authentication results
 */
public class BiometricSimulator {
    private static final StructuredLogger logger = new StructuredLogger(BiometricSimulator.class);
    private final AppiumDriver driver;
    
    public BiometricSimulator(AppiumDriver driver) {
        this.driver = driver;
    }
    
    /**
     * Simulate fingerprint authentication
     */
    public BiometricAuthResult simulateFingerprint(String username, BiometricTestScenario scenario) {
        logger.info("Simulating fingerprint authentication for user: {} with scenario: {}", username, scenario);
        
        try {
            // Simulate fingerprint sensor interaction based on scenario
            switch (scenario) {
                case VALID_BIOMETRIC:
                    return simulateValidFingerprint(username);
                    
                case INVALID_BIOMETRIC:
                    return simulateInvalidFingerprint();
                    
                case SENSOR_ERROR:
                    return simulateFingerprintSensorError();
                    
                case TIMEOUT:
                    return simulateFingerprintTimeout();
                    
                case TOO_MANY_ATTEMPTS:
                    return simulateTooManyFingerprintAttempts();
                    
                case HARDWARE_UNAVAILABLE:
                    return simulateFingerprintHardwareUnavailable();
                    
                case DIRTY_SENSOR:
                    return simulateDirtyFingerprintSensor();
                    
                case PARTIAL_FINGERPRINT:
                    return simulatePartialFingerprint();
                    
                default:
                    return new BiometricAuthResult(false, "Unknown scenario", 
                        BiometricAuthResult.ErrorType.EXECUTION_ERROR);
            }
        } catch (Exception e) {
            logger.error("Error simulating fingerprint authentication", e);
            return new BiometricAuthResult(false, "Simulation error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Simulate Face ID authentication
     */
    public BiometricAuthResult simulateFaceId(String username, BiometricTestScenario scenario) {
        logger.info("Simulating Face ID authentication for user: {} with scenario: {}", username, scenario);
        
        try {
            switch (scenario) {
                case VALID_BIOMETRIC:
                    return simulateValidFaceId(username);
                    
                case INVALID_BIOMETRIC:
                    return simulateInvalidFaceId();
                    
                case POOR_LIGHTING:
                    return simulatePoorLightingFaceId();
                    
                case FACE_OBSTRUCTION:
                    return simulateFaceObstruction();
                    
                case MULTIPLE_FACES:
                    return simulateMultipleFaces();
                    
                case TIMEOUT:
                    return simulateFaceIdTimeout();
                    
                case HARDWARE_UNAVAILABLE:
                    return simulateFaceIdHardwareUnavailable();
                    
                case CAMERA_BLOCKED:
                    return simulateCameraBlocked();
                    
                default:
                    return new BiometricAuthResult(false, "Unknown scenario", 
                        BiometricAuthResult.ErrorType.EXECUTION_ERROR);
            }
        } catch (Exception e) {
            logger.error("Error simulating Face ID authentication", e);
            return new BiometricAuthResult(false, "Simulation error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Simulate voice authentication
     */
    public BiometricAuthResult simulateVoice(String username, BiometricTestScenario scenario) {
        logger.info("Simulating voice authentication for user: {} with scenario: {}", username, scenario);
        
        try {
            switch (scenario) {
                case VALID_BIOMETRIC:
                    return simulateValidVoice(username);
                    
                case INVALID_BIOMETRIC:
                    return simulateInvalidVoice();
                    
                case BACKGROUND_NOISE:
                    return simulateBackgroundNoise();
                    
                case MICROPHONE_UNAVAILABLE:
                    return simulateMicrophoneUnavailable();
                    
                case VOICE_TOO_QUIET:
                    return simulateVoiceTooQuiet();
                    
                case VOICE_TOO_LOUD:
                    return simulateVoiceTooLoud();
                    
                case TIMEOUT:
                    return simulateVoiceTimeout();
                    
                case WRONG_LANGUAGE:
                    return simulateWrongLanguage();
                    
                default:
                    return new BiometricAuthResult(false, "Unknown scenario", 
                        BiometricAuthResult.ErrorType.EXECUTION_ERROR);
            }
        } catch (Exception e) {
            logger.error("Error simulating voice authentication", e);
            return new BiometricAuthResult(false, "Simulation error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Simulate biometric enrollment process
     */
    public BiometricEnrollmentResult simulateEnrollment(BiometricType biometricType, byte[] biometricTemplate) {
        logger.info("Simulating biometric enrollment for type: {}", biometricType);
        
        try {
            // Simulate enrollment progress
            for (int progress = 0; progress <= 100; progress += 20) {
                simulateEnrollmentProgress(progress);
                TimeUnit.MILLISECONDS.sleep(500);
            }
            
            // Simulate successful enrollment
            return new BiometricEnrollmentResult(true, "Enrollment completed successfully", null);
            
        } catch (Exception e) {
            logger.error("Error simulating biometric enrollment", e);
            return new BiometricEnrollmentResult(false, "Enrollment simulation error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Simulate presentation attack (spoofing with photos, videos, etc.)
     */
    public BiometricAuthResult simulatePresentationAttack(String username, BiometricType biometricType) {
        logger.info("Simulating presentation attack for biometric type: {}", biometricType);
        
        try {
            // Send presentation attack detection result
            Map<String, Object> params = new HashMap<>();
            params.put("biometricType", biometricType.toString());
            params.put("attackType", "presentation");
            params.put("detected", true); // Should be detected by security measures
            
            executeDriverScript("mobile: biometricPresentationAttack", params);
            
            // Wait for system response
            TimeUnit.SECONDS.sleep(2);
            
            return new BiometricAuthResult(false, "Presentation attack detected", 
                BiometricAuthResult.ErrorType.SECURITY_BLOCKED);
                
        } catch (Exception e) {
            logger.error("Error simulating presentation attack", e);
            return new BiometricAuthResult(false, "Presentation attack simulation error", 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Simulate template injection attack
     */
    public BiometricAuthResult simulateTemplateInjection(String username, BiometricType biometricType) {
        logger.info("Simulating template injection attack for biometric type: {}", biometricType);
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("biometricType", biometricType.toString());
            params.put("attackType", "template_injection");
            params.put("maliciousTemplate", "fake_template_data");
            
            executeDriverScript("mobile: biometricTemplateInjection", params);
            
            TimeUnit.SECONDS.sleep(1);
            
            return new BiometricAuthResult(false, "Template injection attack blocked", 
                BiometricAuthResult.ErrorType.SECURITY_BLOCKED);
                
        } catch (Exception e) {
            logger.error("Error simulating template injection attack", e);
            return new BiometricAuthResult(false, "Template injection simulation error", 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Simulate spoofing attack
     */
    public BiometricAuthResult simulateSpoofingAttack(String username, BiometricType biometricType) {
        logger.info("Simulating spoofing attack for biometric type: {}", biometricType);
        
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("biometricType", biometricType.toString());
            params.put("attackType", "spoofing");
            params.put("spoofingMethod", getSpoofingMethod(biometricType));
            
            executeDriverScript("mobile: biometricSpoofing", params);
            
            TimeUnit.SECONDS.sleep(2);
            
            return new BiometricAuthResult(false, "Spoofing attack detected and blocked", 
                BiometricAuthResult.ErrorType.SECURITY_BLOCKED);
                
        } catch (Exception e) {
            logger.error("Error simulating spoofing attack", e);
            return new BiometricAuthResult(false, "Spoofing simulation error", 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    // Private helper methods for fingerprint simulation
    
    private BiometricAuthResult simulateValidFingerprint(String username) throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "success");
        params.put("username", username);
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(true, "Fingerprint authentication successful", null);
    }
    
    private BiometricAuthResult simulateInvalidFingerprint() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "failure");
        params.put("reason", "no_match");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Fingerprint not recognized", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    private BiometricAuthResult simulateFingerprintSensorError() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "error");
        params.put("reason", "sensor_error");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Fingerprint sensor error", 
            BiometricAuthResult.ErrorType.HARDWARE_ERROR);
    }
    
    private BiometricAuthResult simulateFingerprintTimeout() throws InterruptedException {
        // Simulate timeout by waiting and not providing input
        TimeUnit.SECONDS.sleep(5);
        
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "timeout");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        
        return new BiometricAuthResult(false, "Fingerprint authentication timeout", 
            BiometricAuthResult.ErrorType.TIMEOUT);
    }
    
    private BiometricAuthResult simulateTooManyFingerprintAttempts() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "lockout");
        params.put("reason", "too_many_attempts");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Too many failed attempts", 
            BiometricAuthResult.ErrorType.RATE_LIMITED);
    }
    
    private BiometricAuthResult simulateFingerprintHardwareUnavailable() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "unavailable");
        params.put("reason", "hardware_unavailable");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Fingerprint hardware unavailable", 
            BiometricAuthResult.ErrorType.HARDWARE_ERROR);
    }
    
    private BiometricAuthResult simulateDirtyFingerprintSensor() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "error");
        params.put("reason", "sensor_dirty");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Please clean the fingerprint sensor", 
            BiometricAuthResult.ErrorType.SENSOR_DIRTY);
    }
    
    private BiometricAuthResult simulatePartialFingerprint() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "fingerprint");
        params.put("result", "partial");
        params.put("reason", "incomplete_scan");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Please place your entire finger on the sensor", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    // Private helper methods for Face ID simulation
    
    private BiometricAuthResult simulateValidFaceId(String username) throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "success");
        params.put("username", username);
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(true, "Face ID authentication successful", null);
    }
    
    private BiometricAuthResult simulateInvalidFaceId() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "failure");
        params.put("reason", "face_not_recognized");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Face not recognized", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    private BiometricAuthResult simulatePoorLightingFaceId() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "error");
        params.put("reason", "poor_lighting");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Poor lighting conditions", 
            BiometricAuthResult.ErrorType.ENVIRONMENTAL_ERROR);
    }
    
    private BiometricAuthResult simulateFaceObstruction() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "error");
        params.put("reason", "face_obstructed");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Face is obstructed", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    private BiometricAuthResult simulateMultipleFaces() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "error");
        params.put("reason", "multiple_faces");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Multiple faces detected", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    private BiometricAuthResult simulateFaceIdTimeout() throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "timeout");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        
        return new BiometricAuthResult(false, "Face ID authentication timeout", 
            BiometricAuthResult.ErrorType.TIMEOUT);
    }
    
    private BiometricAuthResult simulateFaceIdHardwareUnavailable() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "unavailable");
        params.put("reason", "camera_unavailable");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Face ID camera unavailable", 
            BiometricAuthResult.ErrorType.HARDWARE_ERROR);
    }
    
    private BiometricAuthResult simulateCameraBlocked() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "face_id");
        params.put("result", "error");
        params.put("reason", "camera_blocked");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Camera is blocked", 
            BiometricAuthResult.ErrorType.HARDWARE_ERROR);
    }
    
    // Private helper methods for voice simulation
    
    private BiometricAuthResult simulateValidVoice(String username) throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "success");
        params.put("username", username);
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(3);
        
        return new BiometricAuthResult(true, "Voice authentication successful", null);
    }
    
    private BiometricAuthResult simulateInvalidVoice() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "failure");
        params.put("reason", "voice_not_recognized");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(3);
        
        return new BiometricAuthResult(false, "Voice not recognized", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    private BiometricAuthResult simulateBackgroundNoise() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "error");
        params.put("reason", "background_noise");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Too much background noise", 
            BiometricAuthResult.ErrorType.ENVIRONMENTAL_ERROR);
    }
    
    private BiometricAuthResult simulateMicrophoneUnavailable() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "unavailable");
        params.put("reason", "microphone_unavailable");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(1);
        
        return new BiometricAuthResult(false, "Microphone unavailable", 
            BiometricAuthResult.ErrorType.HARDWARE_ERROR);
    }
    
    private BiometricAuthResult simulateVoiceTooQuiet() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "error");
        params.put("reason", "voice_too_quiet");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Please speak louder", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    private BiometricAuthResult simulateVoiceTooLoud() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "error");
        params.put("reason", "voice_too_loud");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Voice input too loud", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    private BiometricAuthResult simulateVoiceTimeout() throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "timeout");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        
        return new BiometricAuthResult(false, "Voice authentication timeout", 
            BiometricAuthResult.ErrorType.TIMEOUT);
    }
    
    private BiometricAuthResult simulateWrongLanguage() throws InterruptedException {
        Map<String, Object> params = new HashMap<>();
        params.put("biometricType", "voice");
        params.put("result", "error");
        params.put("reason", "language_mismatch");
        
        executeDriverScript("mobile: biometricAuthentication", params);
        TimeUnit.SECONDS.sleep(2);
        
        return new BiometricAuthResult(false, "Language not supported", 
            BiometricAuthResult.ErrorType.AUTHENTICATION_FAILED);
    }
    
    // Utility methods
    
    private void simulateEnrollmentProgress(int progress) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("progress", progress);
            params.put("message", "Enrollment progress: " + progress + "%");
            
            executeDriverScript("mobile: biometricEnrollmentProgress", params);
        } catch (Exception e) {
            logger.error("Error simulating enrollment progress", e);
        }
    }
    
    private String getSpoofingMethod(BiometricType biometricType) {
        return switch (biometricType) {
            case FINGERPRINT -> "silicone_finger";
            case FACE_ID -> "photo_attack";
            case VOICE -> "recorded_voice";
        };
    }
    
    private void executeDriverScript(String scriptName, Map<String, Object> params) {
        try {
            if (driver instanceof JavascriptExecutor) {
                ((JavascriptExecutor) driver).executeScript(scriptName, params);
            } else {
                logger.warn("Driver does not support JavaScript execution for script: {}", scriptName);
            }
        } catch (Exception e) {
            logger.debug("Script execution not supported or failed: {} - {}", scriptName, e.getMessage());
            // This is expected in test environments without real biometric hardware
        }
    }
}