package com.securitytests.tests;

import com.securitytests.config.AppiumConfig;
import com.securitytests.utils.MockServerUtil;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for all test classes with common setup and teardown methods
 */
public abstract class BaseTest {
    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);
    protected AppiumDriver driver;
    
    @BeforeClass
    public void classSetup() {
        LOGGER.info("Setting up test class: {}", getClass().getSimpleName());
        
        // Start mock server for API simulations
        MockServerUtil.startServer();
    }
    
    @AfterClass
    public void classTeardown() {
        LOGGER.info("Tearing down test class: {}", getClass().getSimpleName());
        
        // Stop mock server
        MockServerUtil.stopServer();
    }
    
    @BeforeMethod
    @Step("Setting up test")
    public void methodSetup() {
        LOGGER.info("Setting up test method");
        
        // Initialize driver for each test
        driver = AppiumConfig.getDriver();
    }
    
    @AfterMethod
    @Step("Tearing down test")
    public void methodTeardown() {
        LOGGER.info("Tearing down test method");
        
        // Quit driver after each test
        AppiumConfig.quitDriver();
    }
}
