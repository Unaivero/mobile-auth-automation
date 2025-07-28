package com.securitytests.runners;

import com.securitytests.utils.config.SecurityTestConfig;
import com.securitytests.utils.logging.StructuredLogger;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.qameta.allure.Allure;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;

import java.util.HashMap;
import java.util.Map;

/**
 * TestNG runner for BDD security tests using Cucumber
 * This runner is specifically designed for security tests and includes
 * special configuration handling and reporting for security test results
 */
@CucumberOptions(
    features = "src/test/resources/features/security",
    glue = {"com.securitytests.steps"},
    plugin = {
        "io.qameta.allure.cucumber6jvm.AllureCucumber6Jvm",
        "json:target/cucumber-reports/security-tests-report.json",
        "html:target/cucumber-reports/security-tests-report.html",
        "pretty"
    },
    tags = "@security"
)
public class BDDSecurityTestRunner extends AbstractTestNGCucumberTests {
    
    private static final StructuredLogger logger = new StructuredLogger(BDDSecurityTestRunner.class);
    private SecurityTestConfig securityConfig;
    private long testStartTime;
    
    @BeforeTest
    @Parameters({"testEnvironment"})
    public void setupTestEnvironment(ITestContext context, String testEnvironment) {
        logger.info("Setting up security test environment: {}", testEnvironment);
        
        // Load environment-specific properties based on the testEnvironment parameter
        System.setProperty("TEST_ENVIRONMENT", testEnvironment);
        
        // Initialize security configuration
        securityConfig = SecurityTestConfig.getInstance();
        securityConfig.initialize();
        
        // Add security configuration to Allure report
        Map<String, Object> configSummary = securityConfig.getConfigurationSummary();
        Allure.addAttachment("Security Test Configuration", 
                "application/json", 
                formatConfigMapAsJson(configSummary));
        
        logger.info("Security configuration loaded for environment: {}", testEnvironment);
        
        // Set dynamic test parameters from configuration
        context.setAttribute("securityTestLevel", securityConfig.getOwaspLevel());
        context.setAttribute("zapEnabled", securityConfig.isZapEnabled());
        context.setAttribute("apiBaseUrl", securityConfig.getApiBaseUrl());
        
        // Register timestamp for test run duration tracking
        testStartTime = System.currentTimeMillis();
    }
    
    @BeforeClass(alwaysRun = true)
    public void setupSecurityTestClass() {
        logger.info("Setting up security test class execution");
        
        // Verify all required configuration is present
        securityConfig = SecurityTestConfig.getInstance();
        boolean configValid = securityConfig.validateConfiguration();
        
        if (!configValid) {
            logger.error("Security test configuration validation failed");
            Allure.addAttachment("Configuration Error", "text/plain", 
                    "Security test configuration validation failed. Check logs for details.");
            throw new IllegalStateException("Invalid security test configuration");
        }
        
        // Setup any additional test requirements
        setupZapProxyIfEnabled();
        
        logger.info("Security test class setup completed successfully");
    }
    
    @AfterClass(alwaysRun = true)
    public void tearDownSecurityTestClass() {
        logger.info("Tearing down security test class");
        
        // Calculate and report test run duration
        long testDuration = System.currentTimeMillis() - testStartTime;
        logger.info("Security test execution completed in {} ms", testDuration);
        
        // Clean up any resources
        cleanupZapProxyIfEnabled();
        
        // Add security test summary to Allure report
        Map<String, Object> testSummary = new HashMap<>();
        testSummary.put("testDuration", testDuration);
        testSummary.put("securityTestLevel", securityConfig.getOwaspLevel());
        testSummary.put("zapEnabled", securityConfig.isZapEnabled());
        
        Allure.addAttachment("Security Test Summary", 
                "application/json", 
                formatConfigMapAsJson(testSummary));
        
        logger.info("Security test class teardown completed");
    }
    
    private void setupZapProxyIfEnabled() {
        if (securityConfig.isZapEnabled()) {
            String zapProxyUrl = securityConfig.getZapProxyUrl();
            String zapApiKey = securityConfig.getZapApiKey();
            
            logger.info("Setting up ZAP proxy at {}", zapProxyUrl);
            
            // Set proxy system properties for HTTP client to use ZAP proxy
            System.setProperty("http.proxyHost", extractHost(zapProxyUrl));
            System.setProperty("http.proxyPort", extractPort(zapProxyUrl));
            System.setProperty("https.proxyHost", extractHost(zapProxyUrl));
            System.setProperty("https.proxyPort", extractPort(zapProxyUrl));
            
            // Additional ZAP setup can be added here
            
            logger.info("ZAP proxy setup completed");
            Allure.addAttachment("ZAP Proxy Setup", "text/plain", 
                    "ZAP proxy enabled and configured at: " + zapProxyUrl);
        } else {
            logger.info("ZAP proxy integration disabled");
        }
    }
    
    private void cleanupZapProxyIfEnabled() {
        if (securityConfig.isZapEnabled()) {
            logger.info("Cleaning up ZAP proxy configuration");
            
            // Clear proxy system properties
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
            
            // Additional ZAP cleanup can be added here
            
            logger.info("ZAP proxy cleanup completed");
        }
    }
    
    private String extractHost(String proxyUrl) {
        // Simple extraction of host from http://host:port
        if (proxyUrl == null || proxyUrl.isEmpty()) {
            return "localhost";
        }
        
        String host = proxyUrl.replaceFirst("http://|https://", "");
        int colonIndex = host.indexOf(":");
        if (colonIndex > 0) {
            host = host.substring(0, colonIndex);
        }
        return host;
    }
    
    private String extractPort(String proxyUrl) {
        // Simple extraction of port from http://host:port
        if (proxyUrl == null || proxyUrl.isEmpty()) {
            return "8080";
        }
        
        int colonIndex = proxyUrl.indexOf(":");
        int slashIndex = proxyUrl.indexOf("/", colonIndex + 3);
        if (slashIndex < 0) {
            slashIndex = proxyUrl.length();
        }
        
        if (colonIndex > 0 && colonIndex < slashIndex) {
            return proxyUrl.substring(colonIndex + 1, slashIndex);
        }
        
        return "8080"; // Default port
    }
    
    private String formatConfigMapAsJson(Map<String, Object> map) {
        // Simple JSON formatting for the config map
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) {
                json.append(",\n");
            }
            
            json.append("  \"").append(entry.getKey()).append("\": ");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            
            i++;
        }
        
        json.append("\n}");
        return json.toString();
    }
}
