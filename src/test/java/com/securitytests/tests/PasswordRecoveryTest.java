package com.securitytests.tests;

import com.mailslurp.models.Email;
import com.securitytests.pages.DashboardPage;
import com.securitytests.pages.LoginPage;
import com.securitytests.pages.PasswordRecoveryPage;
import com.securitytests.utils.MailSlurpUtil;
import com.securitytests.utils.TestDataProvider;
import io.qameta.allure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for password recovery flow including email verification, token validation,
 * and redirection after password reset
 */
@Epic("Authentication Security")
@Feature("Password Recovery")
public class PasswordRecoveryTest extends BaseTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRecoveryTest.class);
    private LoginPage loginPage;
    private PasswordRecoveryPage passwordRecoveryPage;
    private MailSlurpUtil mailSlurp;
    
    @BeforeMethod
    public void setupTest() {
        loginPage = new LoginPage();
        // Initialize MailSlurp for email testing
        mailSlurp = new MailSlurpUtil();
    }
    
    @Test(dataProvider = "passwordRecoveryData", dataProviderClass = TestDataProvider.class)
    @Description("Verify complete password recovery flow including email verification and redirection")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Complete Password Recovery Flow")
    public void testCompletePasswordRecoveryFlow(String emailPlaceholder, String newPassword) {
        LOGGER.info("Testing complete password recovery flow");
        
        // Create an email inbox for testing
        String testEmail = mailSlurp.createInbox();
        LOGGER.info("Created test email address: {}", testEmail);
        
        // Navigate to password recovery
        passwordRecoveryPage = loginPage.goToPasswordRecovery();
        
        // Request password recovery
        passwordRecoveryPage.requestPasswordRecovery(testEmail);
        
        // Verify recovery confirmation message
        Assert.assertTrue(passwordRecoveryPage.isRecoveryMessageDisplayed(), 
                "Recovery message should be displayed");
        Assert.assertTrue(passwordRecoveryPage.getRecoveryMessage().contains("email"), 
                "Recovery message should mention email sent");
        
        // Wait for and retrieve the recovery email
        LOGGER.info("Waiting for password reset email...");
        Email resetEmail = mailSlurp.waitForPasswordResetEmail();
        
        // Extract the reset token
        String resetToken = mailSlurp.extractResetToken(resetEmail);
        LOGGER.info("Extracted reset token: {}", resetToken);
        
        // Complete the password reset
        LoginPage newLoginPage = passwordRecoveryPage.completePasswordReset(resetToken, newPassword);
        
        // Verify redirect to login page
        Assert.assertNotNull(newLoginPage, "Should be redirected to login page after password reset");
        
        // Login with the new password
        DashboardPage dashboardPage = newLoginPage.login(testEmail, newPassword);
        
        // Verify successful login with new password
        Assert.assertNotNull(dashboardPage, "Should be able to login with new password");
        Assert.assertTrue(dashboardPage.isUserLoggedIn(), "User should be logged in after password reset");
    }
    
    @Test(dataProvider = "invalidTokenData", dataProviderClass = TestDataProvider.class)
    @Description("Verify that invalid tokens are rejected with appropriate error messages")
    @Severity(SeverityLevel.HIGH)
    @Story("Token Validation")
    public void testInvalidTokenValidation(String invalidToken, String expectedError) {
        LOGGER.info("Testing invalid token validation with token: {}", invalidToken);
        
        // Navigate to password recovery
        passwordRecoveryPage = loginPage.goToPasswordRecovery();
        
        // Request password recovery with any email (we're just testing token validation)
        passwordRecoveryPage.requestPasswordRecovery("test@example.com");
        
        // Try to use invalid token
        passwordRecoveryPage.verifyToken(invalidToken);
        
        // Verify error message
        String errorMessage = passwordRecoveryPage.getResetErrorMessage();
        Assert.assertTrue(errorMessage.contains(expectedError), 
                "Error message should indicate invalid token");
    }
    
    @Test
    @Description("Verify that token expiration is handled correctly")
    @Severity(SeverityLevel.HIGH)
    @Story("Token Expiration")
    public void testTokenExpiration() {
        LOGGER.info("Testing token expiration handling");
        
        // This test assumes we have a way to generate an expired token or simulate expiration
        // In a real test, we might use a mock server to simulate expired token response
        
        // Navigate to password recovery
        passwordRecoveryPage = loginPage.goToPasswordRecovery();
        
        // Request password recovery with test email
        passwordRecoveryPage.requestPasswordRecovery("test@example.com");
        
        // Use a token that we know is expired (mock would return expired=true)
        String expiredToken = "999999"; // This would be configured in mock server to return expired
        passwordRecoveryPage.verifyToken(expiredToken);
        
        // Verify error message about expiration
        String errorMessage = passwordRecoveryPage.getResetErrorMessage();
        Assert.assertTrue(errorMessage.contains("expired") || errorMessage.contains("Expired"), 
                "Error message should indicate token expiration");
    }
    
    @Test
    @Description("Verify redirection after successful password reset")
    @Severity(SeverityLevel.NORMAL)
    @Story("Reset Redirect")
    public void testRedirectionAfterReset() {
        LOGGER.info("Testing redirection after successful password reset");
        
        // Create an email inbox for testing
        String testEmail = mailSlurp.createInbox();
        
        // Navigate to password recovery
        passwordRecoveryPage = loginPage.goToPasswordRecovery();
        
        // Request password recovery
        passwordRecoveryPage.requestPasswordRecovery(testEmail);
        
        // Get reset token (in real test would use mailSlurp)
        String mockToken = "123456"; // In real test, would get from email
        
        // Complete password reset
        LoginPage resultPage = passwordRecoveryPage.completePasswordReset(mockToken, "NewPassword123!");
        
        // Verify redirect to login page specifically, not any other page
        Assert.assertNotNull(resultPage, "Should be redirected to login page after reset");
        Assert.assertTrue(resultPage instanceof LoginPage, "Should be redirected to login page after reset");
    }
}
