package com.securitytests.steps;

import com.securitytests.utils.client.MobileAuthClient;
import com.securitytests.utils.config.SecurityTestConfig;
import com.securitytests.utils.data.TestDataManager;
import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.security.SessionSecurityTester;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.json.JSONObject;
import org.testng.Assert;

import java.util.*;

/**
 * Step definitions for session security BDD scenarios
 */
public class SessionSecuritySteps {
    private static final StructuredLogger logger = new StructuredLogger(SessionSecuritySteps.class);
    private final MobileAuthClient authClient;
    private final TestDataManager testDataManager;
    private final SessionSecurityTester securityTester;
    private final SecurityTestConfig securityConfig;
    
    private String username;
    private String password;
    private String authToken;
    private Map<String, Object> sessionData;
    private JSONObject securityTestResults;
    private String jwtToken;
    private Map<String, String> responseHeaders;
    
    public SessionSecuritySteps() {
        authClient = new MobileAuthClient();
        testDataManager = TestDataManager.getInstance();
        securityTester = new SessionSecurityTester();
        securityConfig = SecurityTestConfig.getInstance();
        sessionData = new HashMap<>();
    }
    
    @Before
    public void setup() {
        securityConfig.initialize();
        logger.info("Setting up session security tests");
        sessionData.clear();
    }
    
    @After
    public void tearDown() {
        // Clean up any test data
        testDataManager.cleanupTestData();
        
        // Ensure we log out if we're still authenticated
        if (authToken != null) {
            try {
                authClient.logout(authToken);
            } catch (Exception e) {
                logger.warn("Error during logout in teardown", e);
            }
        }
    }
    
    @Given("the authentication API is available")
    public void theAuthenticationAPIIsAvailable() {
        Allure.step("Verifying authentication API availability");
        boolean isAvailable = authClient.isApiAvailable();
        logger.info("Authentication API availability check: {}", isAvailable);
        Assert.assertTrue(isAvailable, "Authentication API should be available for testing");
    }
    
    @Given("test user accounts are configured")
    public void testUserAccountsAreConfigured() {
        Allure.step("Setting up test user accounts");
        
        // Create a test user for our security tests
        Map<String, Object> userData = testDataManager.createTestUser();
        username = (String) userData.get("email");
        password = (String) userData.get("password");
        
        logger.info("Test user configured: {}", username);
        sessionData.put("username", username);
        sessionData.put("password", password);
    }
    
    @Given("a user is authenticated with the mobile app")
    public void aUserIsAuthenticatedWithTheMobileApp() {
        Allure.step("Authenticating user with mobile app");
        
        if (username == null || password == null) {
            Map<String, Object> userData = testDataManager.createTestUser();
            username = (String) userData.get("email");
            password = (String) userData.get("password");
            sessionData.put("username", username);
            sessionData.put("password", password);
        }
        
        try {
            JSONObject loginResponse = authClient.login(username, password);
            authToken = loginResponse.getString("token");
            jwtToken = authToken; // Store for later JWT validation
            
            Assert.assertNotNull(authToken, "Auth token should be returned after login");
            logger.info("User successfully authenticated: {}", username);
            sessionData.put("authToken", authToken);
            sessionData.put("loginResponse", loginResponse.toString());
        } catch (Exception e) {
            logger.error("Authentication failed", e);
            Assert.fail("Authentication failed: " + e.getMessage());
        }
    }
    
    @When("the user is inactive for the timeout period")
    public void theUserIsInactiveForTheTimeoutPeriod() throws Exception {
        int sessionTimeout = securityConfig.getSessionTimeoutMinutes();
        Allure.step("Simulating user inactivity for " + sessionTimeout + " minutes");
        
        // This is a simulation for testing - we won't actually wait for the full timeout
        // Instead we'll use the security tester to verify the timeout configuration
        securityTestResults = securityTester.testSessionTimeout(authToken, sessionTimeout);
        
        logger.info("Session timeout test completed. Session timeout set to {} minutes", sessionTimeout);
        sessionData.put("sessionTimeoutResults", securityTestResults.toString());
    }
    
    @When("examining the session cookies")
    public void examiningTheSessionCookies() {
        Allure.step("Examining session cookies security attributes");
        
        securityTestResults = securityTester.testCookieSecurityAttributes(authToken);
        logger.info("Session cookie security attributes examined");
        sessionData.put("cookieSecurityResults", securityTestResults.toString());
    }
    
    @When("examining the issued JWT token")
    public void examiningTheIssuedJWTToken() {
        Allure.step("Examining JWT token structure and security");
        
        securityTestResults = securityTester.testJWTSecurity(jwtToken);
        logger.info("JWT token security examined");
        sessionData.put("jwtSecurityResults", securityTestResults.toString());
        
        // Save token parts for Allure reporting
        Map<String, Object> tokenParts = authClient.getJWTSecurityAnalyzer().decodeJWT(jwtToken);
        Allure.addAttachment("JWT Header", "application/json", tokenParts.get("header").toString());
        Allure.addAttachment("JWT Payload", "application/json", tokenParts.get("payload").toString());
    }
    
    @When("the JWT token is modified by tampering with the {}")
    public void theJWTTokenIsModifiedByTamperingWithTheComponent(String component) {
        Allure.step("Tampering with JWT token " + component);
        
        String tamperedToken = authClient.getJWTSecurityAnalyzer().tamperJWT(jwtToken, component);
        securityTestResults = securityTester.testTamperedToken(tamperedToken);
        
        logger.info("JWT token tampered with component: {}", component);
        sessionData.put("tamperedToken", tamperedToken);
        sessionData.put("tamperedComponent", component);
        sessionData.put("tamperingResults", securityTestResults.toString());
        
        Allure.addAttachment("Tampered JWT", "text/plain", tamperedToken);
    }
    
    @When("examining the API response headers")
    public void examiningTheAPIResponseHeaders() {
        Allure.step("Examining API response security headers");
        
        responseHeaders = authClient.getSecurityHeaders(authToken);
        logger.info("API response headers examined: {} headers found", responseHeaders.size());
        
        StringBuilder headerInfo = new StringBuilder();
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            headerInfo.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
        }
        Allure.addAttachment("Response Headers", "text/plain", headerInfo.toString());
    }
    
    @When("the user logs out")
    public void theUserLogsOut() {
        Allure.step("User logging out");
        
        try {
            boolean logoutSuccess = authClient.logout(authToken);
            securityTestResults = securityTester.testSessionTermination(authToken);
            
            logger.info("User logged out: {}", logoutSuccess);
            sessionData.put("logoutSuccess", logoutSuccess);
            sessionData.put("sessionTerminationResults", securityTestResults.toString());
        } catch (Exception e) {
            logger.error("Logout failed", e);
            Assert.fail("Logout failed: " + e.getMessage());
        }
    }
    
    @When("the user authenticates from a different device")
    public void theUserAuthenticatesFromADifferentDevice() {
        Allure.step("Authenticating user from a different device");
        
        try {
            // Simulate login from another device
            Map<String, String> deviceHeaders = new HashMap<>();
            deviceHeaders.put("User-Agent", "Different-Device-Agent");
            deviceHeaders.put("X-Device-ID", UUID.randomUUID().toString());
            
            JSONObject secondLoginResponse = authClient.login(username, password, deviceHeaders);
            String secondAuthToken = secondLoginResponse.getString("token");
            
            Assert.assertNotNull(secondAuthToken, "Second auth token should be returned after login");
            logger.info("User authenticated from second device: {}", username);
            
            sessionData.put("secondAuthToken", secondAuthToken);
            sessionData.put("secondLoginResponse", secondLoginResponse.toString());
            
            // Test concurrent session handling
            securityTestResults = securityTester.testConcurrentSessions(authToken, secondAuthToken);
            sessionData.put("concurrentSessionResults", securityTestResults.toString());
        } catch (Exception e) {
            logger.error("Second device authentication failed", e);
            Assert.fail("Second device authentication failed: " + e.getMessage());
        }
    }
    
    @When("the user changes their password")
    public void theUserChangesTheirPassword() {
        Allure.step("User changing password");
        
        String newPassword = "NewSecureP@ssw0rd" + System.currentTimeMillis();
        
        try {
            boolean passwordChanged = authClient.changePassword(
                    authToken, password, newPassword);
            
            logger.info("Password changed successfully: {}", passwordChanged);
            sessionData.put("passwordChanged", passwordChanged);
            sessionData.put("oldPassword", password);
            sessionData.put("newPassword", newPassword);
            
            // Update password for future steps
            password = newPassword;
            sessionData.put("password", password);
            
            // Test if sessions are invalidated after password change
            securityTestResults = securityTester.testSessionsAfterPasswordChange(authToken);
            sessionData.put("passwordChangeSessionResults", securityTestResults.toString());
        } catch (Exception e) {
            logger.error("Password change failed", e);
            Assert.fail("Password change failed: " + e.getMessage());
        }
    }
    
    @When("the app is closed and reopened")
    public void theAppIsClosedAndReopened() {
        Allure.step("Simulating app restart");
        
        try {
            // Store the current token for comparison
            String originalToken = authToken;
            sessionData.put("originalToken", originalToken);
            
            // Simulate app restart by creating a new client instance but reusing token
            MobileAuthClient newClient = new MobileAuthClient();
            boolean isStillValid = newClient.validateToken(authToken);
            
            logger.info("Token still valid after app restart: {}", isStillValid);
            sessionData.put("tokenValidAfterRestart", isStillValid);
        } catch (Exception e) {
            logger.error("App restart simulation failed", e);
            Assert.fail("App restart simulation failed: " + e.getMessage());
        }
    }
    
    @Given("a user has an existing unauthenticated session")
    public void aUserHasAnExistingUnauthenticatedSession() {
        Allure.step("Creating unauthenticated session");
        
        // Get session identifier before authentication
        String preAuthSessionId = securityTester.getSessionIdentifier();
        sessionData.put("preAuthSessionId", preAuthSessionId);
        
        logger.info("Pre-authentication session created with ID: {}", preAuthSessionId);
    }
    
    @Then("the session should expire")
    public void theSessionShouldExpire() {
        Allure.step("Verifying session expiration");
        
        boolean sessionExpired = securityTestResults.getBoolean("sessionExpired");
        logger.info("Session expired: {}", sessionExpired);
        
        Assert.assertTrue(sessionExpired, "Session should expire after timeout period");
        Allure.addAttachment("Session Timeout Test Results", "application/json", 
                securityTestResults.toString());
    }
    
    @Then("the user should be redirected to the login screen")
    public void theUserShouldBeRedirectedToTheLoginScreen() {
        Allure.step("Verifying redirection to login screen");
        
        boolean redirectedToLogin = securityTestResults.getBoolean("redirectedToLogin");
        logger.info("Redirected to login: {}", redirectedToLogin);
        
        Assert.assertTrue(redirectedToLogin, "User should be redirected to login screen after session expiration");
    }
    
    @Then("access to protected resources should be denied")
    public void accessToProtectedResourcesShouldBeDenied() {
        Allure.step("Verifying access to protected resources is denied");
        
        try {
            // Attempt to access protected resource with current token
            boolean accessDenied = !authClient.canAccessProtectedResources(authToken);
            logger.info("Access denied to protected resources: {}", accessDenied);
            
            Assert.assertTrue(accessDenied, "Access to protected resources should be denied");
        } catch (Exception e) {
            // Exception is expected and confirms access is denied
            logger.info("Expected exception confirming access denial: {}", e.getMessage());
        }
    }
    
    @Then("a new session identifier should be generated")
    public void aNewSessionIdentifierShouldBeGenerated() {
        Allure.step("Verifying new session identifier generation");
        
        // Get session identifier after authentication
        String postAuthSessionId = securityTester.getSessionIdentifier();
        String preAuthSessionId = (String) sessionData.get("preAuthSessionId");
        
        logger.info("Pre-auth session ID: {}, Post-auth session ID: {}", preAuthSessionId, postAuthSessionId);
        sessionData.put("postAuthSessionId", postAuthSessionId);
        
        Assert.assertNotEquals(preAuthSessionId, postAuthSessionId, 
                "A new session identifier should be generated after authentication");
    }
    
    @Then("the original session identifier should be invalidated")
    public void theOriginalSessionIdentifierShouldBeInvalidated() {
        Allure.step("Verifying original session identifier invalidation");
        
        String preAuthSessionId = (String) sessionData.get("preAuthSessionId");
        boolean isInvalidated = securityTester.isSessionInvalidated(preAuthSessionId);
        
        logger.info("Original session invalidated: {}", isInvalidated);
        Assert.assertTrue(isInvalidated, "Original session identifier should be invalidated");
    }
    
    @Then("the new session should have proper security attributes")
    public void theNewSessionShouldHaveProperSecurityAttributes() {
        Allure.step("Verifying new session security attributes");
        
        JSONObject sessionAttributes = securityTester.getSessionAttributes();
        boolean hasProperAttributes = sessionAttributes.getBoolean("hasProperAttributes");
        
        logger.info("Session has proper security attributes: {}", hasProperAttributes);
        Assert.assertTrue(hasProperAttributes, "New session should have proper security attributes");
        
        Allure.addAttachment("Session Security Attributes", "application/json", 
                sessionAttributes.toString());
    }
    
    @Then("the system should detect concurrent sessions")
    public void theSystemShouldDetectConcurrentSessions() {
        Allure.step("Verifying concurrent session detection");
        
        boolean concurrentSessionsDetected = securityTestResults.getBoolean("concurrentSessionsDetected");
        logger.info("Concurrent sessions detected: {}", concurrentSessionsDetected);
        
        Assert.assertTrue(concurrentSessionsDetected, "System should detect concurrent sessions");
    }
    
    @Then("the user should be notified of multiple active sessions")
    public void theUserShouldBeNotifiedOfMultipleActiveSessions() {
        Allure.step("Verifying user notification of multiple sessions");
        
        boolean userNotified = securityTestResults.getBoolean("userNotified");
        logger.info("User notified of multiple sessions: {}", userNotified);
        
        Assert.assertTrue(userNotified, "User should be notified of multiple active sessions");
    }
    
    @Then("one of the following security actions should occur:")
    public void oneOfTheFollowingSecurityActionsShouldOccur(DataTable dataTable) {
        Allure.step("Verifying security action for concurrent sessions");
        
        List<String> expectedActions = dataTable.asList(String.class);
        expectedActions.remove(0); // Remove header row
        
        String actualAction = securityTestResults.getString("securityAction");
        logger.info("Security action taken: {}", actualAction);
        
        boolean validAction = expectedActions.stream()
                .anyMatch(action -> action.trim().equalsIgnoreCase(actualAction.trim()));
        
        Assert.assertTrue(validAction, 
                String.format("Security action '%s' should be one of the expected actions: %s", 
                        actualAction, expectedActions));
    }
    
    @Then("the session should be completely terminated")
    public void theSessionShouldBeCompletelyTerminated() {
        Allure.step("Verifying complete session termination");
        
        boolean sessionTerminated = securityTestResults.getBoolean("sessionTerminated");
        logger.info("Session completely terminated: {}", sessionTerminated);
        
        Assert.assertTrue(sessionTerminated, "Session should be completely terminated after logout");
    }
    
    @Then("the session cookie should be deleted")
    public void theSessionCookieShouldBeDeleted() {
        Allure.step("Verifying session cookie deletion");
        
        boolean cookieDeleted = securityTestResults.getBoolean("cookieDeleted");
        logger.info("Session cookie deleted: {}", cookieDeleted);
        
        Assert.assertTrue(cookieDeleted, "Session cookie should be deleted after logout");
    }
    
    @Then("session data should be removed from the server")
    public void sessionDataShouldBeRemovedFromTheServer() {
        Allure.step("Verifying session data removal from server");
        
        boolean dataRemoved = securityTestResults.getBoolean("serverDataRemoved");
        logger.info("Session data removed from server: {}", dataRemoved);
        
        Assert.assertTrue(dataRemoved, "Session data should be removed from server after logout");
    }
    
    @Then("secure flag should be set")
    public void secureFlagShouldBeSet() {
        Allure.step("Verifying Secure flag on cookies");
        
        boolean secureFlag = securityTestResults.getBoolean("secureFlag");
        logger.info("Secure flag set: {}", secureFlag);
        
        Assert.assertTrue(secureFlag, "Secure flag should be set on session cookies");
    }
    
    @Then("httpOnly flag should be set")
    public void httpOnlyFlagShouldBeSet() {
        Allure.step("Verifying HttpOnly flag on cookies");
        
        boolean httpOnlyFlag = securityTestResults.getBoolean("httpOnlyFlag");
        logger.info("HttpOnly flag set: {}", httpOnlyFlag);
        
        Assert.assertTrue(httpOnlyFlag, "HttpOnly flag should be set on session cookies");
    }
    
    @Then("SameSite attribute should be set appropriately")
    public void sameSiteAttributeShouldBeSetAppropriately() {
        Allure.step("Verifying SameSite attribute on cookies");
        
        boolean sameSiteAttribute = securityTestResults.getBoolean("sameSiteAttribute");
        String sameSiteValue = securityTestResults.getString("sameSiteValue");
        
        logger.info("SameSite attribute set: {}, value: {}", sameSiteAttribute, sameSiteValue);
        
        Assert.assertTrue(sameSiteAttribute, "SameSite attribute should be set on session cookies");
        Assert.assertTrue(
                "Strict".equalsIgnoreCase(sameSiteValue) || "Lax".equalsIgnoreCase(sameSiteValue),
                "SameSite attribute should be set to Strict or Lax");
    }
    
    @Then("secure cookie transmission should be enforced")
    public void secureCookieTransmissionShouldBeEnforced() {
        Allure.step("Verifying secure cookie transmission enforcement");
        
        boolean secureTransmission = securityTestResults.getBoolean("secureTransmission");
        logger.info("Secure cookie transmission enforced: {}", secureTransmission);
        
        Assert.assertTrue(secureTransmission, "Secure cookie transmission should be enforced");
    }
    
    @Then("the token should have a valid structure")
    public void theTokenShouldHaveAValidStructure() {
        Allure.step("Verifying JWT token structure");
        
        boolean validStructure = securityTestResults.getBoolean("validStructure");
        logger.info("JWT token has valid structure: {}", validStructure);
        
        Assert.assertTrue(validStructure, "JWT token should have a valid structure");
    }
    
    @Then("the token should not contain sensitive data")
    public void theTokenShouldNotContainSensitiveData() {
        Allure.step("Verifying JWT token does not contain sensitive data");
        
        boolean noSensitiveData = securityTestResults.getBoolean("noSensitiveData");
        logger.info("JWT token free of sensitive data: {}", noSensitiveData);
        
        Assert.assertTrue(noSensitiveData, "JWT token should not contain sensitive data");
    }
    
    @Then("the token should have a reasonable expiry time")
    public void theTokenShouldHaveAReasonableExpiryTime() {
        Allure.step("Verifying JWT token expiry time");
        
        boolean reasonableExpiry = securityTestResults.getBoolean("reasonableExpiry");
        int expiryMinutes = securityTestResults.getInt("expiryMinutes");
        
        logger.info("JWT token has reasonable expiry: {}, expires in {} minutes", 
                reasonableExpiry, expiryMinutes);
        
        Assert.assertTrue(reasonableExpiry, "JWT token should have a reasonable expiry time");
        Assert.assertTrue(expiryMinutes > 0 && expiryMinutes <= 60, 
                "JWT token expiry time should be between 1 and 60 minutes");
    }
    
    @Then("the token signature should be verified with the proper key")
    public void theTokenSignatureShouldBeVerifiedWithTheProperKey() {
        Allure.step("Verifying JWT token signature validation");
        
        boolean signatureValid = securityTestResults.getBoolean("signatureValid");
        logger.info("JWT token signature valid: {}", signatureValid);
        
        Assert.assertTrue(signatureValid, "JWT token signature should be verified with proper key");
    }
    
    @Then("token validation should fail with appropriate error")
    public void tokenValidationShouldFailWithAppropriateError() {
        Allure.step("Verifying tampered token validation failure");
        
        boolean validationFailed = securityTestResults.getBoolean("validationFailed");
        String errorMessage = securityTestResults.getString("errorMessage");
        
        logger.info("Tampered token validation failed: {}, error: {}", 
                validationFailed, errorMessage);
        
        Assert.assertTrue(validationFailed, "Tampered token validation should fail");
        Assert.assertNotNull(errorMessage, "Validation failure should include error message");
        Assert.assertFalse(errorMessage.isEmpty(), "Error message should not be empty");
    }
    
    @Then("the user session should remain valid")
    public void theUserSessionShouldRemainValid() {
        Allure.step("Verifying session remains valid after app restart");
        
        boolean tokenValidAfterRestart = (boolean) sessionData.get("tokenValidAfterRestart");
        
        logger.info("Session valid after app restart: {}", tokenValidAfterRestart);
        Assert.assertTrue(tokenValidAfterRestart, "User session should remain valid after app restart");
    }
    
    @Then("the user should not be prompted to log in again")
    public void theUserShouldNotBePromptedToLogInAgain() {
        Allure.step("Verifying no login prompt after app restart");
        
        // Implemented by ensuring the token still works for API calls
        try {
            JSONObject userProfile = authClient.getUserProfile(authToken);
            boolean profileAccessible = userProfile != null && userProfile.has("email");
            
            logger.info("User profile accessible without re-login: {}", profileAccessible);
            Assert.assertTrue(profileAccessible, "User should not be prompted to log in again");
        } catch (Exception e) {
            logger.error("Profile access failed after app restart", e);
            Assert.fail("User should not be prompted to log in again but token was rejected");
        }
    }
    
    @Then("access to protected resources should be maintained")
    public void accessToProtectedResourcesShouldBeMaintained() {
        Allure.step("Verifying continued access to protected resources");
        
        boolean canAccessProtected = authClient.canAccessProtectedResources(authToken);
        logger.info("Access to protected resources maintained: {}", canAccessProtected);
        
        Assert.assertTrue(canAccessProtected, "Access to protected resources should be maintained");
    }
    
    @Then("all active sessions should be invalidated")
    public void allActiveSessionsShouldBeInvalidated() {
        Allure.step("Verifying all sessions invalidated after password change");
        
        boolean allSessionsInvalidated = securityTestResults.getBoolean("allSessionsInvalidated");
        logger.info("All sessions invalidated after password change: {}", allSessionsInvalidated);
        
        Assert.assertTrue(allSessionsInvalidated, "All active sessions should be invalidated after password change");
    }
    
    @Then("the user should be required to log in with the new password")
    public void theUserShouldBeRequiredToLogInWithTheNewPassword() {
        Allure.step("Verifying re-login requirement with new password");
        
        // Try old password first - should fail
        try {
            String oldPassword = (String) sessionData.get("oldPassword");
            authClient.login(username, oldPassword);
            Assert.fail("Login with old password should fail");
        } catch (Exception e) {
            logger.info("Expected failure with old password: {}", e.getMessage());
        }
        
        // Try new password - should succeed
        try {
            JSONObject loginResponse = authClient.login(username, password);
            boolean loginSuccessful = loginResponse != null && loginResponse.has("token");
            
            logger.info("Login with new password successful: {}", loginSuccessful);
            Assert.assertTrue(loginSuccessful, "Login with new password should succeed");
        } catch (Exception e) {
            logger.error("Login with new password failed", e);
            Assert.fail("Login with new password failed: " + e.getMessage());
        }
    }
    
    @Then("security headers should be properly configured:")
    public void securityHeadersShouldBeProperlyConfigured(DataTable dataTable) {
        Allure.step("Verifying security headers configuration");
        
        List<Map<String, String>> headerRows = dataTable.asMaps(String.class, String.class);
        boolean allHeadersValid = true;
        StringBuilder errors = new StringBuilder();
        
        for (Map<String, String> row : headerRows) {
            String headerName = row.get("header");
            String expectedValue = row.get("value");
            
            if (responseHeaders.containsKey(headerName)) {
                String actualValue = responseHeaders.get(headerName);
                boolean matches = actualValue.contains(expectedValue);
                
                logger.info("Header {}: Expected contains '{}', Actual '{}', Match: {}", 
                        headerName, expectedValue, actualValue, matches);
                
                if (!matches) {
                    allHeadersValid = false;
                    errors.append("Header ").append(headerName)
                          .append(" has incorrect value. Expected to contain: ")
                          .append(expectedValue).append(", Actual: ")
                          .append(actualValue).append("\n");
                }
            } else {
                allHeadersValid = false;
                errors.append("Missing required header: ").append(headerName).append("\n");
                logger.warn("Missing required header: {}", headerName);
            }
        }
        
        if (!allHeadersValid) {
            Allure.addAttachment("Header Validation Errors", "text/plain", errors.toString());
            Assert.fail("Security headers are not properly configured:\n" + errors.toString());
        }
    }
}
