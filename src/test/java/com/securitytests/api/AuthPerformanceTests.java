package com.securitytests.api;

import com.securitytests.api.auth.AuthApiService;
import com.securitytests.utils.ConfigReader;
import com.securitytests.utils.data.DataGenerator;
import com.securitytests.utils.data.ExternalDataReader;
import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.performance.PerformanceMonitor;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Performance tests for authentication API endpoints
 */
@Epic("Authentication Security")
@Feature("Performance Testing")
public class AuthPerformanceTests {
    private AuthApiService authApiService;
    private static final StructuredLogger logger = new StructuredLogger(AuthPerformanceTests.class);
    private DataGenerator dataGenerator;
    private ExternalDataReader dataReader;
    private String testEmail;
    private String testId;
    
    @BeforeClass
    public void setup() {
        ConfigReader configReader = new ConfigReader();
        String baseUrl = configReader.getProperty("api.baseUrl");
        authApiService = new AuthApiService(baseUrl);
        dataGenerator = new DataGenerator();
        dataReader = new ExternalDataReader();
        testEmail = configReader.getProperty("test.email");
        testId = "auth_perf_" + UUID.randomUUID().toString();
        
        // Initialize performance monitoring
        PerformanceMonitor.resetStats();
        PerformanceMonitor.startTest(testId);
        
        logger.info("Initialized performance test environment with base URL: {}", baseUrl);
    }
    
    @AfterClass
    public void teardown() {
        // Generate performance report
        PerformanceMonitor.generatePerformanceReport();
        PerformanceMonitor.endTest();
    }
    
    @Test(description = "Login performance test", invocationCount = 10)
    @Severity(SeverityLevel.CRITICAL)
    @Story("Login Performance")
    @Description("Measure performance of login API endpoint")
    public void testLoginPerformance() throws IOException {
        logger.startTest("Login Performance Test");
        
        // Get credentials from test data
        Map<String, String> credentials = dataReader.readLoginCredentials().get("valid_user");
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        // Start timing
        String timerId = PerformanceMonitor.startTimer("login_api");
        
        // Perform login
        ApiResponse response = authApiService.login(username, password);
        
        // Stop timing and record results
        long duration = PerformanceMonitor.stopTimer(timerId);
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "Login request should be successful");
        
        // Log performance data
        logger.info("Login API call completed in {} ms", duration);
        
        // Performance assertions
        Assert.assertTrue(duration < 2000, "Login API call should complete in under 2 seconds");
        
        logger.endTest("pass");
    }
    
    @Test(description = "Password reset request performance test", invocationCount = 5)
    @Severity(SeverityLevel.CRITICAL)
    @Story("Password Reset Performance")
    @Description("Measure performance of password reset request API endpoint")
    public void testPasswordResetRequestPerformance() throws IOException {
        logger.startTest("Password Reset Request Performance Test");
        
        // Generate a unique email for each test to avoid rate limiting
        String email = dataGenerator.generateEmail();
        
        // Start timing
        String timerId = PerformanceMonitor.startTimer("password_reset_request_api");
        
        // Request password reset
        ApiResponse response = authApiService.requestPasswordReset(email);
        
        // Stop timing and record results
        long duration = PerformanceMonitor.stopTimer(timerId);
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "Password reset request should be successful");
        
        // Log performance data
        logger.info("Password reset request API call completed in {} ms", duration);
        
        // Performance assertions
        Assert.assertTrue(duration < 1500, "Password reset request API call should complete in under 1.5 seconds");
        
        // Wait between invocations to avoid rate limiting
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        logger.endTest("pass");
    }
    
    @Test(description = "CAPTCHA request performance test", invocationCount = 10)
    @Severity(SeverityLevel.CRITICAL)
    @Story("CAPTCHA Performance")
    @Description("Measure performance of CAPTCHA request API endpoint")
    public void testCaptchaRequestPerformance() throws IOException {
        logger.startTest("CAPTCHA Request Performance Test");
        
        // Start timing
        String timerId = PerformanceMonitor.startTimer("captcha_request_api");
        
        // Request a new CAPTCHA
        ApiResponse response = authApiService.requestCaptcha();
        
        // Stop timing and record results
        long duration = PerformanceMonitor.stopTimer(timerId);
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "CAPTCHA request should be successful");
        
        // Log performance data
        logger.info("CAPTCHA request API call completed in {} ms", duration);
        
        // Performance assertions
        Assert.assertTrue(duration < 1000, "CAPTCHA request API call should complete in under 1 second");
        
        logger.endTest("pass");
    }
    
    @Test(description = "User profile performance test", invocationCount = 5, dependsOnMethods = "testLoginPerformance")
    @Severity(SeverityLevel.NORMAL)
    @Story("Profile Performance")
    @Description("Measure performance of user profile API endpoint")
    public void testUserProfilePerformance() throws IOException {
        logger.startTest("User Profile Performance Test");
        
        // Log in first
        Map<String, String> credentials = dataReader.readLoginCredentials().get("valid_user");
        authApiService.login(credentials.get("username"), credentials.get("password"));
        
        // Start timing
        String timerId = PerformanceMonitor.startTimer("get_user_profile_api");
        
        // Get user profile
        ApiResponse response = authApiService.getUserProfile();
        
        // Stop timing and record results
        long duration = PerformanceMonitor.stopTimer(timerId);
        
        // Verify response
        Assert.assertEquals(response.getStatusCode(), 200, "Profile request should be successful");
        
        // Log performance data
        logger.info("User profile API call completed in {} ms", duration);
        
        // Performance assertions
        Assert.assertTrue(duration < 800, "User profile API call should complete in under 800 ms");
        
        logger.endTest("pass");
    }
    
    @Test(description = "End-to-end login flow performance test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("E2E Performance")
    @Description("Measure performance of complete login flow including validation")
    public void testEndToEndLoginPerformance() throws IOException {
        logger.startTest("End-to-End Login Flow Performance Test");
        
        // Get credentials from test data
        Map<String, String> credentials = dataReader.readLoginCredentials().get("valid_user");
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        // Start timing the entire flow
        String flowTimerId = PerformanceMonitor.startTimer("e2e_login_flow");
        
        // Step 1: Request CAPTCHA
        String captchaTimerId = PerformanceMonitor.startTimer("captcha_request_api");
        ApiResponse captchaResponse = authApiService.requestCaptcha();
        PerformanceMonitor.stopTimer(captchaTimerId);
        
        String captchaId = captchaResponse.getJsonValue("data.captchaId");
        Assert.assertNotNull(captchaId, "CAPTCHA ID should be returned");
        
        // Step 2: Validate CAPTCHA (simulated)
        String validateTimerId = PerformanceMonitor.startTimer("captcha_validate_api");
        ApiResponse validateResponse = authApiService.validateCaptcha(captchaId, "simulated_solution");
        PerformanceMonitor.stopTimer(validateTimerId);
        
        // Step 3: Login
        String loginTimerId = PerformanceMonitor.startTimer("login_api");
        ApiResponse loginResponse = authApiService.login(username, password);
        PerformanceMonitor.stopTimer(loginTimerId);
        
        // Stop timing the entire flow
        long flowDuration = PerformanceMonitor.stopTimer(flowTimerId);
        
        // Log performance data
        logger.info("End-to-end login flow completed in {} ms", flowDuration);
        
        // Performance assertions
        Assert.assertTrue(flowDuration < 3000, "End-to-end login flow should complete in under 3 seconds");
        
        logger.endTest("pass");
    }
}
