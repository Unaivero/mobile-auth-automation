package com.securitytests.config;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Properties;

public class AppiumConfig {
    private static AppiumDriver driver;
    private static Properties properties;
    private static final String CONFIG_PATH = "src/main/resources/config.properties";

    static {
        try {
            properties = new Properties();
            FileInputStream fis = new FileInputStream(CONFIG_PATH);
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized AppiumDriver getDriver() {
        if (driver == null) {
            initDriver();
        }
        return driver;
    }

    private static void initDriver() {
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            String platformName = properties.getProperty("platformName");
            
            capabilities.setCapability("platformName", platformName);
            capabilities.setCapability("deviceName", properties.getProperty("deviceName"));
            capabilities.setCapability("automationName", properties.getProperty("automationName"));
            capabilities.setCapability("appPackage", properties.getProperty("appPackage"));
            capabilities.setCapability("appActivity", properties.getProperty("appActivity"));
            capabilities.setCapability("noReset", false);
            
            URL appiumServerURL = new URL(properties.getProperty("appiumServerUrl"));
            
            if ("Android".equalsIgnoreCase(platformName)) {
                driver = new AndroidDriver(appiumServerURL, capabilities);
            } else if ("iOS".equalsIgnoreCase(platformName)) {
                driver = new IOSDriver(appiumServerURL, capabilities);
            } else {
                throw new IllegalArgumentException("Unsupported platform: " + platformName);
            }
            
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize Appium driver", e);
        }
    }

    public static void quitDriver() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
