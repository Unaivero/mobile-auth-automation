package com.securitytests.pages;

import com.securitytests.config.AppiumConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base class for all page objects with common interactions
 */
public abstract class BasePage {
    // Changed from protected to public to allow access from VisualValidator
    public AppiumDriver driver;
    protected WebDriverWait wait;

    public BasePage() {
        this.driver = AppiumConfig.getDriver();
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(
                Integer.parseInt(AppiumConfig.getProperty("explicit.wait.seconds"))));
    }

    @Step("Waiting for element to be visible: {0}")
    protected WebElement waitForElement(WebElement element) {
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    @Step("Waiting for element to be clickable: {0}")
    protected WebElement waitForElementToBeClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }
    
    @Step("Waiting for element by locator: {0}")
    protected WebElement waitForElement(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    @Step("Clicking on element: {0}")
    protected void click(WebElement element) {
        waitForElementToBeClickable(element).click();
    }

    @Step("Entering text: {1} into field: {0}")
    protected void enterText(WebElement element, String text) {
        waitForElement(element).clear();
        element.sendKeys(text);
    }

    @Step("Checking if element is displayed: {0}")
    protected boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Step("Checking if element is displayed with timeout: {0}")
    protected boolean isElementDisplayed(By locator, int timeoutInSeconds) {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
            shortWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Getting text from element: {0}")
    protected String getText(WebElement element) {
        return waitForElement(element).getText();
    }
    
    @Step("Taking screenshot")
    public byte[] takeScreenshot() {
        return driver.getScreenshotAs(org.openqa.selenium.OutputType.BYTES);
    }
}
