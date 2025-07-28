package com.securitytests.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Page Object representing the Login Page of the application
 */
public class LoginPage extends BasePage {
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/etUsername")
    @iOSXCUITFindBy(accessibility = "username_input")
    private WebElement usernameField;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/etPassword")
    @iOSXCUITFindBy(accessibility = "password_input")
    private WebElement passwordField;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/btnLogin")
    @iOSXCUITFindBy(accessibility = "login_button")
    private WebElement loginButton;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/tvForgotPassword")
    @iOSXCUITFindBy(accessibility = "forgot_password_link")
    private WebElement forgotPasswordLink;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/tvErrorMessage")
    @iOSXCUITFindBy(accessibility = "error_message")
    private WebElement errorMessageText;
    
    // CAPTCHA related elements
    @AndroidFindBy(id = "com.securitytests.demoapp:id/captchaContainer")
    @iOSXCUITFindBy(accessibility = "captcha_container")
    private WebElement captchaContainer;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/etCaptchaInput")
    @iOSXCUITFindBy(accessibility = "captcha_input")
    private WebElement captchaInputField;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/btnVerifyCaptcha")
    @iOSXCUITFindBy(accessibility = "verify_captcha_button")
    private WebElement verifyCaptchaButton;
    
    private static final By CAPTCHA_CONTAINER_LOCATOR = By.id("com.securitytests.demoapp:id/captchaContainer");

    /**
     * Constructor
     */
    public LoginPage() {
        super();
    }
    
    @Step("Entering username: {0}")
    public LoginPage enterUsername(String username) {
        enterText(usernameField, username);
        return this;
    }
    
    @Step("Entering password: {0}")
    public LoginPage enterPassword(String password) {
        enterText(passwordField, password);
        return this;
    }
    
    @Step("Clicking login button")
    public void clickLogin() {
        click(loginButton);
    }
    
    @Step("Performing login with username: {0} and password: {1}")
    public DashboardPage login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
        
        // Check if we're redirected to dashboard or if we have captcha/error
        if (!isCaptchaDisplayed() && !isErrorMessageDisplayed()) {
            return new DashboardPage();
        }
        
        return null;
    }
    
    @Step("Clicking on 'Forgot Password' link")
    public PasswordRecoveryPage goToPasswordRecovery() {
        click(forgotPasswordLink);
        return new PasswordRecoveryPage();
    }
    
    @Step("Checking if error message is displayed")
    public boolean isErrorMessageDisplayed() {
        return isElementDisplayed(errorMessageText);
    }
    
    @Step("Getting error message text")
    public String getErrorMessage() {
        return getText(errorMessageText);
    }
    
    @Step("Checking if CAPTCHA is displayed")
    public boolean isCaptchaDisplayed() {
        return isElementDisplayed(CAPTCHA_CONTAINER_LOCATOR, 5);
    }
    
    @Step("Entering CAPTCHA text: {0}")
    public LoginPage enterCaptcha(String captchaText) {
        if (isCaptchaDisplayed()) {
            enterText(captchaInputField, captchaText);
        }
        return this;
    }
    
    @Step("Verifying CAPTCHA")
    public DashboardPage verifyCaptchaAndLogin() {
        click(verifyCaptchaButton);
        return new DashboardPage();
    }
}
