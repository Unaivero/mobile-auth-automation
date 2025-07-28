package com.securitytests.utils.data;

import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central utility for generating test data using JavaFaker
 */
public class DataGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataGenerator.class);
    private static final Faker faker = new Faker(Locale.US);
    
    // Thread-local storage for test data to ensure isolation between parallel tests
    private static final ThreadLocal<Map<String, Object>> threadLocalData = ThreadLocal.withInitial(ConcurrentHashMap::new);
    
    /**
     * Get a unique username for the current test thread
     * @return A unique username
     */
    public static String getUniqueUsername() {
        String username = faker.name().username() + UUID.randomUUID().toString().substring(0, 5);
        storeThreadData("username", username);
        return username;
    }
    
    /**
     * Get a strong password for the current test thread
     * @return A strong password
     */
    public static String getStrongPassword() {
        String password = faker.internet().password(10, 15, true, true, true);
        storeThreadData("password", password);
        return password;
    }
    
    /**
     * Get a valid email address for the current test thread
     * @return A valid email address
     */
    public static String getEmail() {
        String email = faker.internet().emailAddress();
        storeThreadData("email", email);
        return email;
    }
    
    /**
     * Get a name for the current test thread
     * @return A full name
     */
    public static String getName() {
        return faker.name().fullName();
    }
    
    /**
     * Get an invalid email address for negative testing
     * @return An invalid email address
     */
    public static String getInvalidEmail() {
        String[] invalidEmails = {
            "plainaddress",
            "@missingusername.com",
            "username@.com",
            "username@domain..com"
        };
        return invalidEmails[faker.random().nextInt(invalidEmails.length)];
    }
    
    /**
     * Get an invalid password for negative testing
     * @return An invalid password
     */
    public static String getInvalidPassword() {
        String[] invalidPasswords = {
            "short",
            "no_uppercase",
            "NO_LOWERCASE",
            "NoSpecialChars123"
        };
        return invalidPasswords[faker.random().nextInt(invalidPasswords.length)];
    }
    
    /**
     * Get a fake captcha code
     * @return A captcha code
     */
    public static String getCaptchaCode() {
        return faker.number().digits(6);
    }
    
    /**
     * Get a fake reset token
     * @return A reset token
     */
    public static String getResetToken() {
        return faker.crypto().md5().substring(0, 10);
    }
    
    /**
     * Store data in the thread-local storage for the current test
     * @param key The key for the data
     * @param value The value to store
     */
    public static void storeThreadData(String key, Object value) {
        threadLocalData.get().put(key, value);
        LOGGER.info("Stored thread-local data: {} = {}", key, value);
    }
    
    /**
     * Get stored data from the thread-local storage
     * @param key The key for the data
     * @return The stored value, or null if not found
     */
    public static Object getThreadData(String key) {
        return threadLocalData.get().get(key);
    }
    
    /**
     * Get a map of all thread-local data
     * @return Map of all thread-local data
     */
    public static Map<String, Object> getAllThreadData() {
        return new HashMap<>(threadLocalData.get());
    }
    
    /**
     * Clear all thread-local data
     */
    public static void clearThreadData() {
        threadLocalData.get().clear();
        LOGGER.info("Cleared thread-local test data");
    }
}
