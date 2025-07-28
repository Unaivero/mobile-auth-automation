package com.securitytests.examples;

import com.securitytests.api.ApiResponse;
import com.securitytests.api.auth.AuthApiService;
import com.securitytests.pages.LoginPage;
import com.securitytests.utils.accessibility.AccessibilityChecker;
import com.securitytests.utils.accessibility.AccessibilityReport;
import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.performance.PerformanceMonitor;
import com.securitytests.utils.retry.RetryAnalyzer;
import com.securitytests.utils.visual.VisualComparisonResult;
import com.securitytests.utils.visual.VisualValidator;
import com.securitytests.utils.wait.SmartWait;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration example showing how to use all test utilities together
 */
@Epic("Authentication Security")
@Feature("Integrated Testing Example")
public class IntegrationExampleTest {
    private AppiumDriver driver;
    private LoginPage loginPage;
    private AuthApiService authApiService;
    private AccessibilityChecker accessibilityChecker;
    private VisualValidator visualValidator;
    private SmartWait smartWait;
    private static final StructuredLogger logger = new StructuredLogger(IntegrationExampleTest.class);
    private String testId;
    
    @BeforeClass
    public void setupClass() {
        // Initialize API client
        authApiService = new AuthApiService("https://api.example.com");
        
        // Start performance monitoring for the entire test class
        PerformanceMonitor.resetStats();
        testId = "integrated_test_" + System.currentTimeMillis();
        PerformanceMonitor.startTest(testId);
        
        logger.info("Initialized test environment");
    }
    
    @BeforeMethod
    public void setupTest(ITestResult result) {
        // Start correlation ID for this test method
        String methodName = result.getMethod().getMethodName();
        testId = logger.startTest("Integrated Test - " + methodName);
        
        // Initialize the UI components
        loginPage = new LoginPage();
        this.driver = loginPage.driver;
        
        // Initialize the testing utilities
        accessibilityChecker = new AccessibilityChecker(driver);
        visualValidator = new VisualValidator(driver);
        smartWait = new SmartWait(driver);
        
        logger.info("Test setup complete");
    }
    
    @AfterMethod
    public void tearDown(ITestResult result) {
        if (result.isSuccess()) {
            logger.endTest("pass");
        } else {
            // Capture failure evidence
            visualValidator.captureAndAttachScreenshot("Failure Screenshot");
            logger.endTest("fail");
        }
        
        // Quit the driver
        if (driver != null) {
            driver.quit();
        }
    }
    
    @AfterClass
    public void tearDownClass() {
        // Generate performance report
        PerformanceMonitor.generatePerformanceReport();
        PerformanceMonitor.endTest();
    }
    
    /**
     * Example test that integrates UI, API, accessibility, visual, and performance testing
     */
    @Test(description = "Comprehensive login test with multiple validations", 
          retryAnalyzer = RetryAnalyzer.class)
    @Severity(SeverityLevel.CRITICAL)
    @Story("Integrated Login Validation")
    @Description("Demonstrates integration of UI, API, accessibility, visual, and performance testing")
    public void testIntegratedLoginFlow() {
        // 1. API Call - Pre-validation
        logger.info("Step 1: Pre-validation API call to check service status");
        String statusTimerId = PerformanceMonitor.startTimer("status_check_api");
        ApiResponse statusResponse = authApiService.getUserServiceStatus();
        PerformanceMonitor.stopTimer(statusTimerId);
        
        Assert.assertEquals(statusResponse.getStatusCode(), 200, 
            "Auth service should be available");
        
        // 2. UI Interaction - Login Screen
        logger.info("Step 2: UI interaction - navigating to login screen");
        String uiLoadTimerId = PerformanceMonitor.startTimer("login_page_load");
        loginPage.navigateToLoginScreen();
        PerformanceMonitor.stopTimer(uiLoadTimerId);
        
        // 3. Smart Wait - Wait for screen to be fully loaded and stable
        logger.info("Step 3: Waiting for screen to be stable");
        smartWait.waitForNetworkIdle();
        smartWait.waitForAnimationsToComplete();
        
        // 4. Visual Validation - Login Screen Appearance
        logger.info("Step 4: Visual validation of login screen");
        VisualComparisonResult visualResult = visualValidator.validateScreen(
            "integrated_login_screen", "IntegratedTest");
        
        // We don't fail the test if it's the first run (baseline creation)
        if (!visualResult.isNewBaseline()) {
            Assert.assertFalse(visualResult.hasDifference(), 
                "Login screen should match visual baseline");
        }
        
        // 5. Accessibility Check - Login Screen
        logger.info("Step 5: Accessibility validation of login screen");
        AccessibilityReport accessibilityReport = accessibilityChecker.auditScreen("Login");
        
        // Attach accessibility report to Allure
        Allure.addAttachment("Login Screen Accessibility Report", accessibilityReport.toString());
        
        // 6. API + UI Integration - Login with API then verify in UI
        logger.info("Step 6: API login followed by UI verification");
        
        // 6.1 Perform login via API
        String apiLoginTimerId = PerformanceMonitor.startTimer("api_login");
        ApiResponse loginResponse = authApiService.login("testuser", "testpassword");
        PerformanceMonitor.stopTimer(apiLoginTimerId);
        
        Assert.assertEquals(loginResponse.getStatusCode(), 200, "API login should succeed");
        String authToken = loginResponse.getJsonValue("data.authToken");
        Assert.assertNotNull(authToken, "Auth token should be returned");
        
        // 6.2 Set token in UI context (simulating shared state)
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("authToken", authToken);
        loginPage.setSessionData(tokenData);
        
        // 6.3 Navigate to dashboard (should use token from session)
        String navigationTimerId = PerformanceMonitor.startTimer("navigation_to_dashboard");
        loginPage.navigateToDashboard();
        PerformanceMonitor.stopTimer(navigationTimerId);
        
        // 7. Smart Wait + Visual Validation - Dashboard
        logger.info("Step 7: Smart wait and visual validation of dashboard");
        smartWait.waitForElement(By.id("dashboard_welcome_text"), true);
        
        WebElement welcomeElement = driver.findElement(By.id("dashboard_welcome_text"));
        String welcomeText = welcomeElement.getText();
        
        // Verify welcome text contains username
        Assert.assertTrue(welcomeText.contains("testuser"), 
            "Welcome message should contain the username");
        
        // 8. Performance Validation
        logger.info("Step 8: Performance validation");
        
        // 8.1 Check API login time
        double apiLoginTime = PerformanceMonitor.getMetric("api_login");
        logger.info("API login time: {} ms", apiLoginTime);
        Assert.assertTrue(apiLoginTime < 2000, 
            "API login should complete in under 2 seconds");
        
        // 8.2 Check navigation time
        double navigationTime = PerformanceMonitor.getMetric("navigation_to_dashboard");
        logger.info("Dashboard navigation time: {} ms", navigationTime);
        Assert.assertTrue(navigationTime < 3000, 
            "Navigation to dashboard should complete in under 3 seconds");
        
        // 9. Final Accessibility Check - Dashboard
        logger.info("Step 9: Accessibility validation of dashboard");
        AccessibilityReport dashboardReport = accessibilityChecker.auditScreen("Dashboard");
        
        // Attach dashboard accessibility report to Allure
        Allure.addAttachment("Dashboard Accessibility Report", dashboardReport.toString());
        
        // Log completion
        logger.info("Integrated login test completed successfully");
    }
}
