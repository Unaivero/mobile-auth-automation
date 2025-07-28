package com.securitytests.api.auth;

import com.securitytests.api.ApiClient;
import com.securitytests.api.ApiResponse;
import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Step;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API service for authentication-related endpoints
 */
public class AuthApiService {
    private final ApiClient apiClient;
    private static final StructuredLogger logger = new StructuredLogger(AuthApiService.class);
    
    /**
     * Create a new authentication API service
     * @param baseUrl The base URL for the API
     */
    public AuthApiService(String baseUrl) {
        this.apiClient = new ApiClient(baseUrl);
    }
    
    /**
     * Attempt to log in with the given credentials
     * @param username Username
     * @param password Password
     * @return API response with auth token
     * @throws IOException If there's an error making the request
     */
    @Step("API: Login with username {username}")
    public ApiResponse login(String username, String password) throws IOException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("username", username);
        requestBody.put("password", password);
        
        logger.info("Attempting API login for user: {}", username);
        ApiResponse response = apiClient.post("/auth/login", requestBody);
        
        if (response.isSuccess()) {
            String token = response.getJsonValue("data.token");
            if (token != null && !token.isEmpty()) {
                apiClient.setAuthToken(token);
                logger.info("Login successful for user: {}", username);
            } else {
                logger.warn("Login response missing token for user: {}", username);
            }
        } else {
            logger.warn("Login failed for user: {}, status: {}", username, response.getStatusCode());
        }
        
        return response;
    }
    
    /**
     * Request a password reset for the given email
     * @param email Email address
     * @return API response
     * @throws IOException If there's an error making the request
     */
    @Step("API: Request password reset for {email}")
    public ApiResponse requestPasswordReset(String email) throws IOException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", email);
        
        logger.info("Requesting password reset for email: {}", email);
        ApiResponse response = apiClient.post("/auth/password/reset-request", requestBody);
        
        if (response.isSuccess()) {
            logger.info("Password reset requested successfully for: {}", email);
        } else {
            logger.warn("Password reset request failed for: {}, status: {}", 
                email, response.getStatusCode());
        }
        
        return response;
    }
    
    /**
     * Reset password using token
     * @param token Reset token
     * @param newPassword New password
     * @return API response
     * @throws IOException If there's an error making the request
     */
    @Step("API: Reset password with token")
    public ApiResponse resetPassword(String token, String newPassword) throws IOException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", token);
        requestBody.put("password", newPassword);
        
        logger.info("Resetting password with token");
        ApiResponse response = apiClient.post("/auth/password/reset", requestBody);
        
        if (response.isSuccess()) {
            logger.info("Password reset successful");
        } else {
            logger.warn("Password reset failed, status: {}", response.getStatusCode());
        }
        
        return response;
    }
    
    /**
     * Validate a CAPTCHA solution
     * @param captchaId CAPTCHA ID
     * @param solution CAPTCHA solution
     * @return API response
     * @throws IOException If there's an error making the request
     */
    @Step("API: Validate CAPTCHA solution")
    public ApiResponse validateCaptcha(String captchaId, String solution) throws IOException {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("captchaId", captchaId);
        requestBody.put("solution", solution);
        
        logger.info("Validating CAPTCHA solution");
        ApiResponse response = apiClient.post("/auth/captcha/validate", requestBody);
        
        if (response.isSuccess()) {
            boolean valid = Boolean.parseBoolean(response.getJsonValue("data.valid"));
            logger.info("CAPTCHA validation result: {}", valid ? "valid" : "invalid");
        } else {
            logger.warn("CAPTCHA validation request failed, status: {}", response.getStatusCode());
        }
        
        return response;
    }
    
    /**
     * Request a new CAPTCHA
     * @return API response with CAPTCHA details
     * @throws IOException If there's an error making the request
     */
    @Step("API: Request new CAPTCHA")
    public ApiResponse requestCaptcha() throws IOException {
        logger.info("Requesting new CAPTCHA");
        ApiResponse response = apiClient.get("/auth/captcha/new");
        
        if (response.isSuccess()) {
            String captchaId = response.getJsonValue("data.captchaId");
            logger.info("New CAPTCHA received with ID: {}", captchaId);
        } else {
            logger.warn("CAPTCHA request failed, status: {}", response.getStatusCode());
        }
        
        return response;
    }
    
    /**
     * Get the current user's profile
     * @return API response with user profile
     * @throws IOException If there's an error making the request
     */
    @Step("API: Get user profile")
    public ApiResponse getUserProfile() throws IOException {
        logger.info("Getting user profile");
        ApiResponse response = apiClient.get("/auth/user/profile");
        
        if (response.isSuccess()) {
            String username = response.getJsonValue("data.username");
            logger.info("Retrieved profile for user: {}", username);
        } else {
            logger.warn("Get user profile failed, status: {}", response.getStatusCode());
        }
        
        return response;
    }
    
    /**
     * Logout the current user
     * @return API response
     * @throws IOException If there's an error making the request
     */
    @Step("API: Logout")
    public ApiResponse logout() throws IOException {
        logger.info("Logging out user");
        ApiResponse response = apiClient.post("/auth/logout", null);
        
        if (response.isSuccess()) {
            apiClient.setAuthToken(null);
            logger.info("Logout successful");
        } else {
            logger.warn("Logout failed, status: {}", response.getStatusCode());
        }
        
        return response;
    }
}
