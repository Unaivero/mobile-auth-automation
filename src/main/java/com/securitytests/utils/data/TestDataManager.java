package com.securitytests.utils.data;

import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Step;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.ITestContext;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Manages test data creation, tracking, and cleanup
 */
public class TestDataManager {
    private static final StructuredLogger logger = new StructuredLogger(TestDataManager.class);
    private static final TestDataManager INSTANCE = new TestDataManager();
    private final String apiBaseUrl;
    private final String apiKey;
    private final List<TestData> createdTestData;
    private final Map<String, Object> testDataCache;
    private final Random random = new Random();
    private final SyntheticDataGenerator syntheticDataGenerator;

    /**
     * Private constructor for singleton pattern
     */
    private TestDataManager() {
        this.apiBaseUrl = System.getProperty("test.apiBaseUrl", "https://api.example.com");
        this.apiKey = System.getProperty("test.apiKey", "");
        this.createdTestData = new CopyOnWriteArrayList<>();
        this.testDataCache = new HashMap<>();
        this.syntheticDataGenerator = new SyntheticDataGenerator();
        
        logger.info("Test Data Manager initialized with API base URL: {}", apiBaseUrl);
        
        // Register shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupAllTestData));
    }
    
    /**
     * Get TestDataManager singleton instance
     */
    public static TestDataManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Create a user account for testing
     * 
     * @param userType Type of user to create
     * @return TestData object with user information
     */
    @Step("Create test user of type: {userType}")
    public TestData createTestUser(String userType) {
        logger.info("Creating test user of type: {}", userType);
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiBaseUrl + "/testdata/users");
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + apiKey);
            
            // Generate user data
            String username = syntheticDataGenerator.generateEmail("testuser");
            String password = syntheticDataGenerator.generateStrongPassword();
            
            JSONObject userData = new JSONObject();
            userData.put("username", username);
            userData.put("password", password);
            userData.put("userType", userType);
            userData.put("firstName", syntheticDataGenerator.generateFirstName());
            userData.put("lastName", syntheticDataGenerator.generateLastName());
            userData.put("phone", syntheticDataGenerator.generatePhoneNumber());
            
            request.setEntity(new StringEntity(userData.toString()));
            
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 201) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JSONObject createdUser = new JSONObject(responseBody);
                    
                    TestData testData = new TestData(
                            "user", 
                            createdUser.getString("id"), 
                            username, 
                            userType,
                            password);
                    
                    // Track created data for cleanup
                    createdTestData.add(testData);
                    logger.info("Created test user with ID: {}", testData.getId());
                    
                    return testData;
                } else {
                    logger.error("Failed to create test user. Status: {}", 
                            response.getStatusLine().getStatusCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error creating test user", e);
            return null;
        }
    }
    
    /**
     * Create a batch of test users
     * 
     * @param userType Type of user to create
     * @param count Number of users to create
     * @return List of created TestData objects
     */
    @Step("Create {count} test users of type: {userType}")
    public List<TestData> createTestUsers(String userType, int count) {
        logger.info("Creating {} test users of type: {}", count, userType);
        List<TestData> users = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TestData user = createTestUser(userType);
            if (user != null) {
                users.add(user);
            }
        }
        
        return users;
    }
    
    /**
     * Create authentication token for testing
     * 
     * @param username Username
     * @param password Password
     * @return Token string
     */
    @Step("Create auth token for user: {username}")
    public String createAuthToken(String username, String password) {
        logger.info("Creating auth token for user: {}", username);
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiBaseUrl + "/auth/login");
            request.setHeader("Content-Type", "application/json");
            
            JSONObject credentials = new JSONObject();
            credentials.put("username", username);
            credentials.put("password", password);
            
            request.setEntity(new StringEntity(credentials.toString()));
            
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JSONObject tokenResponse = new JSONObject(responseBody);
                    String token = tokenResponse.getString("token");
                    
                    // Cache the token for later use
                    testDataCache.put("token_" + username, token);
                    
                    logger.info("Created auth token for user: {}", username);
                    return token;
                } else {
                    logger.error("Failed to create auth token. Status: {}", 
                            response.getStatusLine().getStatusCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error creating auth token", e);
            return null;
        }
    }
    
    /**
     * Create test data through API
     * 
     * @param endpoint API endpoint
     * @param data JSON data to create
     * @param dataType Type of data being created
     * @return TestData object
     */
    @Step("Create test data of type {dataType}")
    public TestData createTestData(String endpoint, JSONObject data, String dataType) {
        logger.info("Creating test data of type: {} at endpoint: {}", dataType, endpoint);
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiBaseUrl + endpoint);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + apiKey);
            
            request.setEntity(new StringEntity(data.toString()));
            
            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getStatusLine().getStatusCode() == 201) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JSONObject createdData = new JSONObject(responseBody);
                    
                    String id = createdData.getString("id");
                    String name = createdData.has("name") ? createdData.getString("name") : id;
                    
                    TestData testData = new TestData(dataType, id, name, "", null);
                    
                    // Track created data for cleanup
                    createdTestData.add(testData);
                    logger.info("Created test data {} with ID: {}", dataType, id);
                    
                    return testData;
                } else {
                    logger.error("Failed to create test data. Status: {}", 
                            response.getStatusLine().getStatusCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error creating test data", e);
            return null;
        }
    }
    
    /**
     * Store arbitrary test data in the cache
     * 
     * @param key Cache key
     * @param value Data value
     */
    public void storeTestData(String key, Object value) {
        testDataCache.put(key, value);
        logger.info("Stored test data with key: {}", key);
    }
    
    /**
     * Retrieve test data from the cache
     * 
     * @param key Cache key
     * @return Data value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getTestData(String key) {
        if (testDataCache.containsKey(key)) {
            logger.info("Retrieved test data with key: {}", key);
            return (T) testDataCache.get(key);
        }
        logger.warn("Test data with key '{}' not found in cache", key);
        return null;
    }
    
    /**
     * Retrieve test data from the cache with a default value if not found
     * 
     * @param key Cache key
     * @param defaultValue Default value to return if key not found
     * @return Data value or default value if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getTestData(String key, T defaultValue) {
        if (testDataCache.containsKey(key)) {
            logger.info("Retrieved test data with key: {}", key);
            return (T) testDataCache.get(key);
        }
        logger.info("Test data with key '{}' not found, returning default value", key);
        return defaultValue;
    }
    
    /**
     * Retrieve test data from the cache, generating it if not found
     * 
     * @param key Cache key
     * @param dataSupplier Supplier to generate the data if not found
     * @return Data value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateTestData(String key, Supplier<T> dataSupplier) {
        if (testDataCache.containsKey(key)) {
            logger.info("Retrieved test data with key: {}", key);
            return (T) testDataCache.get(key);
        }
        
        logger.info("Test data with key '{}' not found, generating new data", key);
        T newData = dataSupplier.get();
        testDataCache.put(key, newData);
        return newData;
    }
    
    /**
     * Clean up a specific test data item
     * 
     * @param testData TestData object to clean up
     * @return true if cleanup was successful
     */
    @Step("Clean up test data: {testData.type}/{testData.id}")
    public boolean cleanupTestData(TestData testData) {
        logger.info("Cleaning up test data: {}/{}", testData.getType(), testData.getId());
        
        String endpoint = getEndpointForType(testData.getType()) + "/" + testData.getId();
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(apiBaseUrl + endpoint);
            request.setHeader("Authorization", "Bearer " + apiKey);
            
            HttpResponse response = client.execute(request);
            boolean success = response.getStatusLine().getStatusCode() == 200 || 
                              response.getStatusLine().getStatusCode() == 204;
            
            if (success) {
                logger.info("Successfully cleaned up test data: {}/{}", 
                        testData.getType(), testData.getId());
                createdTestData.remove(testData);
            } else {
                logger.warn("Failed to clean up test data: {}/{}. Status: {}", 
                        testData.getType(), testData.getId(), response.getStatusLine().getStatusCode());
            }
            
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                EntityUtils.consume(entity);
            }
            
            return success;
        } catch (IOException e) {
            logger.error("Error cleaning up test data: {}/{}", testData.getType(), testData.getId(), e);
            return false;
        }
    }
    
    /**
     * Clean up all test data created in this test context
     */
    @Step("Clean up all test data")
    public void cleanupAllTestData() {
        logger.info("Cleaning up all created test data");
        
        // Create a copy to avoid concurrent modification
        List<TestData> dataToCleanup = new ArrayList<>(createdTestData);
        
        // Reverse to delete in reverse order of creation (dependencies)
        Collections.reverse(dataToCleanup);
        
        for (TestData testData : dataToCleanup) {
            cleanupTestData(testData);
        }
        
        logger.info("Test data cleanup completed");
    }
    
    /**
     * Clean up test data for a specific test context
     * 
     * @param context TestNG test context
     */
    public void cleanupTestData(ITestContext context) {
        String testName = context.getName();
        logger.info("Cleaning up test data for test: {}", testName);
        
        // Filter test data for this test
        List<TestData> dataToCleanup = createdTestData.stream()
                .filter(data -> data.getTestName() != null && data.getTestName().equals(testName))
                .toList();
        
        for (TestData testData : dataToCleanup) {
            cleanupTestData(testData);
        }
        
        logger.info("Test data cleanup completed for test: {}", testName);
    }
    
    /**
     * Get the API endpoint for a given data type
     * 
     * @param dataType Type of data
     * @return API endpoint
     */
    private String getEndpointForType(String dataType) {
        switch (dataType) {
            case "user":
                return "/testdata/users";
            case "device":
                return "/testdata/devices";
            case "session":
                return "/testdata/sessions";
            default:
                return "/testdata/" + dataType;
        }
    }
    
    /**
     * Get the synthetic data generator
     */
    public SyntheticDataGenerator getSyntheticDataGenerator() {
        return syntheticDataGenerator;
    }
    
    /**
     * Class representing test data
     */
    public static class TestData {
        private final String type;
        private final String id;
        private final String name;
        private final String testName;
        private final Object metadata;
        private final Date creationTime;
        
        public TestData(String type, String id, String name, String testName, Object metadata) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.testName = testName;
            this.metadata = metadata;
            this.creationTime = new Date();
        }
        
        public String getType() {
            return type;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getTestName() {
            return testName;
        }
        
        public Object getMetadata() {
            return metadata;
        }
        
        public Date getCreationTime() {
            return creationTime;
        }
        
        @Override
        public String toString() {
            return String.format("%s [%s] %s", type, id, name);
        }
    }
}
