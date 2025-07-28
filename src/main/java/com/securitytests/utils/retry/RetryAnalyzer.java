package com.securitytests.utils.retry;

import com.securitytests.utils.logging.StructuredLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG retry analyzer for handling flaky tests
 */
public class RetryAnalyzer implements IRetryAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRY_COUNT = 2;
    private static final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();
    private static final StructuredLogger structuredLogger = new StructuredLogger(RetryAnalyzer.class);
    
    // Metrics collection for test stability
    private static final Map<String, Integer> testExecutionMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> testFailureMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> testRetrySuccessMap = new ConcurrentHashMap<>();
    
    @Override
    public boolean retry(ITestResult result) {
        String testKey = getTestKey(result);
        int retryCount = retryCountMap.getOrDefault(testKey, 0);
        
        // Update test execution metrics
        testExecutionMap.put(testKey, testExecutionMap.getOrDefault(testKey, 0) + 1);
        
        if (result.isSuccess()) {
            // If test passes after retry, count it as a retry success
            if (retryCount > 0) {
                testRetrySuccessMap.put(testKey, testRetrySuccessMap.getOrDefault(testKey, 0) + 1);
            }
            return false;
        }
        
        // Update test failure metrics
        testFailureMap.put(testKey, testFailureMap.getOrDefault(testKey, 0) + 1);
        
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            retryCountMap.put(testKey, retryCount);
            
            Map<String, Object> context = new HashMap<>();
            context.put("testName", result.getMethod().getMethodName());
            context.put("retryCount", retryCount);
            context.put("maxRetries", MAX_RETRY_COUNT);
            context.put("exceptionMessage", result.getThrowable().getMessage());
            
            structuredLogger.info(
                "Test '{}' failed, retrying ({}/{}): {}", 
                new Object[]{result.getMethod().getMethodName(), retryCount, MAX_RETRY_COUNT, 
                    result.getThrowable().getMessage()},
                context
            );
            
            return true;
        } else {
            structuredLogger.error("Test '{}' failed after {} retries", 
                result.getMethod().getMethodName(), MAX_RETRY_COUNT);
        }
        
        return false;
    }
    
    /**
     * Get a unique key for the test
     * @param result The test result
     * @return A unique key for the test
     */
    private String getTestKey(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName() + 
               (result.getParameters().length > 0 ? "_" + result.getParameters()[0] : "");
    }
    
    /**
     * Get the flakiness percentage for a given test
     * @param testName The test name
     * @return The flakiness percentage (0-100)
     */
    public static double getFlakinessPercentage(String testName) {
        int executions = testExecutionMap.getOrDefault(testName, 0);
        if (executions == 0) return 0.0;
        
        int failures = testFailureMap.getOrDefault(testName, 0);
        return (double) failures / executions * 100.0;
    }
    
    /**
     * Get the retry success percentage for a given test
     * @param testName The test name
     * @return The retry success percentage (0-100)
     */
    public static double getRetrySuccessPercentage(String testName) {
        int failures = testFailureMap.getOrDefault(testName, 0);
        if (failures == 0) return 0.0;
        
        int retrySuccesses = testRetrySuccessMap.getOrDefault(testName, 0);
        return (double) retrySuccesses / failures * 100.0;
    }
    
    /**
     * Get the overall stability metrics for all tests
     * @return Map containing stability metrics
     */
    public static Map<String, Object> getStabilityMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        int totalExecutions = 0;
        int totalFailures = 0;
        int totalRetrySuccesses = 0;
        
        for (Map.Entry<String, Integer> entry : testExecutionMap.entrySet()) {
            totalExecutions += entry.getValue();
        }
        
        for (Map.Entry<String, Integer> entry : testFailureMap.entrySet()) {
            totalFailures += entry.getValue();
        }
        
        for (Map.Entry<String, Integer> entry : testRetrySuccessMap.entrySet()) {
            totalRetrySuccesses += entry.getValue();
        }
        
        metrics.put("totalExecutions", totalExecutions);
        metrics.put("totalFailures", totalFailures);
        metrics.put("totalRetrySuccesses", totalRetrySuccesses);
        
        double overallFlakinessPercentage = totalExecutions > 0 ? 
            (double) totalFailures / totalExecutions * 100.0 : 0.0;
        metrics.put("overallFlakinessPercentage", overallFlakinessPercentage);
        
        double overallRetrySuccessPercentage = totalFailures > 0 ? 
            (double) totalRetrySuccesses / totalFailures * 100.0 : 0.0;
        metrics.put("overallRetrySuccessPercentage", overallRetrySuccessPercentage);
        
        return metrics;
    }
}
