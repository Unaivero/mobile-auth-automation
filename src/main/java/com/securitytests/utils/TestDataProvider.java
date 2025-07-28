package com.securitytests.utils;

import org.testng.annotations.DataProvider;

/**
 * Data provider class for test data
 */
public class TestDataProvider {
    
    /**
     * Provides invalid login credentials
     * @return Object array of username and password combinations
     */
    @DataProvider(name = "invalidLoginCredentials")
    public static Object[][] invalidLoginCredentials() {
        return new Object[][] {
            {"invalid_user", "wrongPassword123", "Invalid username or password"},
            {"test@example.com", "password123!", "Invalid username or password"},
            {"admin", "admin", "Invalid username or password"}
        };
    }
    
    /**
     * Provides valid login credentials
     * @return Object array of username and password combinations
     */
    @DataProvider(name = "validLoginCredentials")
    public static Object[][] validLoginCredentials() {
        return new Object[][] {
            {"validuser@example.com", "Valid@Password123"}
        };
    }
    
    /**
     * Provides credentials for testing password recovery
     * @return Object array with email, new password and confirmation password
     */
    @DataProvider(name = "passwordRecoveryData")
    public static Object[][] passwordRecoveryData() {
        return new Object[][] {
            // Using a dynamic email (will be replaced during test execution)
            {"EMAIL_PLACEHOLDER", "NewSecurePassword123!"}
        };
    }
    
    /**
     * Provides invalid token data for testing token validation
     * @return Object array with invalid tokens and expected error messages
     */
    @DataProvider(name = "invalidTokenData")
    public static Object[][] invalidTokenData() {
        return new Object[][] {
            {"000000", "Invalid or expired token"},
            {"123", "Token must be 6 digits"},
            {"abcdef", "Token must contain only digits"}
        };
    }
}
