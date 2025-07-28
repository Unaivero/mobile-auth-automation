package com.securitytests.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Represents an API response
 */
public class ApiResponse {
    private final int statusCode;
    private final String responseBody;
    private final JsonNode jsonResponse;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Create a new API response
     * @param statusCode HTTP status code
     * @param responseBody Raw response body
     * @param jsonResponse Response parsed as JSON (may be null)
     */
    public ApiResponse(int statusCode, String responseBody, JsonNode jsonResponse) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.jsonResponse = jsonResponse;
    }
    
    /**
     * Get the HTTP status code
     * @return The status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Get the raw response body
     * @return The response body
     */
    public String getResponseBody() {
        return responseBody;
    }
    
    /**
     * Get the response as JsonNode
     * @return The response as JsonNode, or null if not JSON
     */
    public JsonNode getJsonResponse() {
        return jsonResponse;
    }
    
    /**
     * Check if the response is successful (2xx status code)
     * @return True if successful, false otherwise
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Parse the response body into an object
     * @param clazz The class to parse into
     * @param <T> The type to parse into
     * @return The parsed object
     * @throws IOException If there's an error parsing
     */
    public <T> T parseAs(Class<T> clazz) throws IOException {
        return objectMapper.readValue(responseBody, clazz);
    }
    
    /**
     * Get a value from the JSON response
     * @param path The JSON path (e.g. "data.user.name")
     * @return The value, or null if not found
     */
    public String getJsonValue(String path) {
        if (jsonResponse == null) return null;
        
        String[] parts = path.split("\\.");
        JsonNode current = jsonResponse;
        
        for (String part : parts) {
            current = current.get(part);
            if (current == null) return null;
        }
        
        return current.asText();
    }
    
    /**
     * Get a JSON node from the JSON response
     * @param path The JSON path (e.g. "data.user")
     * @return The JSON node, or null if not found
     */
    public JsonNode getJsonNode(String path) {
        if (jsonResponse == null) return null;
        
        String[] parts = path.split("\\.");
        JsonNode current = jsonResponse;
        
        for (String part : parts) {
            current = current.get(part);
            if (current == null) return null;
        }
        
        return current;
    }
    
    @Override
    public String toString() {
        return "ApiResponse{" +
                "statusCode=" + statusCode +
                ", responseBody='" + (responseBody.length() > 100 ? responseBody.substring(0, 97) + "..." : responseBody) + '\'' +
                '}';
    }
}
