package com.securitytests.utils.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced logging utility that provides structured logging with context
 */
public class StructuredLogger {
    private final Logger logger;
    private static final ThreadLocal<String> testIdThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> stepIdThreadLocal = new ThreadLocal<>();
    
    /**
     * Create a new StructuredLogger for the given class
     * @param clazz The class to log for
     */
    public StructuredLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }
    
    /**
     * Start a new test session with a unique ID
     * @param testName The name of the test
     * @return The generated test ID
     */
    public String startTest(String testName) {
        String testId = UUID.randomUUID().toString();
        testIdThreadLocal.set(testId);
        MDC.put("testId", testId);
        MDC.put("testName", testName);
        
        Map<String, Object> context = new HashMap<>();
        context.put("event", "test_start");
        context.put("testName", testName);
        
        info("Test started: {}", testName, context);
        return testId;
    }
    
    /**
     * Start a new test step
     * @param stepName The name of the step
     * @return The generated step ID
     */
    public String startStep(String stepName) {
        String stepId = UUID.randomUUID().toString();
        stepIdThreadLocal.set(stepId);
        MDC.put("stepId", stepId);
        MDC.put("stepName", stepName);
        
        Map<String, Object> context = new HashMap<>();
        context.put("event", "step_start");
        context.put("stepName", stepName);
        
        info("Step started: {}", stepName, context);
        return stepId;
    }
    
    /**
     * End the current test step
     * @param status The status of the step (pass, fail, skip)
     */
    public void endStep(String status) {
        String stepId = stepIdThreadLocal.get();
        String stepName = MDC.get("stepName");
        
        if (stepId == null) {
            warn("Attempting to end step that was not started");
            return;
        }
        
        Map<String, Object> context = new HashMap<>();
        context.put("event", "step_end");
        context.put("stepName", stepName);
        context.put("stepStatus", status);
        
        info("Step completed: {} - Status: {}", stepName, status, context);
        
        MDC.remove("stepId");
        MDC.remove("stepName");
        stepIdThreadLocal.remove();
    }
    
    /**
     * End the current test
     * @param status The status of the test (pass, fail, skip)
     */
    public void endTest(String status) {
        String testId = testIdThreadLocal.get();
        String testName = MDC.get("testName");
        
        if (testId == null) {
            warn("Attempting to end test that was not started");
            return;
        }
        
        Map<String, Object> context = new HashMap<>();
        context.put("event", "test_end");
        context.put("testName", testName);
        context.put("testStatus", status);
        
        info("Test completed: {} - Status: {}", testName, status, context);
        
        MDC.remove("testId");
        MDC.remove("testName");
        testIdThreadLocal.remove();
    }
    
    /**
     * Log a mobile app event
     * @param eventType Type of app event (click, input, etc.)
     * @param elementId ID of the element involved
     * @param details Additional details about the event
     */
    public void appEvent(String eventType, String elementId, String details) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "app_" + eventType);
        context.put("elementId", elementId);
        
        info("App {}: {} - {}", eventType, elementId, details, context);
    }
    
    /**
     * Log an API call
     * @param method HTTP method
     * @param endpoint API endpoint
     * @param responseCode HTTP response code
     */
    public void apiCall(String method, String endpoint, int responseCode) {
        Map<String, Object> context = new HashMap<>();
        context.put("event", "api_call");
        context.put("method", method);
        context.put("endpoint", endpoint);
        context.put("responseCode", responseCode);
        
        info("API Call: {} {} - Response: {}", method, endpoint, responseCode, context);
    }
    
    /**
     * Log a message with INFO level
     * @param message The message to log
     * @param args Arguments to the message
     */
    public void info(String message, Object... args) {
        logger.info(message, args);
    }
    
    /**
     * Log a message with INFO level and additional context
     * @param message The message to log
     * @param args Arguments to the message (last arg should be a Map of context)
     */
    public void info(String message, Object[] args, Map<String, Object> context) {
        addContextToMDC(context);
        logger.info(message, args);
        removeContextFromMDC(context);
    }
    
    /**
     * Log a message with ERROR level
     * @param message The message to log
     * @param args Arguments to the message
     */
    public void error(String message, Object... args) {
        logger.error(message, args);
    }
    
    /**
     * Log a message with ERROR level and additional context
     * @param message The message to log
     * @param args Arguments to the message (last arg should be a Map of context)
     */
    public void error(String message, Object[] args, Map<String, Object> context) {
        addContextToMDC(context);
        logger.error(message, args);
        removeContextFromMDC(context);
    }
    
    /**
     * Log a message with WARN level
     * @param message The message to log
     * @param args Arguments to the message
     */
    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }
    
    /**
     * Log a message with WARN level and additional context
     * @param message The message to log
     * @param args Arguments to the message (last arg should be a Map of context)
     */
    public void warn(String message, Object[] args, Map<String, Object> context) {
        addContextToMDC(context);
        logger.warn(message, args);
        removeContextFromMDC(context);
    }
    
    /**
     * Log a message with DEBUG level
     * @param message The message to log
     * @param args Arguments to the message
     */
    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }
    
    /**
     * Log a message with DEBUG level and additional context
     * @param message The message to log
     * @param args Arguments to the message (last arg should be a Map of context)
     */
    public void debug(String message, Object[] args, Map<String, Object> context) {
        addContextToMDC(context);
        logger.debug(message, args);
        removeContextFromMDC(context);
    }
    
    /**
     * Add context values to MDC
     * @param context The context values to add
     */
    private void addContextToMDC(Map<String, Object> context) {
        if (context == null) return;
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (entry.getValue() != null) {
                MDC.put(entry.getKey(), entry.getValue().toString());
            }
        }
    }
    
    /**
     * Remove context values from MDC
     * @param context The context values to remove
     */
    private void removeContextFromMDC(Map<String, Object> context) {
        if (context == null) return;
        
        for (String key : context.keySet()) {
            MDC.remove(key);
        }
    }
}
