package com.securitytests.utils.config;

import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Allure;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration manager for security tests
 * Handles configuration from properties files and environment variables
 */
public class SecurityTestConfig {
    private static final StructuredLogger logger = new StructuredLogger(SecurityTestConfig.class);
    private static final SecurityTestConfig INSTANCE = new SecurityTestConfig();
    
    private final Properties properties = new Properties();
    private final Map<String, Object> configCache = new HashMap<>();
    private boolean initialized = false;
    
    /**
     * Private constructor for singleton pattern
     */
    private SecurityTestConfig() {
        // Private constructor
    }
    
    /**
     * Get instance of SecurityTestConfig
     * 
     * @return SecurityTestConfig instance
     */
    public static SecurityTestConfig getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize configuration from properties files and environment variables
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        logger.info("Initializing security test configuration");
        
        // Load properties in order of precedence (lowest to highest)
        loadPropertiesFile("security-test.properties");
        loadPropertiesFile("security-test-" + getEnvironment() + ".properties");
        
        // Override with system properties
        properties.putAll(System.getProperties());
        
        initialized = true;
        logger.info("Security test configuration initialized with {} properties", properties.size());
        
        // Add configuration summary to Allure report
        Allure.addAttachment("Security Test Configuration", "text/plain", getConfigSummary());
    }
    
    /**
     * Load properties file if it exists
     * 
     * @param filename Properties file name
     */
    private void loadPropertiesFile(String filename) {
        // Try to load from classpath first
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded configuration from classpath: {}", filename);
                return;
            }
        } catch (IOException e) {
            logger.warn("Error loading configuration from classpath: {}", filename, e);
        }
        
        // Then try to load from file system
        Path path = Paths.get(filename);
        if (Files.exists(path)) {
            try (InputStream input = new FileInputStream(path.toFile())) {
                properties.load(input);
                logger.info("Loaded configuration from file: {}", path.toAbsolutePath());
            } catch (IOException e) {
                logger.warn("Error loading configuration from file: {}", path.toAbsolutePath(), e);
            }
        } else {
            logger.info("Configuration file not found: {}", filename);
        }
    }
    
    /**
     * Get current environment (dev, test, staging, prod)
     * 
     * @return Current environment
     */
    public String getEnvironment() {
        String env = System.getProperty("test.environment");
        if (env == null) {
            env = System.getenv("TEST_ENVIRONMENT");
        }
        return env != null ? env : "dev";
    }
    
    /**
     * Get configuration value as string
     * 
     * @param key Configuration key
     * @return Configuration value or null if not found
     */
    public String getString(String key) {
        return getString(key, null);
    }
    
    /**
     * Get configuration value as string with default
     * 
     * @param key Configuration key
     * @param defaultValue Default value if key not found
     * @return Configuration value or default if not found
     */
    public String getString(String key, String defaultValue) {
        ensureInitialized();
        
        // First check cache
        if (configCache.containsKey(key)) {
            Object value = configCache.get(key);
            return value != null ? value.toString() : defaultValue;
        }
        
        // Then check properties
        String value = properties.getProperty(key);
        
        // Then check environment variables
        if (value == null) {
            String envKey = key.replace('.', '_').toUpperCase();
            value = System.getenv(envKey);
        }
        
        // Cache the value
        configCache.put(key, value);
        
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get configuration value as integer
     * 
     * @param key Configuration key
     * @param defaultValue Default value if key not found or not an integer
     * @return Configuration value or default if not found or not an integer
     */
    public int getInt(String key, int defaultValue) {
        ensureInitialized();
        
        // Check cache first
        if (configCache.containsKey(key) && configCache.get(key) instanceof Integer) {
            return (Integer) configCache.get(key);
        }
        
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            int intValue = Integer.parseInt(value);
            configCache.put(key, intValue);
            return intValue;
        } catch (NumberFormatException e) {
            logger.warn("Configuration value for '{}' is not a valid integer: {}", key, value);
            return defaultValue;
        }
    }
    
    /**
     * Get configuration value as boolean
     * 
     * @param key Configuration key
     * @param defaultValue Default value if key not found
     * @return Configuration value or default if not found
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        ensureInitialized();
        
        // Check cache first
        if (configCache.containsKey(key) && configCache.get(key) instanceof Boolean) {
            return (Boolean) configCache.get(key);
        }
        
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        
        boolean boolValue = "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
        configCache.put(key, boolValue);
        return boolValue;
    }
    
    /**
     * Get API base URL
     * 
     * @return API base URL
     */
    public String getApiBaseUrl() {
        return getString("api.baseUrl", "https://api.example.com");
    }
    
    /**
     * Get API timeout in milliseconds
     * 
     * @return API timeout
     */
    public int getApiTimeoutMs() {
        return getInt("api.timeout", 30000);
    }
    
    /**
     * Get ZAP proxy URL
     * 
     * @return ZAP proxy URL
     */
    public String getZapProxyUrl() {
        return getString("zap.proxyUrl", "http://localhost:8080");
    }
    
    /**
     * Get ZAP API key
     * 
     * @return ZAP API key
     */
    public String getZapApiKey() {
        return getString("zap.apiKey", "");
    }
    
    /**
     * Check if ZAP integration is enabled
     * 
     * @return true if ZAP integration is enabled
     */
    public boolean isZapEnabled() {
        return getBoolean("zap.enabled", false);
    }
    
    /**
     * Get OWASP rules level to enforce
     * 
     * @return OWASP rules level (1-3)
     */
    public int getOwaspRulesLevel() {
        return getInt("security.owaspLevel", 2);
    }
    
    /**
     * Get JWT secret for testing
     * 
     * @return JWT secret
     */
    public String getJwtSecret() {
        return getString("security.jwtSecret", "");
    }
    
    /**
     * Get session timeout in minutes
     * 
     * @return Session timeout
     */
    public int getSessionTimeoutMinutes() {
        return getInt("security.sessionTimeout", 30);
    }
    
    /**
     * Check if security scanning is enabled
     * 
     * @return true if security scanning is enabled
     */
    public boolean isSecurityScanEnabled() {
        return getBoolean("security.scanEnabled", true);
    }
    
    /**
     * Get account lockout threshold
     * 
     * @return Account lockout threshold
     */
    public int getAccountLockoutThreshold() {
        return getInt("security.accountLockoutThreshold", 5);
    }
    
    /**
     * Get the maximum allowed password age in days
     * 
     * @return Maximum password age
     */
    public int getMaxPasswordAgeDays() {
        return getInt("security.maxPasswordAgeDays", 90);
    }
    
    /**
     * Get list of required security headers
     * 
     * @return Array of required security headers
     */
    public String[] getRequiredSecurityHeaders() {
        String headers = getString("security.requiredHeaders", 
                "Strict-Transport-Security,X-Content-Type-Options,X-XSS-Protection,Content-Security-Policy");
        return headers.split(",");
    }
    
    /**
     * Get a summary of the configuration
     * 
     * @return Configuration summary
     */
    public String getConfigSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Security Test Configuration Summary:\n");
        summary.append("-------------------------------------\n");
        summary.append(String.format("Environment: %s\n", getEnvironment()));
        summary.append(String.format("API Base URL: %s\n", getApiBaseUrl()));
        summary.append(String.format("API Timeout: %d ms\n", getApiTimeoutMs()));
        summary.append(String.format("ZAP Integration Enabled: %b\n", isZapEnabled()));
        summary.append(String.format("Security Scan Enabled: %b\n", isSecurityScanEnabled()));
        summary.append(String.format("OWASP Rules Level: %d\n", getOwaspRulesLevel()));
        summary.append(String.format("Session Timeout: %d minutes\n", getSessionTimeoutMinutes()));
        summary.append(String.format("Account Lockout Threshold: %d attempts\n", getAccountLockoutThreshold()));
        summary.append(String.format("Max Password Age: %d days\n", getMaxPasswordAgeDays()));
        summary.append("Required Security Headers: ");
        for (String header : getRequiredSecurityHeaders()) {
            summary.append(header).append(", ");
        }
        if (summary.toString().endsWith(", ")) {
            summary.setLength(summary.length() - 2);
        }
        summary.append("\n");
        
        return summary.toString();
    }
    
    /**
     * Ensure the configuration is initialized
     */
    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }
    
    /**
     * Set configuration value (for testing purposes)
     * 
     * @param key Configuration key
     * @param value Configuration value
     */
    public void setConfigValue(String key, Object value) {
        ensureInitialized();
        configCache.put(key, value);
        
        if (value != null) {
            properties.setProperty(key, value.toString());
        } else {
            properties.remove(key);
        }
    }
}
