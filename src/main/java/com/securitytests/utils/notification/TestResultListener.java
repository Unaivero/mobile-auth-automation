package com.securitytests.utils.notification;

import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.performance.PerformanceMonitor;
import org.testng.*;
import org.testng.xml.XmlSuite;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG listener that integrates with notification service to report test results
 */
public class TestResultListener implements ITestListener, ISuiteListener, IExecutionListener {
    private static final StructuredLogger logger = new StructuredLogger(TestResultListener.class);
    private final NotificationService notificationService;
    private final Map<String, TestResult> testResults;
    private final Map<String, Long> testStartTimes;
    private final Set<String> notifiedFailures;
    private final List<String> recipients;
    private final boolean notifyOnFailure;
    private final boolean sendDailySummary;
    private final double performanceThreshold;
    
    /**
     * Create a new test result listener
     */
    public TestResultListener() {
        this.notificationService = new NotificationService();
        this.testResults = new ConcurrentHashMap<>();
        this.testStartTimes = new ConcurrentHashMap<>();
        this.notifiedFailures = Collections.newSetFromMap(new ConcurrentHashMap<>());
        
        // Read configuration
        String recipientString = System.getProperty("notification.recipients", "");
        this.recipients = Arrays.asList(recipientString.split(","));
        this.notifyOnFailure = Boolean.parseBoolean(System.getProperty("notification.onFailure", "true"));
        this.sendDailySummary = Boolean.parseBoolean(System.getProperty("notification.dailySummary", "true"));
        this.performanceThreshold = Double.parseDouble(System.getProperty("notification.performanceThreshold", "1.5"));
        
        logger.info("Test result listener initialized with recipients: {}", recipients);
    }
    
    @Override
    public void onStart(ITestContext context) {
        logger.info("Starting test execution: {}", context.getName());
    }
    
    @Override
    public void onTestStart(ITestResult result) {
        String testId = getTestId(result);
        testStartTimes.put(testId, System.currentTimeMillis());
        logger.info("Test started: {}", testId);
    }
    
    @Override
    public void onTestSuccess(ITestResult result) {
        String testId = getTestId(result);
        long startTime = testStartTimes.getOrDefault(testId, System.currentTimeMillis());
        long duration = System.currentTimeMillis() - startTime;
        
        TestResult testResult = TestResult.success(
                testId,
                duration,
                result.getTestClass().getName(),
                result.getMethod().getMethodName()
        );
        
        testResults.put(testId, testResult);
        logger.info("Test passed: {} ({})", testId, testResult.getFormattedDuration());
        
        // Check for performance regression
        checkForPerformanceRegression(result, duration);
    }
    
    @Override
    public void onTestFailure(ITestResult result) {
        String testId = getTestId(result);
        long startTime = testStartTimes.getOrDefault(testId, System.currentTimeMillis());
        long duration = System.currentTimeMillis() - startTime;
        
        Throwable throwable = result.getThrowable();
        String errorMessage = throwable != null ? throwable.getMessage() : "Unknown error";
        
        TestResult testResult = TestResult.failure(
                testId,
                errorMessage,
                duration,
                result.getTestClass().getName(),
                result.getMethod().getMethodName()
        );
        
        testResults.put(testId, testResult);
        logger.error("Test failed: {} ({}): {}", testId, testResult.getFormattedDuration(), errorMessage);
        
        // Send notification for test failure if enabled
        if (notifyOnFailure && !notifiedFailures.contains(testId)) {
            notifiedFailures.add(testId);
            sendFailureNotification(result, errorMessage);
        }
    }
    
    @Override
    public void onTestSkipped(ITestResult result) {
        String testId = getTestId(result);
        
        Throwable throwable = result.getThrowable();
        String skipReason = throwable != null ? throwable.getMessage() : "Skipped";
        
        TestResult testResult = TestResult.skipped(
                testId,
                skipReason,
                result.getTestClass().getName(),
                result.getMethod().getMethodName()
        );
        
        testResults.put(testId, testResult);
        logger.info("Test skipped: {}: {}", testId, skipReason);
    }
    
    @Override
    public void onFinish(ITestContext context) {
        logger.info("Finished test execution: {}", context.getName());
    }
    
    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // Handle as a regular failure
        onTestFailure(result);
    }
    
    @Override
    public void onStart(ISuite suite) {
        logger.info("Starting test suite: {}", suite.getName());
    }
    
    @Override
    public void onFinish(ISuite suite) {
        logger.info("Finished test suite: {}", suite.getName());
    }
    
    @Override
    public void onExecutionStart() {
        logger.info("Test execution started");
    }
    
    @Override
    public void onExecutionFinish() {
        logger.info("Test execution finished");
        
        // Send daily summary if enabled
        if (sendDailySummary && !recipients.isEmpty()) {
            sendSummaryNotification();
        }
    }
    
    /**
     * Generate a unique test ID from test result
     * @param result TestNG test result
     * @return Unique test ID
     */
    private String getTestId(ITestResult result) {
        return result.getTestClass().getName() + "." + result.getMethod().getMethodName();
    }
    
    /**
     * Send notification about test failure
     * @param result TestNG test result
     * @param errorMessage Error message
     */
    private void sendFailureNotification(ITestResult result, String errorMessage) {
        if (recipients.isEmpty()) {
            logger.warn("No recipients configured, skipping failure notification");
            return;
        }
        
        String testId = getTestId(result);
        String subject = "Test Failure: " + testId;
        
        StringBuilder message = new StringBuilder();
        message.append("Test failed: ").append(testId).append("\n\n");
        message.append("Error: ").append(errorMessage).append("\n\n");
        
        // Add parameters if any
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            message.append("Parameters:\n");
            for (int i = 0; i < parameters.length; i++) {
                message.append("- Parameter ").append(i + 1).append(": ")
                       .append(parameters[i]).append("\n");
            }
            message.append("\n");
        }
        
        // Add stack trace
        Throwable throwable = result.getThrowable();
        if (throwable != null && throwable.getStackTrace() != null) {
            message.append("Stack trace:\n");
            StackTraceElement[] stack = throwable.getStackTrace();
            for (int i = 0; i < Math.min(10, stack.length); i++) {
                message.append("    ").append(stack[i]).append("\n");
            }
            if (stack.length > 10) {
                message.append("    ... ").append(stack.length - 10).append(" more\n");
            }
        }
        
        notificationService.sendTestFailureNotification(
                subject,
                message.toString(),
                recipients,
                NotificationPriority.HIGH
        );
    }
    
    /**
     * Check for performance regression and send notification if needed
     * @param result TestNG test result
     * @param duration Test duration in milliseconds
     */
    private void checkForPerformanceRegression(ITestResult result, long duration) {
        // Only check tests that have performance measurements
        String testId = getTestId(result);
        Map<String, Double> metrics = PerformanceMonitor.getMetricsForTest(testId);
        
        if (metrics != null && !metrics.isEmpty()) {
            // Check for performance regressions
            Map<String, Double> regressions = new HashMap<>();
            
            for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                String operationId = entry.getKey();
                double currentTime = entry.getValue();
                double baselineTime = PerformanceMonitor.getBaselineForOperation(operationId);
                
                // Skip if no baseline or baseline is zero
                if (baselineTime <= 0) continue;
                
                // Check if current time exceeds threshold compared to baseline
                if (currentTime > baselineTime * performanceThreshold) {
                    double percentage = ((currentTime - baselineTime) / baselineTime) * 100.0;
                    regressions.put(operationId, percentage);
                }
            }
            
            // If regressions found, send notification
            if (!regressions.isEmpty() && !recipients.isEmpty()) {
                sendPerformanceRegressionNotification(result, metrics, regressions);
            }
        }
    }
    
    /**
     * Send notification about performance regression
     * @param result TestNG test result
     * @param metrics Performance metrics
     * @param regressions Detected regressions
     */
    private void sendPerformanceRegressionNotification(
            ITestResult result, 
            Map<String, Double> metrics, 
            Map<String, Double> regressions) {
        
        String testId = getTestId(result);
        String subject = "Performance Regression: " + testId;
        
        StringBuilder message = new StringBuilder();
        message.append("Performance regression detected in test: ").append(testId).append("\n\n");
        
        // Add regression details
        message.append("Regressions:\n");
        for (Map.Entry<String, Double> entry : regressions.entrySet()) {
            String operationId = entry.getKey();
            double percentage = entry.getValue();
            double currentTime = metrics.get(operationId);
            double baselineTime = PerformanceMonitor.getBaselineForOperation(operationId);
            
            message.append("- ").append(operationId).append(": ")
                   .append(String.format("%.2f%%", percentage)).append(" slower than baseline ")
                   .append("(Current: ").append(String.format("%.2f", currentTime)).append("ms, ")
                   .append("Baseline: ").append(String.format("%.2f", baselineTime)).append("ms)\n");
        }
        
        // Get chart path if available
        String chartPath = PerformanceMonitor.generatePerformanceChart(testId);
        
        notificationService.sendPerformanceRegressionNotification(
                subject,
                message.toString(),
                metrics,
                recipients,
                NotificationPriority.MEDIUM,
                chartPath
        );
    }
    
    /**
     * Send summary notification with test results
     */
    private void sendSummaryNotification() {
        String subject = "Test Execution Summary: " + new Date();
        
        StringBuilder summary = new StringBuilder();
        summary.append("Test execution completed at ").append(new Date()).append("\n\n");
        
        // Count test results by status
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        
        for (TestResult result : testResults.values()) {
            switch (result.getStatus()) {
                case PASS:
                    passed++;
                    break;
                case FAIL:
                    failed++;
                    break;
                case SKIP:
                    skipped++;
                    break;
            }
        }
        
        summary.append("Summary:\n");
        summary.append("- Total tests: ").append(testResults.size()).append("\n");
        summary.append("- Passed: ").append(passed).append("\n");
        summary.append("- Failed: ").append(failed).append("\n");
        summary.append("- Skipped: ").append(skipped).append("\n\n");
        
        if (failed > 0) {
            summary.append("Test failures occurred. Please check the detailed results.\n");
        } else {
            summary.append("All executed tests passed successfully.\n");
        }
        
        notificationService.sendDailySummaryNotification(
                subject,
                summary.toString(),
                testResults,
                recipients
        );
    }
}
