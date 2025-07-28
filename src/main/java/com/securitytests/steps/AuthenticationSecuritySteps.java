package com.securitytests.steps;

import com.securitytests.utils.config.SecurityTestConfig;
import com.securitytests.utils.data.TestDataManager;
import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.security.AuthenticationSecurityTester;
import com.securitytests.utils.security.MobileAuthClient;
import io.cucumber.java.en.*;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Status;
import org.testng.Assert;

import java.util.*;

/**
 * Step definitions for Authentication Security BDD scenarios
 */
public class AuthenticationSecuritySteps {
    private static final StructuredLogger logger = new StructuredLogger(AuthenticationSecuritySteps.class);
    private final MobileAuthClient authClient;
    private final AuthenticationSecurityTester securityTester;
    private final TestDataManager testDataManager;
    private final SecurityTestConfig securityConfig;
    
    // Test context to store scenario data
    private final Map<String, Object> testContext = new HashMap<>();
    
    // Constructor for dependency injection
    public AuthenticationSecuritySteps() {
        this.authClient = MobileAuthClient.getInstance();
        this.securityTester = new AuthenticationSecurityTester();
        this.testDataManager = TestDataManager.getInstance();
        this.securityConfig = SecurityTestConfig.getInstance();
        
        logger.info("Initialized Authentication Security Steps");
    }

    @Given("a user attempting to access the system")
    public void aUserAttemptingToAccessTheSystem() {
        logger.info("Setting up user attempting to access the system");
        
        testContext.put("username", testDataManager.generateUniqueUsername());
        testContext.put("password", "Password123!");
        
        Allure.step("Created test user for authentication security testing", Status.PASSED);
    }
    
    @Given("a user with a weak password {string}")
    public void aUserWithWeakPassword(String password) {
        testContext.put("username", testDataManager.generateUniqueUsername());
        testContext.put("password", password);
        
        logger.info("Created user with weak password: {}", password);
        Allure.step("Created user with weak password: " + password, Status.PASSED);
    }

    @When("the user attempts {int} failed logins")
    public void userAttemptsFailedLogins(int attempts) {
        String username = (String) testContext.get("username");
        
        logger.info("Attempting {} failed logins for user: {}", attempts, username);
        
        for (int i = 0; i < attempts; i++) {
            boolean loginResult = securityTester.attemptLoginWithInvalidCredentials(
                    username, "WrongPassword" + i);
            
            // Should return false since these are invalid credentials
            Assert.assertFalse(loginResult, 
                    "Failed login attempt " + (i+1) + " should be rejected");
        }
        
        testContext.put("failedAttempts", attempts);
        Allure.step("Completed " + attempts + " failed login attempts", Status.PASSED);
    }

    @Then("the account should be temporarily locked")
    public void accountShouldBeTemporarilyLocked() {
        String username = (String) testContext.get("username");
        String password = (String) testContext.get("password");
        
        logger.info("Verifying account lockout for user: {}", username);
        
        boolean isLocked = securityTester.verifyAccountLockout(username, password);
        
        Assert.assertTrue(isLocked, "Account should be temporarily locked after failed login attempts");
        Allure.step("Verified account is temporarily locked after failed attempts", Status.PASSED);
    }

    @Then("the account should remain locked for at least {int} minutes")
    public void accountShouldRemainLockedForMinutes(int minutes) {
        String username = (String) testContext.get("username");
        
        logger.info("Verifying account remains locked for {} minutes", minutes);
        
        boolean remainsLocked = securityTester.verifyAccountRemainsLocked(username, minutes);
        
        Assert.assertTrue(remainsLocked, 
                "Account should remain locked for at least " + minutes + " minutes");
        
        Allure.step("Verified account remains locked for required duration", Status.PASSED);
    }

    @When("the user submits a {string} password")
    public void userSubmitsPassword(String passwordType) {
        String username = (String) testContext.get("username");
        String password;
        
        switch (passwordType) {
            case "too short":
                password = "Pass1!";
                break;
            case "without lowercase":
                password = "PASSWORD123!";
                break;
            case "without uppercase":
                password = "password123!";
                break;
            case "without numbers":
                password = "Password!!";
                break;
            case "without special characters":
                password = "Password123";
                break;
            case "common dictionary":
                password = "Password123";
                break;
            default:
                password = "StrongP@ssw0rd!";
                break;
        }
        
        testContext.put("password", password);
        testContext.put("passwordType", passwordType);
        
        logger.info("User submitting {} password: {}", passwordType, password);
        Allure.step("User submitted " + passwordType + " password", Status.PASSED);
    }

    @Then("the system should reject the password")
    public void systemShouldRejectPassword() {
        String username = (String) testContext.get("username");
        String password = (String) testContext.get("password");
        
        logger.info("Verifying system rejects weak password");
        
        boolean passwordRejected = securityTester.verifyPasswordRejected(username, password);
        
        Assert.assertTrue(passwordRejected, 
                "System should reject weak password: " + testContext.get("passwordType"));
        
        Allure.step("Verified system rejected weak password", Status.PASSED);
    }

    @When("an attacker attempts a brute force attack")
    public void attackerAttemptsBruteForceAttack() {
        logger.info("Simulating brute force attack");
        
        boolean attackDetected = securityTester.simulateBruteForceAttack();
        
        Assert.assertTrue(attackDetected, "Brute force attack should be detected");
        testContext.put("attackDetected", attackDetected);
        
        Allure.step("Simulated brute force attack", Status.PASSED);
    }

    @Then("the system should detect and prevent the attack")
    public void systemShouldDetectAndPreventAttack() {
        logger.info("Verifying attack prevention mechanisms");
        
        boolean attackPrevented = securityTester.verifyBruteForcePreventionActive();
        
        Assert.assertTrue(attackPrevented, "System should prevent brute force attack");
        
        Allure.step("Verified system detected and prevented attack", Status.PASSED);
    }

    @Given("a user with a registered email {string}")
    public void userWithRegisteredEmail(String email) {
        String username = testDataManager.generateUniqueUsername();
        String password = "Password123!";
        
        boolean userCreated = testDataManager.createTestUser(username, password, email);
        
        Assert.assertTrue(userCreated, "Test user should be created successfully");
        
        testContext.put("username", username);
        testContext.put("password", password);
        testContext.put("email", email);
        
        logger.info("Created test user with email: {}", email);
        Allure.step("Created user with registered email: " + email, Status.PASSED);
    }

    @When("the user requests a password reset")
    public void userRequestsPasswordReset() {
        String email = (String) testContext.get("email");
        
        logger.info("User requesting password reset for email: {}", email);
        
        boolean resetRequested = securityTester.requestPasswordReset(email);
        
        Assert.assertTrue(resetRequested, "Password reset should be requested successfully");
        testContext.put("resetRequested", resetRequested);
        
        Allure.step("Requested password reset", Status.PASSED);
    }

    @Then("a secure reset link with expiration should be sent")
    public void secureResetLinkShouldBeSent() {
        String email = (String) testContext.get("email");
        
        logger.info("Verifying secure reset link for email: {}", email);
        
        Map<String, Object> resetInfo = securityTester.verifyPasswordResetLink(email);
        
        Assert.assertNotNull(resetInfo.get("resetToken"), "Reset token should be generated");
        Assert.assertNotNull(resetInfo.get("expiration"), "Reset token should have expiration");
        
        testContext.putAll(resetInfo);
        
        Allure.step("Verified secure reset link was sent with expiration", Status.PASSED);
    }

    @When("an attacker attempts to enumerate valid usernames")
    public void attackerAttemptsToEnumerateUsernames() {
        logger.info("Simulating username enumeration attack");
        
        List<String> potentialUsernames = Arrays.asList(
                "admin", "user", (String) testContext.get("username"), 
                "test", "system", "root", "administrator");
        
        Map<String, Boolean> enumerationResults = securityTester.testUsernameEnumeration(potentialUsernames);
        
        testContext.put("enumerationResults", enumerationResults);
        
        Allure.step("Attempted username enumeration with " + potentialUsernames.size() + " usernames", Status.PASSED);
    }

    @Then("the system should provide identical responses for valid and invalid users")
    public void systemProvidesIdenticalResponses() {
        logger.info("Verifying identical responses for valid and invalid users");
        
        @SuppressWarnings("unchecked")
        Map<String, Boolean> enumerationResults = (Map<String, Boolean>) testContext.get("enumerationResults");
        
        boolean identicalResponses = securityTester.verifyResponsesPreventEnumeration(enumerationResults);
        
        Assert.assertTrue(identicalResponses, 
                "System should provide identical responses for valid and invalid users");
        
        Allure.step("Verified system provides identical responses for valid and invalid users", Status.PASSED);
    }

    @When("the user attempts to access a sensitive function")
    public void userAttemptsToAccessSensitiveFunction() {
        String username = (String) testContext.get("username");
        String password = (String) testContext.get("password");
        
        logger.info("User {} attempting to access sensitive function", username);
        
        boolean loggedIn = authClient.login(username, password);
        Assert.assertTrue(loggedIn, "User should log in successfully");
        
        testContext.put("accessAttempt", securityTester.attemptSensitiveFunctionAccess());
        
        Allure.step("Attempted to access sensitive function", Status.PASSED);
    }

    @Then("the system should require 2FA verification")
    public void systemShouldRequire2FA() {
        logger.info("Verifying 2FA requirement for sensitive function");
        
        boolean twoFactorRequired = securityTester.verify2FARequired();
        
        Assert.assertTrue(twoFactorRequired, "2FA should be required for sensitive function");
        
        Allure.step("Verified 2FA is required for sensitive function", Status.PASSED);
    }

    @When("the user performs an authentication action")
    public void userPerformsAuthenticationAction() {
        String username = (String) testContext.get("username");
        String password = (String) testContext.get("password");
        
        logger.info("User {} performing authentication action", username);
        
        boolean actionPerformed = securityTester.performAuthenticationAction(username, password);
        
        Assert.assertTrue(actionPerformed, "Authentication action should be performed");
        testContext.put("actionPerformed", actionPerformed);
        
        Allure.step("Performed authentication action", Status.PASSED);
    }

    @Then("the system should generate appropriate audit logs")
    public void systemGeneratesAuditLogs() {
        String username = (String) testContext.get("username");
        
        logger.info("Verifying audit logs for user: {}", username);
        
        Map<String, Object> auditInfo = securityTester.verifyAuditLogsExist(username);
        
        Assert.assertTrue((Boolean) auditInfo.get("logsExist"), "Audit logs should exist");
        Assert.assertNotNull(auditInfo.get("timestamp"), "Audit logs should have timestamp");
        Assert.assertNotNull(auditInfo.get("actionType"), "Audit logs should have action type");
        Assert.assertNotNull(auditInfo.get("ipAddress"), "Audit logs should have IP address");
        
        testContext.put("auditInfo", auditInfo);
        
        Allure.step("Verified system generated appropriate audit logs", Status.PASSED);
    }

    @When("an attacker attempts credential stuffing with known breached passwords")
    public void attackerAttemptsCredentialStuffing() {
        logger.info("Simulating credential stuffing attack");
        
        boolean attackDetected = securityTester.simulateCredentialStuffingAttack();
        
        Assert.assertTrue(attackDetected, "Credential stuffing attack should be detected");
        testContext.put("stuffingAttackDetected", attackDetected);
        
        Allure.step("Simulated credential stuffing attack", Status.PASSED);
    }

    @Then("the system should detect and block the credential stuffing attempt")
    public void systemShouldBlockCredentialStuffing() {
        logger.info("Verifying credential stuffing prevention");
        
        boolean attackPrevented = securityTester.verifyCredentialStuffingPreventionActive();
        
        Assert.assertTrue(attackPrevented, "System should prevent credential stuffing");
        
        Allure.step("Verified system detected and blocked credential stuffing attempt", Status.PASSED);
    }

    @Then("the user's credentials should be securely stored")
    public void credentialsShouldBeSecurelyStored() {
        String username = (String) testContext.get("username");
        
        logger.info("Verifying secure credential storage for user: {}", username);
        
        Map<String, Object> storageInfo = securityTester.verifySecureCredentialStorage(username);
        
        Assert.assertTrue((Boolean) storageInfo.get("isHashed"), 
                "Credentials should be hashed, not stored in plaintext");
        Assert.assertTrue((Boolean) storageInfo.get("isSalted"), 
                "Credentials should use unique salt");
        Assert.assertNotNull(storageInfo.get("hashAlgorithm"), 
                "Secure hash algorithm should be used");
        
        testContext.put("storageInfo", storageInfo);
        
        Allure.step("Verified credentials are securely stored", Status.PASSED);
    }

    @After("@authentication-security")
    public void cleanupAuthenticationTest() {
        // Clean up any test data created during the test
        if (testContext.containsKey("username")) {
            String username = (String) testContext.get("username");
            testDataManager.cleanupTestUserData(username);
            logger.info("Cleaned up test data for user: {}", username);
        }
    }
}
