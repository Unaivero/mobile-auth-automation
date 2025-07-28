package com.securitytests.utils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Utility class for managing device farm integration (BrowserStack, AWS Device Farm, etc.)
 */
public class DeviceFarmManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceFarmManager.class);
    private static final String BROWSERSTACK_USERNAME_KEY = "browserstack.username";
    private static final String BROWSERSTACK_ACCESS_KEY = "browserstack.accessKey";
    private static final String BROWSERSTACK_URL = "https://hub-cloud.browserstack.com/wd/hub";
    
    // Config properties for device farm
    private final Properties properties;
    
    public DeviceFarmManager() {
        properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (Exception e) {
            LOGGER.error("Failed to load config properties", e);
        }
    }
    
    /**
     * Create an Appium driver configured for BrowserStack
     *
     * @param deviceName       target device name (e.g., "Samsung Galaxy S22")
     * @param osVersion        OS version (e.g., "12.0")
     * @param platformName     platform name (e.g., "Android" or "iOS")
     * @param appPath          app path or app_url for BrowserStack
     * @return                 configured AppiumDriver instance
     */
    public AppiumDriver createBrowserStackDriver(String deviceName, String osVersion, 
                                              String platformName, String appPath) {
        try {
            MutableCapabilities capabilities = new MutableCapabilities();
            HashMap<String, Object> browserstackOptions = new HashMap<>();
            
            // Set BrowserStack credentials from environment variables or properties
            String username = System.getenv("BROWSERSTACK_USERNAME");
            if (username == null) {
                username = properties.getProperty(BROWSERSTACK_USERNAME_KEY);
            }
            
            String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
            if (accessKey == null) {
                accessKey = properties.getProperty(BROWSERSTACK_ACCESS_KEY);
            }
            
            browserstackOptions.put("userName", username);
            browserstackOptions.put("accessKey", accessKey);
            
            // Set device capabilities
            browserstackOptions.put("deviceName", deviceName);
            browserstackOptions.put("platformVersion", osVersion);
            browserstackOptions.put("app", appPath);
            
            // Additional BrowserStack specific capabilities
            browserstackOptions.put("projectName", "Mobile Auth Security Tests");
            browserstackOptions.put("buildName", "Build " + System.currentTimeMillis());
            browserstackOptions.put("sessionName", "Auth Test Suite");
            browserstackOptions.put("local", "false");
            browserstackOptions.put("debug", "true");
            browserstackOptions.put("networkLogs", "true");
            
            // Set browser stack options in capabilities
            capabilities.setCapability("bstack:options", browserstackOptions);
            
            LOGGER.info("Creating BrowserStack driver for {} device: {}", platformName, deviceName);
            
            // Create the appropriate driver based on platform
            if ("android".equalsIgnoreCase(platformName)) {
                return new AndroidDriver(new URL(BROWSERSTACK_URL), capabilities);
            } else if ("ios".equalsIgnoreCase(platformName)) {
                return new IOSDriver(new URL(BROWSERSTACK_URL), capabilities);
            } else {
                throw new IllegalArgumentException("Unsupported platform: " + platformName);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to create BrowserStack driver", e);
            throw new RuntimeException("Failed to create BrowserStack driver", e);
        }
    }
    
    /**
     * Example method for AWS Device Farm integration
     * This is a placeholder that would need to be implemented according to AWS Device Farm requirements
     *
     * @param devicePoolArn    ARN of the device pool in AWS Device Farm
     * @param projectArn       ARN of the project in AWS Device Farm
     * @param appArn           ARN of the uploaded app in AWS Device Farm
     * @return                 capabilities for AWS Device Farm
     */
    public DesiredCapabilities createAWSDeviceFarmCapabilities(String devicePoolArn, String projectArn, String appArn) {
        // This is just a placeholder for AWS Device Farm integration
        // Real implementation would involve AWS Device Farm specific setup
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("devicePoolArn", devicePoolArn);
        capabilities.setCapability("projectArn", projectArn);
        capabilities.setCapability("appArn", appArn);
        LOGGER.info("Created AWS Device Farm capabilities (placeholder)");
        return capabilities;
    }
}
