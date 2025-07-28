package com.securitytests.utils;

import com.securitytests.config.AppiumConfig;
import com.securitytests.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TestNG listener to handle test events, take screenshots on failures
 * and manage driver lifecycle
 */
public class TestListener implements ITestListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TestListener.class);
    
    @Override
    public void onStart(ITestContext context) {
        LOGGER.info("Starting test suite: {}", context.getName());
    }
    
    @Override
    public void onFinish(ITestContext context) {
        LOGGER.info("Finished test suite: {}", context.getName());
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        LOGGER.info("Starting test: {}", result.getName());
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        LOGGER.info("Test passed: {}", result.getName());
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.error("Test failed: {}", result.getName());
        LOGGER.error("Failure cause: {}", result.getThrowable().getMessage());
        
        // Take screenshot on test failure
        takeScreenshot(result.getMethod().getMethodName());
        
        // If a page object is available in test class, take screenshot using it
        try {
            Object testInstance = result.getInstance();
            if (testInstance instanceof BasePage) {
                attachScreenshot(((BasePage) testInstance).takeScreenshot());
            } else {
                AppiumDriver driver = AppiumConfig.getDriver();
                if (driver != null) {
                    attachScreenshot(driver.getScreenshotAs(OutputType.BYTES));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to take screenshot", e);
        }
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        LOGGER.info("Test skipped: {}", result.getName());
    }
    
    @Attachment(value = "Screenshot on failure", type = "image/png")
    private byte[] takeScreenshot(String methodName) {
        try {
            AppiumDriver driver = AppiumConfig.getDriver();
            if (driver != null) {
                return driver.getScreenshotAs(OutputType.BYTES);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to take screenshot for {}", methodName, e);
        }
        return new byte[0];
    }
    
    @Attachment(value = "Screenshot", type = "image/png")
    private byte[] attachScreenshot(byte[] screenshot) {
        return screenshot;
    }
}
