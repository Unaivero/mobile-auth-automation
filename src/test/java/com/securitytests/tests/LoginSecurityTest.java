package com.securitytests.tests;

import com.securitytests.config.AppiumConfig;
import com.securitytests.pages.DashboardPage;
import com.securitytests.pages.LoginPage;
import com.securitytests.utils.TestDataProvider;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for login security features including failed attempts and CAPTCHA challenges
 */
@Epic("Authentication Security")
@Feature("Login Security")
public class LoginSecurityTest extends BaseTest {
    
    private LoginPage loginPage;
    private static final int MAX_ALLOWED_ATTEMPTS = Integer.parseInt(
            AppiumConfig.getProperty("max.login.attempts"));
    
    @BeforeMethod
    public void setupTest() {
        loginPage = new LoginPage();
    }
    
    @Test(dataProvider = "invalidLoginCredentials", dataProviderClass = TestDataProvider.class)
    @Description("Verify that invalid login credentials show appropriate error message")
    @Severity(SeverityLevel.NORMAL)
    @Story("Invalid Login Validation")
    public void testInvalidLogin(String username, String password, String expectedError) {
        LOGGER.info("Testing invalid login with username: {}", username);
        
        // Attempt login with invalid credentials
        loginPage.login(username, password);
        
        // Verify error message is displayed
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        Assert.assertEquals(loginPage.getErrorMessage(), expectedError, "Error message does not match");
    }
    
    @Test(dataProvider = "validLoginCredentials", dataProviderClass = TestDataProvider.class)
    @Description("Verify that valid login credentials successfully logs in user")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Successful Login")
    public void testValidLogin(String username, String password) {
        LOGGER.info("Testing valid login with username: {}", username);
        
        // Attempt login with valid credentials
        DashboardPage dashboardPage = loginPage.login(username, password);
        
        // Verify user is logged in
        Assert.assertNotNull(dashboardPage, "Dashboard page should be returned after login");
        Assert.assertTrue(dashboardPage.isUserLoggedIn(), "User should be logged in");
        
        // Verify welcome message contains username
        String welcomeMessage = dashboardPage.getWelcomeMessage();
        Assert.assertTrue(welcomeMessage.contains(username) || 
                welcomeMessage.contains("Welcome"), "Welcome message should contain username or welcome text");
    }
    
    @Test
    @Description("Verify that three consecutive failed login attempts trigger CAPTCHA challenge")
    @Severity(SeverityLevel.CRITICAL)
    @Story("CAPTCHA After Failed Attempts")
    public void testCaptchaAfterFailedAttempts() {
        LOGGER.info("Testing CAPTCHA after {} failed login attempts", MAX_ALLOWED_ATTEMPTS);
        
        // Perform multiple failed login attempts
        for (int i = 0; i < MAX_ALLOWED_ATTEMPTS; i++) {
            LOGGER.info("Failed login attempt #{}", (i + 1));
            loginPage.enterUsername("failed_user_" + i);
            loginPage.enterPassword("wrong_password");
            loginPage.clickLogin();
            
            // Verify error message
            Assert.assertTrue(loginPage.isErrorMessageDisplayed(), 
                    "Error message should be displayed on attempt " + (i + 1));
        }
        
        // Verify CAPTCHA appears after allowed number of attempts
        Assert.assertTrue(loginPage.isCaptchaDisplayed(), "CAPTCHA should be displayed after " 
                + MAX_ALLOWED_ATTEMPTS + " failed login attempts");
        
        // Test CAPTCHA validation
        String mockCaptchaValue = "123456"; // In real tests, would need to extract the value
        loginPage.enterCaptcha(mockCaptchaValue);
        
        // Try to login with valid credentials after CAPTCHA
        DashboardPage dashboardPage = loginPage.verifyCaptchaAndLogin();
        
        // Verify successful login after CAPTCHA
        Assert.assertNotNull(dashboardPage, "Dashboard page should be returned after CAPTCHA validation");
        Assert.assertTrue(dashboardPage.isUserLoggedIn(), "User should be logged in after CAPTCHA validation");
    }
}
