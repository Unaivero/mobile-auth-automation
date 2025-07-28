package com.securitytests.utils.biometric;

import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.database.TestDataRepository;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive biometric authentication testing manager
 * Handles fingerprint, face ID, and voice authentication testing scenarios
 */
public class BiometricTestManager {
    private static final StructuredLogger logger = new StructuredLogger(BiometricTestManager.class);
    private final AppiumDriver driver;
    private final WebDriverWait wait;
    private final TestDataRepository testDataRepository;
    private final BiometricSimulator biometricSimulator;
    
    // Biometric authentication selectors
    private static final String BIOMETRIC_PROMPT_CONTAINER = "com.securitytests.demoapp:id/biometricPromptContainer";
    private static final String FINGERPRINT_ICON = "com.securitytests.demoapp:id/fingerprintIcon";
    private static final String FACE_ID_ICON = "com.securitytests.demoapp:id/faceIdIcon";
    private static final String VOICE_ICON = "com.securitytests.demoapp:id/voiceIcon";
    private static final String BIOMETRIC_TITLE = "com.securitytests.demoapp:id/biometricTitle";
    private static final String BIOMETRIC_DESCRIPTION = "com.securitytests.demoapp:id/biometricDescription";
    private static final String BIOMETRIC_CANCEL_BUTTON = "com.securitytests.demoapp:id/biometricCancelButton";
    private static final String BIOMETRIC_FALLBACK_BUTTON = "com.securitytests.demoapp:id/biometricFallbackButton";
    private static final String BIOMETRIC_ERROR_MESSAGE = "com.securitytests.demoapp:id/biometricErrorMessage";
    private static final String BIOMETRIC_SUCCESS_MESSAGE = "com.securitytests.demoapp:id/biometricSuccessMessage";
    
    public BiometricTestManager(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        this.testDataRepository = new TestDataRepository();
        this.biometricSimulator = new BiometricSimulator(driver);
    }
    
    /**
     * Test fingerprint authentication flow
     */
    @Step("Test fingerprint authentication for user: {username}")
    public BiometricAuthResult testFingerprintAuthentication(String username, BiometricTestScenario scenario) {
        logger.info("Testing fingerprint authentication for user: {} with scenario: {}", username, scenario.name());
        
        try {
            // Trigger biometric authentication
            WebElement biometricButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("com.securitytests.demoapp:id/biometricLoginButton")));
            biometricButton.click();
            
            // Wait for biometric prompt to appear
            WebElement biometricPrompt = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id(BIOMETRIC_PROMPT_CONTAINER)));
            
            // Verify fingerprint icon is displayed
            WebElement fingerprintIcon = driver.findElement(By.id(FINGERPRINT_ICON));
            if (!fingerprintIcon.isDisplayed()) {
                return new BiometricAuthResult(false, "Fingerprint icon not displayed", 
                    BiometricAuthResult.ErrorType.UI_ERROR);
            }
            
            // Verify prompt texts
            String title = driver.findElement(By.id(BIOMETRIC_TITLE)).getText();
            String description = driver.findElement(By.id(BIOMETRIC_DESCRIPTION)).getText();
            
            if (!title.contains("Fingerprint") || !description.contains("Touch the fingerprint sensor")) {
                return new BiometricAuthResult(false, "Incorrect biometric prompt text", 
                    BiometricAuthResult.ErrorType.UI_ERROR);
            }
            
            // Simulate fingerprint based on scenario
            BiometricAuthResult result = simulateFingerprintAuth(username, scenario);
            
            // Wait for result and verify UI response
            return verifyBiometricResult(result, BiometricType.FINGERPRINT);
            
        } catch (Exception e) {
            logger.error("Error during fingerprint authentication test", e);
            return new BiometricAuthResult(false, "Test execution error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Test Face ID authentication flow
     */
    @Step("Test Face ID authentication for user: {username}")
    public BiometricAuthResult testFaceIdAuthentication(String username, BiometricTestScenario scenario) {
        logger.info("Testing Face ID authentication for user: {} with scenario: {}", username, scenario.name());
        
        try {
            // Trigger Face ID authentication
            WebElement faceIdButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("com.securitytests.demoapp:id/faceIdLoginButton")));
            faceIdButton.click();
            
            // Wait for Face ID prompt
            WebElement biometricPrompt = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id(BIOMETRIC_PROMPT_CONTAINER)));
            
            // Verify Face ID icon and prompt
            WebElement faceIdIcon = driver.findElement(By.id(FACE_ID_ICON));
            if (!faceIdIcon.isDisplayed()) {
                return new BiometricAuthResult(false, "Face ID icon not displayed", 
                    BiometricAuthResult.ErrorType.UI_ERROR);
            }
            
            String title = driver.findElement(By.id(BIOMETRIC_TITLE)).getText();
            if (!title.contains("Face ID")) {
                return new BiometricAuthResult(false, "Incorrect Face ID prompt title", 
                    BiometricAuthResult.ErrorType.UI_ERROR);
            }
            
            // Simulate Face ID authentication
            BiometricAuthResult result = simulateFaceIdAuth(username, scenario);
            
            return verifyBiometricResult(result, BiometricType.FACE_ID);
            
        } catch (Exception e) {
            logger.error("Error during Face ID authentication test", e);
            return new BiometricAuthResult(false, "Test execution error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Test voice authentication flow
     */
    @Step("Test voice authentication for user: {username}")
    public BiometricAuthResult testVoiceAuthentication(String username, BiometricTestScenario scenario) {
        logger.info("Testing voice authentication for user: {} with scenario: {}", username, scenario.name());
        
        try {
            // Trigger voice authentication
            WebElement voiceButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("com.securitytests.demoapp:id/voiceLoginButton")));
            voiceButton.click();
            
            // Wait for voice prompt
            WebElement biometricPrompt = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id(BIOMETRIC_PROMPT_CONTAINER)));
            
            // Verify voice icon and prompt
            WebElement voiceIcon = driver.findElement(By.id(VOICE_ICON));
            if (!voiceIcon.isDisplayed()) {
                return new BiometricAuthResult(false, "Voice icon not displayed", 
                    BiometricAuthResult.ErrorType.UI_ERROR);
            }
            
            String title = driver.findElement(By.id(BIOMETRIC_TITLE)).getText();
            if (!title.contains("Voice")) {
                return new BiometricAuthResult(false, "Incorrect voice prompt title", 
                    BiometricAuthResult.ErrorType.UI_ERROR);
            }
            
            // Simulate voice authentication
            BiometricAuthResult result = simulateVoiceAuth(username, scenario);
            
            return verifyBiometricResult(result, BiometricType.VOICE);
            
        } catch (Exception e) {
            logger.error("Error during voice authentication test", e);
            return new BiometricAuthResult(false, "Test execution error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    /**
     * Test biometric enrollment process
     */
    @Step("Test biometric enrollment for user: {username} and type: {biometricType}")
    public BiometricEnrollmentResult testBiometricEnrollment(String username, BiometricType biometricType, 
                                                           byte[] biometricTemplate) {
        logger.info("Testing biometric enrollment for user: {} with type: {}", username, biometricType);
        
        try {
            // Get user data
            Map<String, Object> user = testDataRepository.getTestUserByUsername(username);
            if (user == null) {
                return new BiometricEnrollmentResult(false, "User not found", null);
            }
            
            UUID userId = (UUID) user.get("id");
            
            // Navigate to biometric enrollment settings
            navigateToBiometricSettings();
            
            // Select biometric type
            selectBiometricType(biometricType);
            
            // Start enrollment process
            WebElement enrollButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("com.securitytests.demoapp:id/startEnrollmentButton")));
            enrollButton.click();
            
            // Simulate enrollment process
            BiometricEnrollmentResult enrollmentResult = simulateEnrollmentProcess(biometricType, biometricTemplate);
            
            if (enrollmentResult.isSuccess()) {
                // Store biometric data in database
                UUID biometricId = testDataRepository.enrollBiometricData(userId, biometricType.toString(), 
                    biometricTemplate, "test_device_001");
                enrollmentResult.setBiometricId(biometricId);
                
                logger.info("Biometric enrollment successful for user: {} with ID: {}", username, biometricId);
            }
            
            return enrollmentResult;
            
        } catch (Exception e) {
            logger.error("Error during biometric enrollment test", e);
            return new BiometricEnrollmentResult(false, "Enrollment error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Test biometric authentication failure scenarios
     */
    @Step("Test biometric authentication failure scenarios")
    public List<BiometricAuthResult> testBiometricFailureScenarios(String username, BiometricType biometricType) {
        List<BiometricAuthResult> results = new ArrayList<>();
        
        // Test various failure scenarios
        BiometricTestScenario[] failureScenarios = {
            BiometricTestScenario.INVALID_BIOMETRIC,
            BiometricTestScenario.SENSOR_ERROR,
            BiometricTestScenario.TIMEOUT,
            BiometricTestScenario.TOO_MANY_ATTEMPTS,
            BiometricTestScenario.HARDWARE_UNAVAILABLE
        };
        
        for (BiometricTestScenario scenario : failureScenarios) {
            logger.info("Testing failure scenario: {} for biometric type: {}", scenario, biometricType);
            
            BiometricAuthResult result = switch (biometricType) {
                case FINGERPRINT -> testFingerprintAuthentication(username, scenario);
                case FACE_ID -> testFaceIdAuthentication(username, scenario);
                case VOICE -> testVoiceAuthentication(username, scenario);
            };
            
            result.setScenario(scenario);
            results.add(result);
            
            // Wait between attempts to avoid rate limiting
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Test biometric security bypass attempts
     */
    @Step("Test biometric security bypass attempts")
    public BiometricSecurityTestResult testBiometricSecurityBypass(String username, BiometricType biometricType) {
        logger.info("Testing biometric security bypass for user: {} with type: {}", username, biometricType);
        
        BiometricSecurityTestResult securityResult = new BiometricSecurityTestResult();
        
        try {
            // Test 1: Rapid successive attempts
            securityResult.addTest("rapid_attempts", testRapidBiometricAttempts(username, biometricType));
            
            // Test 2: Presentation attack detection
            securityResult.addTest("presentation_attack", testPresentationAttackDetection(username, biometricType));
            
            // Test 3: Template injection attack
            securityResult.addTest("template_injection", testTemplateInjectionAttack(username, biometricType));
            
            // Test 4: Biometric spoofing
            securityResult.addTest("spoofing_attack", testBiometricSpoofing(username, biometricType));
            
            // Test 5: Fallback mechanism security
            securityResult.addTest("fallback_security", testFallbackMechanismSecurity(username, biometricType));
            
        } catch (Exception e) {
            logger.error("Error during biometric security testing", e);
            securityResult.addError("Security test execution error: " + e.getMessage());
        }
        
        return securityResult;
    }
    
    // Private helper methods
    
    private BiometricAuthResult simulateFingerprintAuth(String username, BiometricTestScenario scenario) {
        return biometricSimulator.simulateFingerprint(username, scenario);
    }
    
    private BiometricAuthResult simulateFaceIdAuth(String username, BiometricTestScenario scenario) {
        return biometricSimulator.simulateFaceId(username, scenario);
    }
    
    private BiometricAuthResult simulateVoiceAuth(String username, BiometricTestScenario scenario) {
        return biometricSimulator.simulateVoice(username, scenario);
    }
    
    private BiometricAuthResult verifyBiometricResult(BiometricAuthResult expectedResult, BiometricType biometricType) {
        try {
            if (expectedResult.isSuccess()) {
                // Wait for success message
                WebElement successMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id(BIOMETRIC_SUCCESS_MESSAGE)));
                
                if (successMessage.isDisplayed() && successMessage.getText().contains("Authentication successful")) {
                    logger.info("Biometric authentication successful for type: {}", biometricType);
                    return new BiometricAuthResult(true, "Authentication successful", null);
                } else {
                    return new BiometricAuthResult(false, "Success message not displayed correctly", 
                        BiometricAuthResult.ErrorType.UI_ERROR);
                }
            } else {
                // Wait for error message
                WebElement errorMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.id(BIOMETRIC_ERROR_MESSAGE)));
                
                if (errorMessage.isDisplayed()) {
                    String errorText = errorMessage.getText();
                    logger.info("Biometric authentication failed as expected: {}", errorText);
                    return new BiometricAuthResult(false, errorText, expectedResult.getErrorType());
                } else {
                    return new BiometricAuthResult(false, "Error message not displayed", 
                        BiometricAuthResult.ErrorType.UI_ERROR);
                }
            }
        } catch (Exception e) {
            logger.error("Error verifying biometric result", e);
            return new BiometricAuthResult(false, "Result verification error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
    
    private void navigateToBiometricSettings() {
        // Navigate to settings
        WebElement settingsButton = wait.until(ExpectedConditions.elementToBeClickable(
            By.id("com.securitytests.demoapp:id/settingsButton")));
        settingsButton.click();
        
        // Navigate to biometric settings
        WebElement biometricSettings = wait.until(ExpectedConditions.elementToBeClickable(
            By.id("com.securitytests.demoapp:id/biometricSettingsButton")));
        biometricSettings.click();
    }
    
    private void selectBiometricType(BiometricType biometricType) {
        String biometricTypeSelector = switch (biometricType) {
            case FINGERPRINT -> "com.securitytests.demoapp:id/selectFingerprintButton";
            case FACE_ID -> "com.securitytests.demoapp:id/selectFaceIdButton";
            case VOICE -> "com.securitytests.demoapp:id/selectVoiceButton";
        };
        
        WebElement typeSelector = wait.until(ExpectedConditions.elementToBeClickable(By.id(biometricTypeSelector)));
        typeSelector.click();
    }
    
    private BiometricEnrollmentResult simulateEnrollmentProcess(BiometricType biometricType, byte[] biometricTemplate) {
        return biometricSimulator.simulateEnrollment(biometricType, biometricTemplate);
    }
    
    private BiometricAuthResult testRapidBiometricAttempts(String username, BiometricType biometricType) {
        // Simulate rapid successive authentication attempts to test rate limiting
        logger.info("Testing rapid biometric attempts for rate limiting");
        
        int attempts = 0;
        int maxAttempts = 10;
        long startTime = System.currentTimeMillis();
        
        while (attempts < maxAttempts && (System.currentTimeMillis() - startTime) < 10000) {
            try {
                BiometricAuthResult result = switch (biometricType) {
                    case FINGERPRINT -> testFingerprintAuthentication(username, BiometricTestScenario.VALID_BIOMETRIC);
                    case FACE_ID -> testFaceIdAuthentication(username, BiometricTestScenario.VALID_BIOMETRIC);
                    case VOICE -> testVoiceAuthentication(username, BiometricTestScenario.VALID_BIOMETRIC);
                };
                
                attempts++;
                
                // Check if rate limiting is applied
                if (result.getErrorType() == BiometricAuthResult.ErrorType.RATE_LIMITED) {
                    return new BiometricAuthResult(true, "Rate limiting applied correctly", null);
                }
                
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new BiometricAuthResult(false, "Rate limiting not applied", 
            BiometricAuthResult.ErrorType.SECURITY_ISSUE);
    }
    
    private BiometricAuthResult testPresentationAttackDetection(String username, BiometricType biometricType) {
        // Test if the system can detect presentation attacks (e.g., photo attacks for face recognition)
        logger.info("Testing presentation attack detection");
        return biometricSimulator.simulatePresentationAttack(username, biometricType);
    }
    
    private BiometricAuthResult testTemplateInjectionAttack(String username, BiometricType biometricType) {
        // Test if the system is vulnerable to template injection attacks
        logger.info("Testing template injection attack");
        return biometricSimulator.simulateTemplateInjection(username, biometricType);
    }
    
    private BiometricAuthResult testBiometricSpoofing(String username, BiometricType biometricType) {
        // Test various spoofing techniques
        logger.info("Testing biometric spoofing");
        return biometricSimulator.simulateSpoofingAttack(username, biometricType);
    }
    
    private BiometricAuthResult testFallbackMechanismSecurity(String username, BiometricType biometricType) {
        // Test the security of fallback authentication mechanisms
        logger.info("Testing fallback mechanism security");
        
        try {
            // Trigger biometric authentication
            switch (biometricType) {
                case FINGERPRINT -> testFingerprintAuthentication(username, BiometricTestScenario.VALID_BIOMETRIC);
                case FACE_ID -> testFaceIdAuthentication(username, BiometricTestScenario.VALID_BIOMETRIC);
                case VOICE -> testVoiceAuthentication(username, BiometricTestScenario.VALID_BIOMETRIC);
            }
            
            // Click fallback button
            WebElement fallbackButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.id(BIOMETRIC_FALLBACK_BUTTON)));
            fallbackButton.click();
            
            // Verify that fallback requires proper authentication
            WebElement passwordField = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.id("com.securitytests.demoapp:id/fallbackPasswordField")));
            
            if (passwordField.isDisplayed()) {
                return new BiometricAuthResult(true, "Fallback mechanism requires proper authentication", null);
            } else {
                return new BiometricAuthResult(false, "Fallback mechanism bypasses authentication", 
                    BiometricAuthResult.ErrorType.SECURITY_ISSUE);
            }
            
        } catch (Exception e) {
            logger.error("Error testing fallback mechanism", e);
            return new BiometricAuthResult(false, "Fallback test error: " + e.getMessage(), 
                BiometricAuthResult.ErrorType.EXECUTION_ERROR);
        }
    }
}