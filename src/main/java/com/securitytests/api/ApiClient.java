package com.securitytests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securitytests.utils.logging.StructuredLogger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic API client for making HTTP requests
 */
public class ApiClient {
    private static final StructuredLogger logger = new StructuredLogger(ApiClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String baseUrl;
    private final Map<String, String> defaultHeaders;
    private String authToken;
    
    /**
     * Create a new API client
     * @param baseUrl Base URL for all API calls
     */
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.defaultHeaders = new HashMap<>();
        defaultHeaders.put("Content-Type", "application/json");
        defaultHeaders.put("Accept", "application/json");
    }
    
    /**
     * Set the authentication token
     * @param authToken The auth token
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        if (authToken != null && !authToken.isEmpty()) {
            defaultHeaders.put("Authorization", "Bearer " + authToken);
        } else {
            defaultHeaders.remove("Authorization");
        }
    }
    
    /**
     * Add a default header to all requests
     * @param name Header name
     * @param value Header value
     */
    public void addDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
    }
    
    /**
     * Make a GET request
     * @param endpoint The API endpoint
     * @return The API response as JsonNode
     * @throws IOException If there's an error making the request
     */
    public ApiResponse get(String endpoint) throws IOException {
        return executeRequest(new HttpGet(baseUrl + endpoint));
    }
    
    /**
     * Make a POST request
     * @param endpoint The API endpoint
     * @param body The request body as object (will be serialized to JSON)
     * @return The API response as JsonNode
     * @throws IOException If there's an error making the request
     */
    public ApiResponse post(String endpoint, Object body) throws IOException {
        HttpPost request = new HttpPost(baseUrl + endpoint);
        String jsonBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(jsonBody));
        return executeRequest(request);
    }
    
    /**
     * Make a PUT request
     * @param endpoint The API endpoint
     * @param body The request body as object (will be serialized to JSON)
     * @return The API response as JsonNode
     * @throws IOException If there's an error making the request
     */
    public ApiResponse put(String endpoint, Object body) throws IOException {
        HttpPut request = new HttpPut(baseUrl + endpoint);
        String jsonBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(jsonBody));
        return executeRequest(request);
    }
    
    /**
     * Make a DELETE request
     * @param endpoint The API endpoint
     * @return The API response as JsonNode
     * @throws IOException If there's an error making the request
     */
    public ApiResponse delete(String endpoint) throws IOException {
        return executeRequest(new HttpDelete(baseUrl + endpoint));
    }
    
    /**
     * Execute an HTTP request with default settings
     * @param request The HTTP request to execute
     * @return The API response
     * @throws IOException If there's an error executing the request
     */
    private ApiResponse executeRequest(HttpUriRequest request) throws IOException {
        // Add default headers
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            request.setHeader(header.getKey(), header.getValue());
        }
        
        // Configure timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(30000)
                .setConnectionRequestTimeout(5000)
                .build();
        
        // Log request details
        Map<String, Object> context = new HashMap<>();
        context.put("method", request.getMethod());
        context.put("url", request.getURI().toString());
        context.put("headers", defaultHeaders);
        
        logger.apiCall(request.getMethod(), request.getURI().toString(), 0);
        
        // Execute the request
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            
            long startTime = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(request);
            long endTime = System.currentTimeMillis();
            
            // Process response
            HttpEntity entity = response.getEntity();
            String responseBody = entity != null ? EntityUtils.toString(entity) : "";
            int statusCode = response.getStatusLine().getStatusCode();
            
            // Log response details
            context.put("statusCode", statusCode);
            context.put("responseTimeMs", endTime - startTime);
            
            logger.apiCall(request.getMethod(), request.getURI().toString(), statusCode);
            
            // Parse response body as JSON
            JsonNode jsonResponse = null;
            if (!responseBody.isEmpty()) {
                try {
                    jsonResponse = objectMapper.readTree(responseBody);
                } catch (Exception e) {
                    // Not JSON or invalid JSON
                    logger.warn("Failed to parse response as JSON: {}", e.getMessage());
                }
            }
            
            return new ApiResponse(statusCode, responseBody, jsonResponse);
        }
    }
}
