package com.securitytests.utils.notification;

/**
 * Enum representing possible test execution statuses
 */
public enum TestStatus {
    /**
     * Test passed successfully
     */
    PASS,
    
    /**
     * Test failed due to assertion error or exception
     */
    FAIL,
    
    /**
     * Test was skipped or not executed
     */
    SKIP
}
