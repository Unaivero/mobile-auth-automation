package com.securitytests.ui;

import com.securitytests.pages.BasePage;
import com.securitytests.pages.LoginPage;
import com.securitytests.pages.PasswordResetPage;
import com.securitytests.utils.ConfigReader;
import com.securitytests.utils.accessibility.AccessibilityChecker;
import com.securitytests.utils.accessibility.AccessibilityIssue;
import com.securitytests.utils.accessibility.AccessibilityReport;
import com.securitytests.utils.logging.StructuredLogger;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Accessibility tests for authentication screens
 */
@Epic("Authentication Security")
@Feature("Accessibility Testing")
public class AccessibilityTests {
    private AppiumDriver driver;
    private AccessibilityChecker accessibilityChecker;
    private LoginPage loginPage;
    private PasswordResetPage passwordResetPage;
    private static final StructuredLogger logger = new StructuredLogger(AccessibilityTests.class);
    private String testId;
    
    @BeforeClass
    public void setupClass() {
        // Initialize configuration
        ConfigReader configReader = new ConfigReader();
    }
    
    @BeforeMethod
    public void setupTest() {
        // Start a new test session
        testId = logger.startTest("Accessibility Test");
        
        // Initialize the driver using BasePage (which handles driver setup)
        BasePage basePage = new BasePage();
        this.driver = basePage.driver;
        
        // Initialize the accessibility checker
        accessibilityChecker = new AccessibilityChecker(driver);
        
        logger.info("Initialized accessibility test environment");
    }
    
    @AfterMethod
    public void tearDown(ITestResult result) {
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
    
    @Test(description = "Login screen accessibility test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Login Screen Accessibility")
    @Description("Verify the login screen meets accessibility guidelines")
    public void testLoginScreenAccessibility() {
        // Navigate to login screen
        loginPage = new LoginPage();
        
        // Take a step for Allure reporting
        Allure.step("Checking accessibility on login screen");
        
        // Run accessibility audit
        AccessibilityReport report = accessibilityChecker.auditScreen("Login");
        
        // Log findings
        logger.info("Completed accessibility audit for Login screen, found {} issues", 
            report.getIssueCount());
        
        // Attach the report to Allure
        Allure.addAttachment("Login Screen Accessibility Report", report.toString());
        
        // Assert no critical issues
        Assert.assertFalse(report.hasIssuesOfSeverity(AccessibilityIssue.Severity.CRITICAL),
            "Login screen should not have any critical accessibility issues");
    }
    
    @Test(description = "Password reset screen accessibility test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Password Reset Accessibility")
    @Description("Verify the password reset screen meets accessibility guidelines")
    public void testPasswordResetScreenAccessibility() {
        // Navigate to login screen first
        loginPage = new LoginPage();
        
        // Navigate to password reset screen
        Allure.step("Navigating to password reset screen");
        passwordResetPage = loginPage.navigateToPasswordReset();
        
        // Take a step for Allure reporting
        Allure.step("Checking accessibility on password reset screen");
        
        // Run accessibility audit
        AccessibilityReport report = accessibilityChecker.auditScreen("Password Reset");
        
        // Log findings
        logger.info("Completed accessibility audit for Password Reset screen, found {} issues", 
            report.getIssueCount());
        
        // Attach the report to Allure
        Allure.addAttachment("Password Reset Screen Accessibility Report", report.toString());
        
        // Assert no critical issues
        Assert.assertFalse(report.hasIssuesOfSeverity(AccessibilityIssue.Severity.CRITICAL),
            "Password reset screen should not have any critical accessibility issues");
    }
    
    @Test(description = "CAPTCHA screen accessibility test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("CAPTCHA Accessibility")
    @Description("Verify the CAPTCHA screen meets accessibility guidelines")
    public void testCaptchaScreenAccessibility() {
        // Navigate to login screen first
        loginPage = new LoginPage();
        
        // Trigger CAPTCHA display (attempt multiple failed logins)
        Allure.step("Triggering CAPTCHA display");
        loginPage.attemptInvalidLogin("baduser", "badpass");
        loginPage.attemptInvalidLogin("baduser", "badpass");
        loginPage.attemptInvalidLogin("baduser", "badpass");
        
        // Take a step for Allure reporting
        Allure.step("Checking accessibility on CAPTCHA screen");
        
        // Run accessibility audit
        AccessibilityReport report = accessibilityChecker.auditScreen("CAPTCHA");
        
        // Log findings
        logger.info("Completed accessibility audit for CAPTCHA screen, found {} issues", 
            report.getIssueCount());
        
        // Attach the report to Allure
        Allure.addAttachment("CAPTCHA Screen Accessibility Report", report.toString());
        
        // Assert no critical issues
        Assert.assertFalse(report.hasIssuesOfSeverity(AccessibilityIssue.Severity.CRITICAL),
            "CAPTCHA screen should not have any critical accessibility issues");
    }
    
    @Test(description = "Registration screen accessibility test")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Registration Accessibility")
    @Description("Verify the registration screen meets accessibility guidelines")
    public void testRegistrationScreenAccessibility() {
        // Navigate to login screen first
        loginPage = new LoginPage();
        
        // Navigate to registration screen
        Allure.step("Navigating to registration screen");
        loginPage.navigateToRegistration();
        
        // Take a step for Allure reporting
        Allure.step("Checking accessibility on registration screen");
        
        // Run accessibility audit
        AccessibilityReport report = accessibilityChecker.auditScreen("Registration");
        
        // Log findings
        logger.info("Completed accessibility audit for Registration screen, found {} issues", 
            report.getIssueCount());
        
        // Attach the report to Allure
        Allure.addAttachment("Registration Screen Accessibility Report", report.toString());
        
        // Assert no critical issues
        Assert.assertFalse(report.hasIssuesOfSeverity(AccessibilityIssue.Severity.CRITICAL),
            "Registration screen should not have any critical accessibility issues");
    }
}
