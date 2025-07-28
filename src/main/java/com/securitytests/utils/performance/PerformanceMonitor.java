package com.securitytests.utils.performance;

import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Attachment;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Utility to monitor and report on performance metrics for tests
 */
public class PerformanceMonitor {
    private static final StructuredLogger logger = new StructuredLogger(PerformanceMonitor.class);
    private static final Map<String, DescriptiveStatistics> operationStats = new ConcurrentHashMap<>();
    private static final Map<String, Long> activeTimers = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> testIdThreadLocal = new ThreadLocal<>();
    
    /**
     * Start tracking performance for a test
     * @param testId Unique identifier for the test
     */
    public static void startTest(String testId) {
        testIdThreadLocal.set(testId);
        logger.info("Started performance monitoring for test: {}", testId);
    }
    
    /**
     * End tracking performance for the current test
     */
    public static void endTest() {
        testIdThreadLocal.remove();
    }
    
    /**
     * Start timing an operation
     * @param operationName Name of the operation
     * @return A unique timer ID
     */
    public static String startTimer(String operationName) {
        String testId = testIdThreadLocal.get();
        String timerId = (testId != null ? testId + ":" : "") + operationName + ":" + UUID.randomUUID().toString();
        activeTimers.put(timerId, System.currentTimeMillis());
        return timerId;
    }
    
    /**
     * Stop timing an operation and record the duration
     * @param timerId Timer ID returned from startTimer
     * @return The duration in milliseconds
     */
    public static long stopTimer(String timerId) {
        Long startTime = activeTimers.remove(timerId);
        if (startTime == null) {
            logger.warn("Attempted to stop a timer that was not started: {}", timerId);
            return -1;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        String operationName = timerId.split(":", 3)[1]; // Extract operation name from timer ID
        
        // Record the duration
        DescriptiveStatistics stats = operationStats.computeIfAbsent(operationName, 
                k -> new DescriptiveStatistics());
        stats.addValue(duration);
        
        // Log the duration
        logger.info("Operation '{}' completed in {} ms", operationName, duration);
        
        return duration;
    }
    
    /**
     * Record a duration directly without using timers
     * @param operationName Name of the operation
     * @param durationMs Duration in milliseconds
     */
    public static void recordDuration(String operationName, long durationMs) {
        DescriptiveStatistics stats = operationStats.computeIfAbsent(operationName,
                k -> new DescriptiveStatistics());
        stats.addValue(durationMs);
        
        logger.info("Recorded operation '{}' duration: {} ms", operationName, durationMs);
    }
    
    /**
     * Get statistics for a specific operation
     * @param operationName Name of the operation
     * @return Map of statistics for the operation
     */
    public static Map<String, Double> getOperationStats(String operationName) {
        DescriptiveStatistics stats = operationStats.get(operationName);
        if (stats == null || stats.getN() == 0) {
            return Collections.emptyMap();
        }
        
        Map<String, Double> results = new HashMap<>();
        results.put("min", stats.getMin());
        results.put("max", stats.getMax());
        results.put("mean", stats.getMean());
        results.put("median", stats.getPercentile(50));
        results.put("p90", stats.getPercentile(90));
        results.put("p95", stats.getPercentile(95));
        results.put("p99", stats.getPercentile(99));
        results.put("stdDev", stats.getStandardDeviation());
        results.put("count", (double) stats.getN());
        
        return results;
    }
    
    /**
     * Get performance statistics for all operations
     * @return Map of statistics for all operations
     */
    public static Map<String, Map<String, Double>> getAllOperationStats() {
        Map<String, Map<String, Double>> allStats = new HashMap<>();
        for (String operation : operationStats.keySet()) {
            allStats.put(operation, getOperationStats(operation));
        }
        return allStats;
    }
    
    /**
     * Reset all performance statistics
     */
    public static void resetStats() {
        operationStats.clear();
        activeTimers.clear();
        logger.info("Performance statistics reset");
    }
    
    /**
     * Generate a performance report for Allure
     * @return The report content
     */
    @Attachment(value = "Performance Report", type = "text/plain")
    public static String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("Performance Test Report\n");
        report.append("======================\n\n");
        report.append("Generated at: ").append(new Date()).append("\n\n");
        
        // Add overall summary
        report.append("Overall Summary:\n");
        report.append("---------------\n");
        report.append(String.format("Total operations measured: %d\n\n", operationStats.size()));
        
        // Add detailed stats for each operation
        report.append("Detailed Operation Statistics:\n");
        report.append("-----------------------------\n");
        
        Map<String, Map<String, Double>> allStats = getAllOperationStats();
        List<String> operations = new ArrayList<>(allStats.keySet());
        operations.sort(Comparator.naturalOrder());
        
        for (String operation : operations) {
            Map<String, Double> stats = allStats.get(operation);
            report.append(String.format("Operation: %s\n", operation));
            report.append(String.format("  Count:  %.0f\n", stats.get("count")));
            report.append(String.format("  Min:    %.2f ms\n", stats.get("min")));
            report.append(String.format("  Max:    %.2f ms\n", stats.get("max")));
            report.append(String.format("  Mean:   %.2f ms\n", stats.get("mean")));
            report.append(String.format("  Median: %.2f ms\n", stats.get("median")));
            report.append(String.format("  90%%:    %.2f ms\n", stats.get("p90")));
            report.append(String.format("  95%%:    %.2f ms\n", stats.get("p95")));
            report.append(String.format("  99%%:    %.2f ms\n", stats.get("p99")));
            report.append(String.format("  StdDev: %.2f ms\n", stats.get("stdDev")));
            report.append("\n");
        }
        
        logger.info("Generated performance report");
        return report.toString();
    }
    
    /**
     * Format a duration in a human-readable way
     * @param durationMs Duration in milliseconds
     * @return Formatted duration string
     */
    public static String formatDuration(long durationMs) {
        if (durationMs < 1000) {
            return durationMs + " ms";
        }
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60;
        
        if (minutes == 0) {
            return String.format("%d.%03d s", seconds, durationMs % 1000);
        } else {
            return String.format("%d m %d.%03d s", minutes, seconds, durationMs % 1000);
        }
    }
}
