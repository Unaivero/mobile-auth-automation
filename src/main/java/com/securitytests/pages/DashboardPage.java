package com.securitytests.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Page Object representing the Dashboard/Home Page after successful login
 */
public class DashboardPage extends BasePage {
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/tvWelcomeMessage")
    @iOSXCUITFindBy(accessibility = "welcome_message")
    private WebElement welcomeMessage;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/btnLogout")
    @iOSXCUITFindBy(accessibility = "logout_button")
    private WebElement logoutButton;
    
    @AndroidFindBy(id = "com.securitytests.demoapp:id/btnAccountSettings")
    @iOSXCUITFindBy(accessibility = "account_settings_button")
    private WebElement accountSettingsButton;
    
    /**
     * Constructor
     */
    public DashboardPage() {
        super();
    }
    
    @Step("Checking if user is logged in (Dashboard page is displayed)")
    public boolean isUserLoggedIn() {
        return isElementDisplayed(welcomeMessage);
    }
    
    @Step("Getting welcome message text")
    public String getWelcomeMessage() {
        return getText(welcomeMessage);
    }
    
    @Step("Clicking logout button")
    public LoginPage logout() {
        click(logoutButton);
        return new LoginPage();
    }
    
    @Step("Going to account settings")
    public void goToAccountSettings() {
        click(accountSettingsButton);
        // Return appropriate page object if needed
    }
}
