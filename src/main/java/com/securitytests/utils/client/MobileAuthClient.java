package com.securitytests.utils.client;

import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.security.JWTSecurityAnalyzer;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Client for mobile authentication API interactions
 * Provides methods for login, registration, token validation, and session management
 */
public class MobileAuthClient {
    private static final StructuredLogger logger = new StructuredLogger(MobileAuthClient.class);
    
    private final String baseUrl;
    private final int connectionTimeout;
    private final int socketTimeout;
    private String authToken;
    private String refreshToken;
    private final Map<String, String> defaultHeaders;
    private final JWTSecurityAnalyzer jwtAnalyzer;
    
    /**
     * Create a new mobile authentication client
     * 
     * @param baseUrl Base API URL
     */
    public MobileAuthClient(String baseUrl) {
        this(baseUrl, 30000, 30000);
    }
    
    /**
     * Create a new mobile authentication client with custom timeouts
     * 
     * @param baseUrl Base API URL
     * @param connectionTimeout Connection timeout in ms
     * @param socketTimeout Socket timeout in ms
     */
    public MobileAuthClient(String baseUrl, int connectionTimeout, int socketTimeout) {
        this.baseUrl = baseUrl;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.defaultHeaders = new HashMap<>();
        this.jwtAnalyzer = new JWTSecurityAnalyzer();
        
        // Set default headers
        defaultHeaders.put("Content-Type", "application/json");
        defaultHeaders.put("Accept", "application/json");
        defaultHeaders.put("User-Agent", "MobileAuthClient/1.0");
        
        logger.info("Mobile Auth Client initialized for base URL: {}", baseUrl);
    }
    
    /**
     * Login with username and password
     * 
     * @param username Username or email
     * @param password Password
     * @return API response with login result
     */
    @Step("Login with username: {username}")
    public ApiResponse login(String username, String password) {
        logger.info("Attempting login for user: {}", username);
        
        try {
            JSONObject credentials = new JSONObject();
            credentials.put("username", username);
            credentials.put("password", password);
            
            ApiResponse response = post("/auth/login", credentials.toString());
            
            if (response.isSuccessful()) {
                JSONObject data = response.getJsonBody();
                if (data.has("token")) {
                    authToken = data.getString("token");
                    logger.info("Successfully logged in and obtained auth token");
                    
                    if (data.has("refreshToken")) {
                        refreshToken = data.getString("refreshToken");
                    }
                    
                    // Analyze the JWT token
                    Map<String, Object> tokenAnalysis = jwtAnalyzer.analyzeToken(authToken);
                    
                    // Add JWT analysis as attachment to Allure report
                    Allure.addAttachment("JWT Analysis", "application/json", 
                            new JSONObject(tokenAnalysis).toString(2), ".json");
                } else {
                    logger.warn("Login response did not contain authentication token");
                }
            } else {
                logger.warn("Login failed with status: {}", response.getStatusCode());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error during login", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Register a new user
     * 
     * @param email Email address
     * @param password Password
     * @param firstName First name
     * @param lastName Last name
     * @param additionalFields Additional registration fields
     * @return API response with registration result
     */
    @Step("Register new user: {email}")
    public ApiResponse register(String email, String password, String firstName, 
                               String lastName, Map<String, Object> additionalFields) {
        logger.info("Registering new user with email: {}", email);
        
        try {
            JSONObject userData = new JSONObject();
            userData.put("email", email);
            userData.put("password", password);
            userData.put("firstName", firstName);
            userData.put("lastName", lastName);
            
            // Add any additional fields
            if (additionalFields != null) {
                for (Map.Entry<String, Object> entry : additionalFields.entrySet()) {
                    userData.put(entry.getKey(), entry.getValue());
                }
            }
            
            ApiResponse response = post("/auth/register", userData.toString());
            
            if (response.isSuccessful()) {
                logger.info("Successfully registered user: {}", email);
            } else {
                logger.warn("Registration failed with status: {}", response.getStatusCode());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error during registration", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Verify email address
     * 
     * @param email Email address
     * @param verificationToken Verification token
     * @return API response with verification result
     */
    @Step("Verify email: {email}")
    public ApiResponse verifyEmail(String email, String verificationToken) {
        logger.info("Verifying email: {}", email);
        
        try {
            JSONObject verificationData = new JSONObject();
            verificationData.put("email", email);
            verificationData.put("token", verificationToken);
            
            ApiResponse response = post("/auth/verify-email", verificationData.toString());
            
            if (response.isSuccessful()) {
                logger.info("Successfully verified email: {}", email);
            } else {
                logger.warn("Email verification failed with status: {}", response.getStatusCode());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error during email verification", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Refresh authentication token
     * 
     * @return API response with new token
     */
    @Step("Refresh authentication token")
    public ApiResponse refreshToken() {
        if (refreshToken == null) {
            logger.error("Cannot refresh token: No refresh token available");
            return ApiResponse.error(new IllegalStateException("No refresh token available"));
        }
        
        logger.info("Refreshing authentication token");
        
        try {
            JSONObject refreshData = new JSONObject();
            refreshData.put("refreshToken", refreshToken);
            
            ApiResponse response = post("/auth/refresh", refreshData.toString());
            
            if (response.isSuccessful()) {
                JSONObject data = response.getJsonBody();
                if (data.has("token")) {
                    authToken = data.getString("token");
                    logger.info("Successfully refreshed authentication token");
                    
                    if (data.has("refreshToken")) {
                        refreshToken = data.getString("refreshToken");
                    }
                } else {
                    logger.warn("Refresh response did not contain new authentication token");
                }
            } else {
                logger.warn("Token refresh failed with status: {}", response.getStatusCode());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Logout from the current session
     * 
     * @return API response with logout result
     */
    @Step("Logout")
    public ApiResponse logout() {
        if (authToken == null) {
            logger.warn("Cannot logout: No active session");
            return ApiResponse.error(new IllegalStateException("No active session"));
        }
        
        logger.info("Logging out from current session");
        
        try {
            ApiResponse response = post("/auth/logout", "{}");
            
            if (response.isSuccessful()) {
                logger.info("Successfully logged out");
                authToken = null;
                refreshToken = null;
            } else {
                logger.warn("Logout failed with status: {}", response.getStatusCode());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("Error during logout", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Get user profile for the current authenticated user
     * 
     * @return API response with user profile data
     */
    @Step("Get user profile")
    public ApiResponse getUserProfile() {
        logger.info("Retrieving user profile");
        return get("/user/profile");
    }
    
    /**
     * Change user password
     * 
     * @param currentPassword Current password
     * @param newPassword New password
     * @return API response with password change result
     */
    @Step("Change password")
    public ApiResponse changePassword(String currentPassword, String newPassword) {
        logger.info("Changing user password");
        
        try {
            JSONObject passwordData = new JSONObject();
            passwordData.put("currentPassword", currentPassword);
            passwordData.put("newPassword", newPassword);
            
            return post("/user/change-password", passwordData.toString());
        } catch (Exception e) {
            logger.error("Error changing password", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Request password reset
     * 
     * @param email Email address
     * @return API response with reset request result
     */
    @Step("Request password reset for email: {email}")
    public ApiResponse requestPasswordReset(String email) {
        logger.info("Requesting password reset for email: {}", email);
        
        try {
            JSONObject resetData = new JSONObject();
            resetData.put("email", email);
            
            return post("/auth/reset-password-request", resetData.toString());
        } catch (Exception e) {
            logger.error("Error requesting password reset", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Validate JWT token
     * 
     * @param token JWT token to validate
     * @return Validation results
     */
    @Step("Validate JWT token")
    public Map<String, Object> validateToken(String token) {
        logger.info("Validating JWT token");
        return jwtAnalyzer.analyzeToken(token);
    }
    
    /**
     * Get security headers from an endpoint
     * 
     * @param endpoint Endpoint to check
     * @return Map of security headers
     */
    @Step("Get security headers from endpoint: {endpoint}")
    public Map<String, String> getSecurityHeaders(String endpoint) {
        logger.info("Getting security headers from endpoint: {}", endpoint);
        
        try {
            HttpGet request = createHttpGet(endpoint);
            try (CloseableHttpClient client = createHttpClient()) {
                HttpResponse response = client.execute(request);
                
                Map<String, String> headers = new HashMap<>();
                for (org.apache.http.Header header : response.getAllHeaders()) {
                    headers.put(header.getName(), header.getValue());
                }
                
                EntityUtils.consume(response.getEntity());
                return headers;
            }
        } catch (Exception e) {
            logger.error("Error getting security headers", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Perform GET request
     * 
     * @param endpoint API endpoint
     * @return API response
     */
    public ApiResponse get(String endpoint) {
        return get(endpoint, null);
    }
    
    /**
     * Perform GET request with query parameters
     * 
     * @param endpoint API endpoint
     * @param queryParams Query parameters
     * @return API response
     */
    public ApiResponse get(String endpoint, Map<String, String> queryParams) {
        logger.info("Making GET request to: {}", endpoint);
        
        try {
            HttpGet request = createHttpGet(endpoint);
            
            // Apply authentication token if available
            applyAuthToken(request);
            
            // Apply query parameters if provided
            if (queryParams != null && !queryParams.isEmpty()) {
                StringBuilder urlBuilder = new StringBuilder(baseUrl + endpoint);
                urlBuilder.append("?");
                
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    urlBuilder.append(entry.getKey())
                             .append("=")
                             .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                             .append("&");
                }
                
                // Remove trailing &
                String url = urlBuilder.substring(0, urlBuilder.length() - 1);
                request.setURI(java.net.URI.create(url));
            }
            
            return executeRequest(request);
        } catch (Exception e) {
            logger.error("Error executing GET request", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Perform POST request
     * 
     * @param endpoint API endpoint
     * @param body Request body
     * @return API response
     */
    public ApiResponse post(String endpoint, String body) {
        logger.info("Making POST request to: {}", endpoint);
        
        try {
            HttpPost request = createHttpPost(endpoint);
            
            // Apply authentication token if available
            applyAuthToken(request);
            
            // Set request body
            if (body != null) {
                request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            }
            
            return executeRequest(request);
        } catch (Exception e) {
            logger.error("Error executing POST request", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Perform PUT request
     * 
     * @param endpoint API endpoint
     * @param body Request body
     * @return API response
     */
    public ApiResponse put(String endpoint, String body) {
        logger.info("Making PUT request to: {}", endpoint);
        
        try {
            HttpPut request = createHttpPut(endpoint);
            
            // Apply authentication token if available
            applyAuthToken(request);
            
            // Set request body
            if (body != null) {
                request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
            }
            
            return executeRequest(request);
        } catch (Exception e) {
            logger.error("Error executing PUT request", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Perform DELETE request
     * 
     * @param endpoint API endpoint
     * @return API response
     */
    public ApiResponse delete(String endpoint) {
        logger.info("Making DELETE request to: {}", endpoint);
        
        try {
            HttpDelete request = createHttpDelete(endpoint);
            
            // Apply authentication token if available
            applyAuthToken(request);
            
            return executeRequest(request);
        } catch (Exception e) {
            logger.error("Error executing DELETE request", e);
            return ApiResponse.error(e);
        }
    }
    
    /**
     * Create HTTP client with configuration
     * 
     * @return Configured HTTP client
     */
    private CloseableHttpClient createHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
                
        return HttpClientBuilder.create()
                .setDefaultRequestConfig(config)
                .build();
    }
    
    /**
     * Create HTTP GET request
     * 
     * @param endpoint API endpoint
     * @return Configured HTTP GET request
     */
    private HttpGet createHttpGet(String endpoint) {
        HttpGet request = new HttpGet(baseUrl + endpoint);
        applyDefaultHeaders(request);
        return request;
    }
    
    /**
     * Create HTTP POST request
     * 
     * @param endpoint API endpoint
     * @return Configured HTTP POST request
     */
    private HttpPost createHttpPost(String endpoint) {
        HttpPost request = new HttpPost(baseUrl + endpoint);
        applyDefaultHeaders(request);
        return request;
    }
    
    /**
     * Create HTTP PUT request
     * 
     * @param endpoint API endpoint
     * @return Configured HTTP PUT request
     */
    private HttpPut createHttpPut(String endpoint) {
        HttpPut request = new HttpPut(baseUrl + endpoint);
        applyDefaultHeaders(request);
        return request;
    }
    
    /**
     * Create HTTP DELETE request
     * 
     * @param endpoint API endpoint
     * @return Configured HTTP DELETE request
     */
    private HttpDelete createHttpDelete(String endpoint) {
        HttpDelete request = new HttpDelete(baseUrl + endpoint);
        applyDefaultHeaders(request);
        return request;
    }
    
    /**
     * Apply default headers to HTTP request
     * 
     * @param request HTTP request
     */
    private void applyDefaultHeaders(HttpUriRequest request) {
        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Apply authentication token to HTTP request if available
     * 
     * @param request HTTP request
     */
    private void applyAuthToken(HttpUriRequest request) {
        if (authToken != null) {
            request.setHeader("Authorization", "Bearer " + authToken);
        }
    }
    
    /**
     * Execute HTTP request
     * 
     * @param request HTTP request
     * @return API response
     * @throws IOException if an I/O error occurs
     */
    private ApiResponse executeRequest(HttpUriRequest request) throws IOException {
        Instant startTime = Instant.now();
        
        try (CloseableHttpClient client = createHttpClient()) {
            CloseableHttpResponse response = client.execute(request);
            Instant endTime = Instant.now();
            
            ApiResponse apiResponse = new ApiResponse(response, startTime, endTime);
            
            // Add request/response logging to Allure report
            addAllureAttachments(request, response, apiResponse);
            
            return apiResponse;
        }
    }
    
    /**
     * Add request/response information as Allure attachments
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param apiResponse API response object
     */
    private void addAllureAttachments(HttpUriRequest request, HttpResponse response, ApiResponse apiResponse) {
        // Create request details
        StringBuilder requestDetails = new StringBuilder();
        requestDetails.append(request.getMethod()).append(" ").append(request.getURI()).append("\n\n");
        
        requestDetails.append("Headers:\n");
        for (org.apache.http.Header header : request.getAllHeaders()) {
            String headerValue = header.getName().toLowerCase().contains("authorization") ? 
                "Bearer [REDACTED]" : header.getValue();
            requestDetails.append(header.getName()).append(": ").append(headerValue).append("\n");
        }
        
        if (request instanceof HttpEntityEnclosingRequest) {
            try {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                if (entity != null) {
                    String body = EntityUtils.toString(entity);
                    // Redact sensitive information
                    body = body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"[REDACTED]\"");
                    requestDetails.append("\nBody:\n").append(body);
                }
            } catch (IOException e) {
                requestDetails.append("\nBody: [Could not read request body]");
            }
        }
        
        Allure.addAttachment("API Request", "text/plain", requestDetails.toString());
        
        // Create response details
        StringBuilder responseDetails = new StringBuilder();
        responseDetails.append("Status: ")
                      .append(response.getStatusLine().getStatusCode())
                      .append(" ")
                      .append(response.getStatusLine().getReasonPhrase())
                      .append("\n\n");
        
        responseDetails.append("Headers:\n");
        for (org.apache.http.Header header : response.getAllHeaders()) {
            responseDetails.append(header.getName()).append(": ").append(header.getValue()).append("\n");
        }
        
        responseDetails.append("\nBody:\n").append(apiResponse.getBody());
        responseDetails.append("\n\nResponse Time: ").append(apiResponse.getResponseTimeMs()).append(" ms");
        
        Allure.addAttachment("API Response", "text/plain", responseDetails.toString());
    }
    
    /**
     * Get the current authentication token
     * 
     * @return Authentication token
     */
    public String getAuthToken() {
        return authToken;
    }
    
    /**
     * Set the authentication token
     * 
     * @param authToken Authentication token
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    
    /**
     * Get the current refresh token
     * 
     * @return Refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * Set the refresh token
     * 
     * @param refreshToken Refresh token
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    /**
     * Add or update default header
     * 
     * @param name Header name
     * @param value Header value
     */
    public void setDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
    }
    
    /**
     * API response class
     */
    public static class ApiResponse {
        private final int statusCode;
        private final Map<String, String> headers;
        private final String body;
        private final long responseTimeMs;
        private final Throwable error;
        
        /**
         * Create API response from HTTP response
         * 
         * @param response HTTP response
         * @param startTime Request start time
         * @param endTime Request end time
         * @throws IOException if an I/O error occurs
         */
        public ApiResponse(HttpResponse response, Instant startTime, Instant endTime) throws IOException {
            this.statusCode = response.getStatusLine().getStatusCode();
            this.headers = new HashMap<>();
            for (org.apache.http.Header header : response.getAllHeaders()) {
                this.headers.put(header.getName(), header.getValue());
            }
            
            HttpEntity entity = response.getEntity();
            this.body = entity != null ? EntityUtils.toString(entity) : "";
            this.responseTimeMs = java.time.Duration.between(startTime, endTime).toMillis();
            this.error = null;
        }
        
        /**
         * Create error API response
         * 
         * @param error Error that occurred
         */
        private ApiResponse(Throwable error) {
            this.statusCode = -1;
            this.headers = Collections.emptyMap();
            this.body = error.getMessage();
            this.responseTimeMs = -1;
            this.error = error;
        }
        
        /**
         * Create error API response
         * 
         * @param error Error that occurred
         * @return Error API response
         */
        public static ApiResponse error(Throwable error) {
            return new ApiResponse(error);
        }
        
        /**
         * Check if response has a successful status code (2xx)
         * 
         * @return true if successful, false otherwise
         */
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        /**
         * Get response status code
         * 
         * @return HTTP status code
         */
        public int getStatusCode() {
            return statusCode;
        }
        
        /**
         * Get response headers
         * 
         * @return Map of header names to values
         */
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        /**
         * Get response body as string
         * 
         * @return Response body
         */
        public String getBody() {
            return body;
        }
        
        /**
         * Get response body as JSON object
         * 
         * @return JSON object
         */
        public JSONObject getJsonBody() {
            if (body == null || body.isEmpty()) {
                return new JSONObject();
            }
            return new JSONObject(body);
        }
        
        /**
         * Get response body as JSON array
         * 
         * @return JSON array
         */
        public JSONArray getJsonArrayBody() {
            if (body == null || body.isEmpty()) {
                return new JSONArray();
            }
            return new JSONArray(body);
        }
        
        /**
         * Get response time in milliseconds
         * 
         * @return Response time
         */
        public long getResponseTimeMs() {
            return responseTimeMs;
        }
        
        /**
         * Get error if request failed
         * 
         * @return Error or null if request succeeded
         */
        public Throwable getError() {
            return error;
        }
        
        /**
         * Check if an error occurred
         * 
         * @return true if error occurred, false otherwise
         */
        public boolean hasError() {
            return error != null;
        }
    }
}
