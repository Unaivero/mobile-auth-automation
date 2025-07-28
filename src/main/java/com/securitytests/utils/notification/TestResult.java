package com.securitytests.utils.notification;

import java.util.Date;

/**
 * Class representing the result of a test execution
 */
public class TestResult {
    private final String testName;
    private final TestStatus status;
    private final String message;
    private final long duration;
    private final Date timestamp;
    private final String className;
    private final String methodName;
    
    /**
     * Create a new test result
     * @param testName Full test name
     * @param status Test status (PASS, FAIL, SKIP)
     * @param message Test result message (error message for failures)
     * @param duration Test execution duration in milliseconds
     * @param className Test class name
     * @param methodName Test method name
     */
    public TestResult(String testName, TestStatus status, String message, long duration, 
                    String className, String methodName) {
        this.testName = testName;
        this.status = status;
        this.message = message;
        this.duration = duration;
        this.timestamp = new Date();
        this.className = className;
        this.methodName = methodName;
    }
    
    /**
     * Get the test name
     * @return Full test name
     */
    public String getTestName() {
        return testName;
    }
    
    /**
     * Get the test status
     * @return Test status
     */
    public TestStatus getStatus() {
        return status;
    }
    
    /**
     * Get the test result message
     * @return Test message (error message for failures)
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get the test execution duration
     * @return Duration in milliseconds
     */
    public long getDuration() {
        return duration;
    }
    
    /**
     * Get the timestamp when this result was created
     * @return Result timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the test class name
     * @return Test class name
     */
    public String getClassName() {
        return className;
    }
    
    /**
     * Get the test method name
     * @return Test method name
     */
    public String getMethodName() {
        return methodName;
    }
    
    /**
     * Get formatted duration as a string
     * @return Formatted duration string
     */
    public String getFormattedDuration() {
        if (duration < 1000) {
            return duration + "ms";
        } else if (duration < 60000) {
            return String.format("%.2fs", duration / 1000.0);
        } else {
            long minutes = duration / 60000;
            long seconds = (duration % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }
    
    /**
     * Create a success test result
     * @param testName Test name
     * @param duration Test duration in milliseconds
     * @param className Test class name
     * @param methodName Test method name
     * @return TestResult instance
     */
    public static TestResult success(String testName, long duration, String className, String methodName) {
        return new TestResult(testName, TestStatus.PASS, "Test passed", duration, className, methodName);
    }
    
    /**
     * Create a failure test result
     * @param testName Test name
     * @param errorMessage Error message
     * @param duration Test duration in milliseconds
     * @param className Test class name
     * @param methodName Test method name
     * @return TestResult instance
     */
    public static TestResult failure(String testName, String errorMessage, long duration, 
                                   String className, String methodName) {
        return new TestResult(testName, TestStatus.FAIL, errorMessage, duration, className, methodName);
    }
    
    /**
     * Create a skipped test result
     * @param testName Test name
     * @param skipReason Reason for skipping
     * @param className Test class name
     * @param methodName Test method name
     * @return TestResult instance
     */
    public static TestResult skipped(String testName, String skipReason, String className, String methodName) {
        return new TestResult(testName, TestStatus.SKIP, skipReason, 0, className, methodName);
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s [%s] - %s", 
            testName, status, getFormattedDuration(), message);
    }
}
