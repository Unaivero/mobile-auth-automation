package com.securitytests.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Page Object representing the Password Recovery flow
 */
public class PasswordRecoveryPage extends BasePage {
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/etEmail")
    @iOSXCUITFindBy(accessibility = "email_input")
    private WebElement emailField;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/btnSendRecoveryEmail")
    @iOSXCUITFindBy(accessibility = "send_recovery_email_button")
    private WebElement sendRecoveryButton;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/tvRecoveryMessage")
    @iOSXCUITFindBy(accessibility = "recovery_message")
    private WebElement recoveryMessage;
    
    // Password reset token screen elements
    @AndroidFindBy(id = "com.securitytests.demoapp:id/etResetToken")
    @iOSXCUITFindBy(accessibility = "reset_token_input")
    private WebElement tokenField;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/btnVerifyToken")
    @iOSXCUITFindBy(accessibility = "verify_token_button")
    private WebElement verifyTokenButton;
    
    // New password screen elements
    @AndroidFindBy(id = "com.securitytests.demoapp:id/etNewPassword")
    @iOSXCUITFindBy(accessibility = "new_password_input")
    private WebElement newPasswordField;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/etConfirmPassword")
    @iOSXCUITFindBy(accessibility = "confirm_password_input")
    private WebElement confirmPasswordField;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/btnResetPassword")
    @iOSXCUITFindBy(accessibility = "reset_password_button")
    private WebElement resetPasswordButton;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/tvResetErrorMessage")
    @iOSXCUITFindBy(accessibility = "reset_error_message")
    private WebElement resetErrorMessage;
    
    /**
     * Constructor
     */
    public PasswordRecoveryPage() {
        super();
    }
    
    @Step("Entering email address: {0}")
    public PasswordRecoveryPage enterEmail(String email) {
        enterText(emailField, email);
        return this;
    }
    
    @Step("Requesting password recovery for email: {0}")
    public PasswordRecoveryPage requestPasswordRecovery(String email) {
        enterEmail(email);
        click(sendRecoveryButton);
        return this;
    }
    
    @Step("Checking if recovery confirmation message is displayed")
    public boolean isRecoveryMessageDisplayed() {
        return isElementDisplayed(recoveryMessage);
    }
    
    @Step("Getting recovery message text")
    public String getRecoveryMessage() {
        return getText(recoveryMessage);
    }
    
    @Step("Entering reset token: {0}")
    public PasswordRecoveryPage enterToken(String token) {
        enterText(tokenField, token);
        return this;
    }
    
    @Step("Verifying token")
    public PasswordRecoveryPage verifyToken(String token) {
        enterToken(token);
        click(verifyTokenButton);
        return this;
    }
    
    @Step("Entering new password: {0}")
    public PasswordRecoveryPage enterNewPassword(String password) {
        enterText(newPasswordField, password);
        return this;
    }
    
    @Step("Confirming new password: {0}")
    public PasswordRecoveryPage confirmNewPassword(String password) {
        enterText(confirmPasswordField, password);
        return this;
    }
    
    @Step("Resetting password with new value: {0}")
    public LoginPage resetPassword(String newPassword) {
        enterNewPassword(newPassword);
        confirmNewPassword(newPassword);
        click(resetPasswordButton);
        return new LoginPage();
    }
    
    @Step("Completing full password reset flow")
    public LoginPage completePasswordReset(String token, String newPassword) {
        verifyToken(token);
        return resetPassword(newPassword);
    }
    
    @Step("Getting reset error message")
    public String getResetErrorMessage() {
        return isElementDisplayed(resetErrorMessage) ? getText(resetErrorMessage) : "";
    }
}
