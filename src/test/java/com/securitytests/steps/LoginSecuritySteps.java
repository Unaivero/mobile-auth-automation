package com.securitytests.steps;

import com.securitytests.config.AppiumConfig;
import com.securitytests.pages.DashboardPage;
import com.securitytests.pages.LoginPage;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

/**
 * Step definitions for login security feature
 */
public class LoginSecuritySteps {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginSecuritySteps.class);
    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    
    @Before
    public void setup() {
        LOGGER.info("Setting up test");
        AppiumConfig.getDriver();
    }
    
    @After
    public void tearDown() {
        LOGGER.info("Tearing down test");
        AppiumConfig.quitDriver();
    }
    
    @Given("the mobile application is launched")
    public void theMobileApplicationIsLaunched() {
        LOGGER.info("Mobile application is launched");
        // The driver is already initialized in setup()
        Allure.step("Mobile application launched successfully");
    }
    
    @Given("I am on the login screen")
    public void iAmOnTheLoginScreen() {
        LOGGER.info("Navigating to login screen");
        loginPage = new LoginPage();
        Allure.step("Navigated to login screen");
    }
    
    @When("I enter username {string} and password {string}")
    public void iEnterUsernameAndPassword(String username, String password) {
        LOGGER.info("Entering username {} and password", username);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        Allure.step("Entered username: " + username + " and password");
    }
    
    @And("I tap on the login button")
    public void iTapOnTheLoginButton() {
        LOGGER.info("Tapping on login button");
        loginPage.clickLogin();
        Allure.step("Tapped on login button");
    }
    
    @Then("I should be successfully logged in")
    public void iShouldBeSuccessfullyLoggedIn() {
        LOGGER.info("Verifying successful login");
        dashboardPage = new DashboardPage();
        Assert.assertTrue(dashboardPage.isUserLoggedIn(), "User should be logged in");
        Allure.step("Successfully logged in");
    }
    
    @And("I should see the dashboard screen")
    public void iShouldSeeTheDashboardScreen() {
        LOGGER.info("Verifying dashboard screen is visible");
        Assert.assertNotNull(dashboardPage, "Dashboard page should be displayed");
        Allure.step("Dashboard screen is displayed");
    }
    
    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String errorMessage) {
        LOGGER.info("Verifying error message: {}", errorMessage);
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should be displayed");
        Assert.assertEquals(loginPage.getErrorMessage(), errorMessage, "Error message does not match");
        Allure.step("Error message displayed: " + errorMessage);
    }
    
    @And("I should remain on the login screen")
    public void iShouldRemainOnTheLoginScreen() {
        LOGGER.info("Verifying still on login screen");
        // If error message is displayed, we're still on the login screen
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Should remain on login screen");
        Allure.step("Remained on login screen");
    }
    
    @When("I attempt to login with invalid credentials {int} times")
    public void iAttemptToLoginWithInvalidCredentialsTimes(int attempts) {
        LOGGER.info("Attempting to login with invalid credentials {} times", attempts);
        
        for (int i = 0; i < attempts; i++) {
            LOGGER.info("Invalid login attempt #{}", i + 1);
            loginPage.enterUsername("failed_user_" + i);
            loginPage.enterPassword("wrong_password");
            loginPage.clickLogin();
            
            // Verify error message on each attempt
            Assert.assertTrue(loginPage.isErrorMessageDisplayed(), 
                    "Error message should be displayed on attempt " + (i + 1));
        }
        
        Allure.step("Attempted login with invalid credentials " + attempts + " times");
    }
    
    @Then("I should see a CAPTCHA challenge")
    public void iShouldSeeACaptchaChallenge() {
        LOGGER.info("Verifying CAPTCHA is displayed");
        Assert.assertTrue(loginPage.isCaptchaDisplayed(), "CAPTCHA should be displayed");
        // Take a screenshot of CAPTCHA for visual validation
        VisualValidator.captureCaptchaScreenshot(loginPage);
        Allure.step("CAPTCHA challenge displayed");
    }
    
    @When("I enter the correct CAPTCHA")
    public void iEnterTheCorrectCaptcha() {
        LOGGER.info("Entering CAPTCHA value");
        loginPage.enterCaptcha("123456"); // Mock value for testing
        Allure.step("Entered CAPTCHA value");
    }
    
    @And("I enter valid username {string} and password {string}")
    public void iEnterValidUsernameAndPassword(String username, String password) {
        LOGGER.info("Entering valid username {} and password", username);
        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        Allure.step("Entered valid credentials");
    }
    
    @And("I tap on verify CAPTCHA button")
    public void iTapOnVerifyCaptchaButton() {
        LOGGER.info("Tapping on verify CAPTCHA button");
        dashboardPage = loginPage.verifyCaptchaAndLogin();
        Allure.step("Tapped on verify CAPTCHA button");
    }
}
