package com.securitytests.api;

import com.securitytests.api.auth.AuthApiService;
import com.securitytests.utils.ConfigReader;
import com.securitytests.utils.data.DataGenerator;
import com.securitytests.utils.data.ExternalDataReader;
import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.retry.RetryAnalyzer;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

/**
 * API-level tests for authentication flows
 */
@Epic("Authentication Security")
@Feature("API Authentication Tests")
public class AuthApiTests {
    private AuthApiService authApiService;
    private static final StructuredLogger logger = new StructuredLogger(AuthApiTests.class);
    private DataGenerator dataGenerator;
    private ExternalDataReader dataReader;
    private String testEmail;
    
    @BeforeClass
    public void setup() {
        ConfigReader configReader = new ConfigReader();
        String baseUrl = configReader.getProperty("api.baseUrl");
        authApiService = new AuthApiService(baseUrl);
        dataGenerator = new DataGenerator();
        dataReader = new ExternalDataReader();
        testEmail = configReader.getProperty("test.email");
        
        logger.info("Initialized API test environment with base URL: {}", baseUrl);
    }
    
    @Test(description = "Valid login test")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Login Authentication")
    @Description("Test successful login with valid credentials")
    @RetryAnalyzer(RetryAnalyzer.class)
    public void testValidLogin() throws IOException {
        logger.startTest("Valid API Login Test");
        
        // Get credentials from test data
        Map<String, String> credentials = dataReader.readLoginCredentials().get("valid_user");
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        // Perform login
        ApiResponse response = authApiService.login(username, password);
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for successful login");
        Assert.assertTrue(response.isSuccess(), "Response should indicate success");
        Assert.assertNotNull(response.getJsonValue("data.token"), "Response should contain an authentication token");
        Assert.assertNotNull(response.getJsonValue("data.userId"), "Response should contain a user ID");
        
        logger.endTest("pass");
    }
    
    @Test(description = "Invalid login test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Login Authentication")
    @Description("Test login failure with invalid credentials")
    @RetryAnalyzer(RetryAnalyzer.class)
    public void testInvalidLogin() throws IOException {
        logger.startTest("Invalid API Login Test");
        
        // Generate invalid credentials
        String username = dataGenerator.generateUsername();
        String password = dataGenerator.generatePassword();
        
        // Perform login
        ApiResponse response = authApiService.login(username, password);
        
        // Verify response
        Assert.assertFalse(response.isSuccess(), "Response should indicate failure");
        Assert.assertTrue(response.getStatusCode() == 401 || response.getStatusCode() == 403, 
                "Status code should be 401 or 403 for invalid login");
        Assert.assertNull(response.getJsonValue("data.token"), "Response should not contain an authentication token");
        
        logger.endTest("pass");
    }
    
    @Test(description = "Password reset request test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Password Recovery")
    @Description("Test password reset request functionality")
    @RetryAnalyzer(RetryAnalyzer.class)
    public void testPasswordResetRequest() throws IOException {
        logger.startTest("Password Reset Request API Test");
        
        // Use test email from configuration
        String email = testEmail;
        
        // Request password reset
        ApiResponse response = authApiService.requestPasswordReset(email);
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for password reset request");
        Assert.assertTrue(response.isSuccess(), "Response should indicate success");
        
        logger.endTest("pass");
    }
    
    @DataProvider(name = "loginBruteForceData")
    public Object[][] loginBruteForceData() {
        return new Object[][] {
            {"user1", "wrongpass1"},
            {"user1", "wrongpass2"},
            {"user1", "wrongpass3"},
            {"user1", "wrongpass4"},
            {"user1", "wrongpass5"}
        };
    }
    
    @Test(description = "Login brute force protection test", dataProvider = "loginBruteForceData")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Login Security")
    @Description("Test that brute force protection activates after multiple failed login attempts")
    @RetryAnalyzer(RetryAnalyzer.class)
    public void testLoginBruteForceProtection(String username, String password) throws IOException {
        logger.startTest("Login Brute Force Protection Test");
        logger.info("Attempting login with username: {} (attempt in sequence)", username);
        
        // Perform login
        ApiResponse response = authApiService.login(username, password);
        
        // For the last attempt, we expect to see CAPTCHA required or account locked
        if (password.equals("wrongpass5")) {
            boolean hasCaptchaOrLocked = response.getJsonValue("data.requiresCaptcha") != null || 
                                        response.getJsonValue("data.accountLocked") != null;
                                        
            Assert.assertTrue(hasCaptchaOrLocked, 
                "After multiple failed attempts, response should indicate CAPTCHA required or account locked");
        }
        
        logger.endTest("pass");
    }
    
    @Test(description = "CAPTCHA validation test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("CAPTCHA Security")
    @Description("Test CAPTCHA validation functionality")
    @RetryAnalyzer(RetryAnalyzer.class)
    public void testCaptchaValidation() throws IOException {
        logger.startTest("CAPTCHA Validation API Test");
        
        // Request a new CAPTCHA
        ApiResponse captchaResponse = authApiService.requestCaptcha();
        Assert.assertTrue(captchaResponse.isSuccess(), "CAPTCHA request should be successful");
        
        String captchaId = captchaResponse.getJsonValue("data.captchaId");
        Assert.assertNotNull(captchaId, "CAPTCHA ID should be returned");
        
        // Test with invalid solution
        String invalidSolution = "wrongsolution";
        ApiResponse invalidResponse = authApiService.validateCaptcha(captchaId, invalidSolution);
        
        Assert.assertTrue(invalidResponse.isSuccess(), "CAPTCHA validation request should be successful");
        Assert.assertEquals(invalidResponse.getJsonValue("data.valid"), "false", 
            "Invalid CAPTCHA solution should be rejected");
        
        logger.endTest("pass");
    }
    
    @Test(description = "User profile API test", dependsOnMethods = "testValidLogin")
    @Severity(SeverityLevel.NORMAL)
    @Story("User Profile")
    @Description("Test retrieving user profile information")
    @RetryAnalyzer(RetryAnalyzer.class)
    public void testGetUserProfile() throws IOException {
        logger.startTest("Get User Profile API Test");
        
        // Log in first (reusing the valid login test)
        Map<String, String> credentials = dataReader.readLoginCredentials().get("valid_user");
        authApiService.login(credentials.get("username"), credentials.get("password"));
        
        // Get user profile
        ApiResponse response = authApiService.getUserProfile();
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for profile request");
        Assert.assertTrue(response.isSuccess(), "Response should indicate success");
        Assert.assertEquals(response.getJsonValue("data.username"), credentials.get("username"), 
            "Username in profile should match the logged in user");
        
        logger.endTest("pass");
    }
    
    @Test(description = "Logout API test", dependsOnMethods = "testGetUserProfile")
    @Severity(SeverityLevel.NORMAL)
    @Story("Logout")
    @Description("Test user logout functionality")
    @RetryAnalyzer(RetryAnalyzer.class)
    public void testLogout() throws IOException {
        logger.startTest("Logout API Test");
        
        // Log out
        ApiResponse response = authApiService.logout();
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "Status code should be 200 for logout");
        Assert.assertTrue(response.isSuccess(), "Response should indicate success");
        
        // Verify that profile access is no longer allowed
        ApiResponse profileResponse = authApiService.getUserProfile();
        Assert.assertFalse(profileResponse.isSuccess(), "Profile request should fail after logout");
        Assert.assertTrue(profileResponse.getStatusCode() == 401 || profileResponse.getStatusCode() == 403, 
            "Status code should be 401 or 403 for unauthorized profile request");
        
        logger.endTest("pass");
    }
}
