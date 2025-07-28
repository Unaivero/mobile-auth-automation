package com.securitytests.ui;

import com.securitytests.pages.BasePage;
import com.securitytests.pages.LoginPage;
import com.securitytests.pages.PasswordResetPage;
import com.securitytests.utils.ConfigReader;
import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.visual.VisualComparisonResult;
import com.securitytests.utils.visual.VisualValidator;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.*;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.awt.*;
import java.util.UUID;

/**
 * Visual tests for authentication screens
 */
@Epic("Authentication Security")
@Feature("Visual Testing")
public class VisualTests {
    private AppiumDriver driver;
    private VisualValidator visualValidator;
    private LoginPage loginPage;
    private PasswordResetPage passwordResetPage;
    private static final StructuredLogger logger = new StructuredLogger(VisualTests.class);
    private String testId;
    private boolean updateBaselines;
    
    @BeforeClass
    public void setupClass() {
        // Initialize configuration
        ConfigReader configReader = new ConfigReader();
        
        // Check if we should update baselines
        updateBaselines = Boolean.parseBoolean(configReader.getProperty("visual.updateBaselines", "false"));
        
        if (updateBaselines) {
            logger.info("Running in baseline update mode - will create or update all visual baselines");
        }
    }
    
    @BeforeMethod
    public void setupTest() {
        // Start a new test session
        testId = logger.startTest("Visual Test");
        
        // Initialize the driver using BasePage (which handles driver setup)
        BasePage basePage = new BasePage();
        this.driver = basePage.driver;
        
        // Initialize the visual validator
        visualValidator = new VisualValidator(driver);
        
        logger.info("Initialized visual test environment");
    }
    
    @AfterMethod
    public void tearDown(ITestResult result) {
        // Take a screenshot if the test failed
        if (result.getStatus() == ITestResult.FAILURE) {
            visualValidator.captureAndAttachScreenshot("Failure Screenshot");
        }
        
        // End the test session
        if (result.isSuccess()) {
            logger.endTest("pass");
        } else {
            logger.endTest("fail");
        }
        
        // Quit the driver
        if (driver != null) {
            driver.quit();
        }
    }
    
    @Test(description = "Login screen visual test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Login Screen Visual Validation")
    @Description("Verify the login screen appears as expected")
    public void testLoginScreenAppearance() {
        // Navigate to login screen
        loginPage = new LoginPage();
        Allure.step("Navigating to login screen");
        
        // Let the screen stabilize
        loginPage.waitForPageLoad();
        
        // Run visual validation
        String baselineName = "login_screen";
        
        // Update baseline if requested
        if (updateBaselines) {
            visualValidator.saveBaseline(visualValidator.captureScreen(), baselineName);
            logger.info("Updated baseline for login screen");
            return;
        }
        
        // Compare with baseline
        VisualComparisonResult result = visualValidator.validateScreen(baselineName, "LoginScreenTest");
        
        // Log findings
        logger.info("Completed visual validation for login screen: {}", result);
        
        // Assert no differences
        if (!result.isNewBaseline()) {
            Assert.assertFalse(result.hasDifference(), 
                "Login screen should match visual baseline");
        }
    }
    
    @Test(description = "Password reset screen visual test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Password Reset Screen Visual Validation")
    @Description("Verify the password reset screen appears as expected")
    public void testPasswordResetScreenAppearance() {
        // Navigate to login screen first
        loginPage = new LoginPage();
        
        // Navigate to password reset screen
        Allure.step("Navigating to password reset screen");
        passwordResetPage = loginPage.navigateToPasswordReset();
        
        // Let the screen stabilize
        passwordResetPage.waitForPageLoad();
        
        // Run visual validation
        String baselineName = "password_reset_screen";
        
        // Update baseline if requested
        if (updateBaselines) {
            visualValidator.saveBaseline(visualValidator.captureScreen(), baselineName);
            logger.info("Updated baseline for password reset screen");
            return;
        }
        
        // Compare with baseline
        VisualComparisonResult result = visualValidator.validateScreen(baselineName, "PasswordResetScreenTest");
        
        // Log findings
        logger.info("Completed visual validation for password reset screen: {}", result);
        
        // Assert no differences
        if (!result.isNewBaseline()) {
            Assert.assertFalse(result.hasDifference(), 
                "Password reset screen should match visual baseline");
        }
    }
    
    @Test(description = "Login button visual test")
    @Severity(SeverityLevel.HIGH)
    @Story("UI Element Visual Validation")
    @Description("Verify the login button appears as expected")
    public void testLoginButtonAppearance() {
        // Navigate to login screen
        loginPage = new LoginPage();
        Allure.step("Navigating to login screen");
        
        // Let the screen stabilize
        loginPage.waitForPageLoad();
        
        // Get login button
        WebElement loginButton = loginPage.getLoginButton();
        
        // Run visual validation
        String baselineName = "login_button";
        
        // Update baseline if requested
        if (updateBaselines) {
            visualValidator.saveBaseline(visualValidator.captureElement(loginButton), baselineName);
            logger.info("Updated baseline for login button");
            return;
        }
        
        // Compare with baseline
        VisualComparisonResult result = visualValidator.validateElement(loginButton, baselineName, "LoginButtonTest");
        
        // Log findings
        logger.info("Completed visual validation for login button: {}", result);
        
        // Assert no differences
        if (!result.isNewBaseline()) {
            Assert.assertFalse(result.hasDifference(), 
                "Login button should match visual baseline");
        }
    }
    
    @Test(description = "CAPTCHA visual test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("CAPTCHA Visual Validation")
    @Description("Verify the CAPTCHA appearance")
    public void testCaptchaAppearance() {
        // Navigate to login screen
        loginPage = new LoginPage();
        
        // Trigger CAPTCHA display (attempt multiple failed logins)
        Allure.step("Triggering CAPTCHA display");
        loginPage.attemptInvalidLogin("baduser", "badpass");
        loginPage.attemptInvalidLogin("baduser", "badpass");
        loginPage.attemptInvalidLogin("baduser", "badpass");
        
        // Let the screen stabilize
        loginPage.waitForCaptchaDisplay();
        
        // Get CAPTCHA element
        WebElement captchaElement = loginPage.getCaptchaElement();
        
        // Use a unique baseline name for CAPTCHA to prevent false failures
        // (since CAPTCHA images change)
        String testRunId = UUID.randomUUID().toString().substring(0, 8);
        String baselineName = "captcha_" + testRunId;
        
        // Save the CAPTCHA appearance for reference
        visualValidator.saveBaseline(visualValidator.captureElement(captchaElement), baselineName);
        
        // Log for information only - no comparison since CAPTCHA should change
        logger.info("Captured CAPTCHA appearance with ID: {}", testRunId);
        
        // Attach CAPTCHA image to Allure report
        visualValidator.captureAndAttachScreenshot("CAPTCHA Image");
        
        // No assertions since CAPTCHA images are expected to change
    }
    
    @Test(description = "Form field validation visual test")
    @Severity(SeverityLevel.NORMAL)
    @Story("Form Validation Visual Testing")
    @Description("Verify the visual appearance of form field validation")
    public void testFormValidationAppearance() {
        // Navigate to login screen
        loginPage = new LoginPage();
        
        // Attempt login with empty fields to trigger validation
        Allure.step("Triggering form validation");
        loginPage.clickLogin();
        
        // Let the validation appear
        loginPage.waitForValidationMessages();
        
        // Run visual validation
        String baselineName = "form_validation";
        
        // Update baseline if requested
        if (updateBaselines) {
            visualValidator.saveBaseline(visualValidator.captureScreen(), baselineName);
            logger.info("Updated baseline for form validation");
            return;
        }
        
        // Compare with baseline
        VisualComparisonResult result = visualValidator.validateScreen(baselineName, "FormValidationTest");
        
        // Log findings
        logger.info("Completed visual validation for form validation: {}", result);
        
        // Assert no differences
        if (!result.isNewBaseline()) {
            Assert.assertFalse(result.hasDifference(), 
                "Form validation should match visual baseline");
        }
    }
    
    @Test(description = "Masked test for dynamic content")
    @Severity(SeverityLevel.NORMAL)
    @Story("Dynamic Content Visual Testing")
    @Description("Verify screen appearance while masking dynamic content")
    public void testMaskedScreenAppearance() {
        // Navigate to login screen
        loginPage = new LoginPage();
        
        // Let the screen stabilize
        loginPage.waitForPageLoad();
        
        // Capture the screen
        Rectangle[] dynamicRegions = new Rectangle[] {
            // Example: mask timestamp or other dynamic areas
            new Rectangle(0, 0, 200, 50),
            // Additional dynamic regions can be added here
        };
        
        // Create masked screenshot
        String baselineName = "login_screen_masked";
        
        // Update baseline if requested
        if (updateBaselines) {
            visualValidator.saveBaseline(
                visualValidator.createMaskedScreenshot(
                    visualValidator.captureScreen(),
                    dynamicRegions
                ),
                baselineName
            );
            logger.info("Updated baseline for masked login screen");
            return;
        }
        
        // Compare with baseline
        VisualComparisonResult result = visualValidator.compareWithBaseline(
            visualValidator.createMaskedScreenshot(
                visualValidator.captureScreen(),
                dynamicRegions
            ),
            baselineName,
            "MaskedLoginScreenTest"
        );
        
        // Log findings
        logger.info("Completed visual validation for masked login screen: {}", result);
        
        // Assert no differences
        if (!result.isNewBaseline()) {
            Assert.assertFalse(result.hasDifference(), 
                "Masked login screen should match visual baseline");
        }
    }
}
