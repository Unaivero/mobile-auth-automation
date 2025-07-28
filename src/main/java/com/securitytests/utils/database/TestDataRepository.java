package com.securitytests.utils.database;

import com.securitytests.utils.logging.StructuredLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Repository class for managing test data in the database
 * Provides CRUD operations for test users, scenarios, and execution results
 */
public class TestDataRepository {
    private static final StructuredLogger logger = new StructuredLogger(TestDataRepository.class);
    private final DatabaseManager dbManager;
    private final ObjectMapper objectMapper;
    
    public TestDataRepository() {
        this.dbManager = DatabaseManager.getInstance();
        this.objectMapper = new ObjectMapper();
    }
    
    // ==================== TEST USERS ====================
    
    /**
     * Get a test user by username
     */
    public Map<String, Object> getTestUserByUsername(String username) {
        String sql = "SELECT * FROM test_users WHERE username = ?";
        List<Map<String, Object>> results = dbManager.executeQuery(sql, username);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Get a test user by email
     */
    public Map<String, Object> getTestUserByEmail(String email) {
        String sql = "SELECT * FROM test_users WHERE email = ?";
        List<Map<String, Object>> results = dbManager.executeQuery(sql, email);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Create a new test user
     */
    public UUID createTestUser(String username, String email, String passwordHash, String salt, 
                              boolean biometricEnabled, boolean twoFactorEnabled, String userRole) {
        String sql = "INSERT INTO test_users (username, email, password_hash, salt, " +
                    "biometric_enabled, two_factor_enabled, user_role) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        List<Map<String, Object>> results = dbManager.executeQuery(sql, username, email, 
            passwordHash, salt, biometricEnabled, twoFactorEnabled, userRole);
        
        if (!results.isEmpty()) {
            UUID userId = (UUID) results.get(0).get("id");
            logger.info("Created test user: {} with ID: {}", username, userId);
            return userId;
        }
        
        throw new RuntimeException("Failed to create test user: " + username);
    }
    
    /**
     * Update user login attempt information
     */
    public void updateUserLoginAttempt(String username, boolean successful) {
        Map<String, Object> user = getTestUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("User not found: " + username);
        }
        
        if (successful) {
            // Reset failed attempts on successful login
            String sql = "UPDATE test_users SET failed_login_attempts = 0, " +
                        "last_login_attempt = CURRENT_TIMESTAMP, is_locked = false WHERE username = ?";
            dbManager.executeUpdate(sql, username);
        } else {
            // Increment failed attempts
            String sql = "UPDATE test_users SET failed_login_attempts = failed_login_attempts + 1, " +
                        "last_login_attempt = CURRENT_TIMESTAMP WHERE username = ?";
            dbManager.executeUpdate(sql, username);
            
            // Check if user should be locked (after 5 failed attempts)
            int failedAttempts = (Integer) user.get("failed_login_attempts") + 1;
            if (failedAttempts >= 5) {
                lockUser(username);
            }
        }
    }
    
    /**
     * Lock a user account
     */
    public void lockUser(String username) {
        String sql = "UPDATE test_users SET is_locked = true WHERE username = ?";
        dbManager.executeUpdate(sql, username);
        logger.info("User account locked: {}", username);
    }
    
    /**
     * Unlock a user account
     */
    public void unlockUser(String username) {
        String sql = "UPDATE test_users SET is_locked = false, failed_login_attempts = 0 WHERE username = ?";
        dbManager.executeUpdate(sql, username);
        logger.info("User account unlocked: {}", username);
    }
    
    // ==================== TEST SCENARIOS ====================
    
    /**
     * Get all active test scenarios by type
     */
    public List<Map<String, Object>> getTestScenariosByType(String scenarioType) {
        String sql = "SELECT * FROM test_scenarios WHERE scenario_type = ? AND is_active = true " +
                    "ORDER BY priority ASC, scenario_name";
        return dbManager.executeQuery(sql, scenarioType);
    }
    
    /**
     * Get test scenario by name
     */
    public Map<String, Object> getTestScenarioByName(String scenarioName) {
        String sql = "SELECT * FROM test_scenarios WHERE scenario_name = ?";
        List<Map<String, Object>> results = dbManager.executeQuery(sql, scenarioName);
        return results.isEmpty() ? null : results.get(0);
    }
    
    /**
     * Create a new test scenario
     */
    public UUID createTestScenario(String scenarioName, String scenarioType, 
                                  Map<String, Object> testData, String expectedResult, 
                                  String[] tags, int priority) {
        try {
            String testDataJson = objectMapper.writeValueAsString(testData);
            String sql = "INSERT INTO test_scenarios (scenario_name, scenario_type, test_data, " +
                        "expected_result, tags, priority) VALUES (?, ?, ?::jsonb, ?, ?, ?) RETURNING id";
            
            List<Map<String, Object>> results = dbManager.executeQuery(sql, scenarioName, 
                scenarioType, testDataJson, expectedResult, tags, priority);
            
            if (!results.isEmpty()) {
                UUID scenarioId = (UUID) results.get(0).get("id");
                logger.info("Created test scenario: {} with ID: {}", scenarioName, scenarioId);
                return scenarioId;
            }
        } catch (Exception e) {
            logger.error("Error creating test scenario: " + scenarioName, e);
            throw new RuntimeException("Failed to create test scenario", e);
        }
        
        throw new RuntimeException("Failed to create test scenario: " + scenarioName);
    }
    
    // ==================== TEST EXECUTION RESULTS ====================
    
    /**
     * Record a test execution result
     */
    public UUID recordTestExecution(String executionId, String testSuiteName, String testClassName,
                                   String testMethodName, String testStatus, LocalDateTime startTime,
                                   LocalDateTime endTime, Long durationMs, String errorMessage,
                                   String stackTrace, Map<String, Object> testData, String environment,
                                   String deviceName, String appVersion, String buildNumber,
                                   String gitCommitHash, String jenkinsBuildNumber) {
        try {
            String testDataJson = testData != null ? objectMapper.writeValueAsString(testData) : null;
            
            String sql = "INSERT INTO test_executions (execution_id, test_suite_name, test_class_name, " +
                        "test_method_name, test_status, start_time, end_time, duration_ms, error_message, " +
                        "stack_trace, test_data, environment, device_name, app_version, build_number, " +
                        "git_commit_hash, jenkins_build_number) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?) RETURNING id";
            
            List<Map<String, Object>> results = dbManager.executeQuery(sql, executionId, testSuiteName,
                testClassName, testMethodName, testStatus, Timestamp.valueOf(startTime),
                endTime != null ? Timestamp.valueOf(endTime) : null, durationMs, errorMessage,
                stackTrace, testDataJson, environment, deviceName, appVersion, buildNumber,
                gitCommitHash, jenkinsBuildNumber);
            
            if (!results.isEmpty()) {
                UUID testId = (UUID) results.get(0).get("id");
                logger.debug("Recorded test execution: {} - {}", testMethodName, testStatus);
                return testId;
            }
        } catch (Exception e) {
            logger.error("Error recording test execution: " + testMethodName, e);
            throw new RuntimeException("Failed to record test execution", e);
        }
        
        throw new RuntimeException("Failed to record test execution");
    }
    
    /**
     * Record a security test result
     */
    public UUID recordSecurityTestResult(UUID executionId, String vulnerabilityType, String severityLevel,
                                        String confidenceLevel, String affectedUrl, String affectedParameter,
                                        Integer cweId, String owaspCategory, String description,
                                        String solution, String[] referenceUrls, String proofOfConcept,
                                        Integer zapAlertId, Integer riskScore) {
        String sql = "INSERT INTO security_test_results (execution_id, vulnerability_type, severity_level, " +
                    "confidence_level, affected_url, affected_parameter, cwe_id, owasp_category, " +
                    "description, solution, reference_urls, proof_of_concept, zap_alert_id, risk_score) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        
        List<Map<String, Object>> results = dbManager.executeQuery(sql, executionId, vulnerabilityType,
            severityLevel, confidenceLevel, affectedUrl, affectedParameter, cweId, owaspCategory,
            description, solution, referenceUrls, proofOfConcept, zapAlertId, riskScore);
        
        if (!results.isEmpty()) {
            UUID resultId = (UUID) results.get(0).get("id");
            logger.debug("Recorded security test result: {} - {}", vulnerabilityType, severityLevel);
            return resultId;
        }
        
        throw new RuntimeException("Failed to record security test result");
    }
    
    /**
     * Record performance metrics
     */
    public void recordPerformanceMetric(UUID executionId, String metricName, double metricValue,
                                       String metricUnit, String metricType, String endpoint) {
        String sql = "INSERT INTO performance_metrics (execution_id, metric_name, metric_value, " +
                    "metric_unit, metric_type, endpoint) VALUES (?, ?, ?, ?, ?, ?)";
        
        dbManager.executeUpdate(sql, executionId, metricName, metricValue, metricUnit, metricType, endpoint);
        logger.debug("Recorded performance metric: {} = {} {}", metricName, metricValue, metricUnit);
    }
    
    // ==================== BIOMETRIC TEST DATA ====================
    
    /**
     * Enroll biometric data for a user
     */
    public UUID enrollBiometricData(UUID userId, String biometricType, byte[] templateData, String deviceId) {
        String sql = "INSERT INTO biometric_test_data (user_id, biometric_type, template_data, " +
                    "is_enrolled, device_id) VALUES (?, ?, ?, true, ?) RETURNING id";
        
        List<Map<String, Object>> results = dbManager.executeQuery(sql, userId, biometricType, 
            templateData, deviceId);
        
        if (!results.isEmpty()) {
            UUID biometricId = (UUID) results.get(0).get("id");
            logger.info("Enrolled biometric data: {} for user: {}", biometricType, userId);
            return biometricId;
        }
        
        throw new RuntimeException("Failed to enroll biometric data");
    }
    
    /**
     * Get biometric data for a user
     */
    public List<Map<String, Object>> getBiometricDataForUser(UUID userId, String biometricType) {
        String sql = "SELECT * FROM biometric_test_data WHERE user_id = ? AND biometric_type = ? " +
                    "AND is_active = true";
        return dbManager.executeQuery(sql, userId, biometricType);
    }
    
    // ==================== AUTHENTICATION EVENTS ====================
    
    /**
     * Log an authentication event
     */
    public void logAuthenticationEvent(UUID userId, String eventType, String ipAddress, String userAgent,
                                      String deviceFingerprint, Map<String, Object> locationData,
                                      Map<String, Object> additionalData, String sessionId, int riskScore) {
        try {
            String locationJson = locationData != null ? objectMapper.writeValueAsString(locationData) : null;
            String additionalJson = additionalData != null ? objectMapper.writeValueAsString(additionalData) : null;
            
            String sql = "INSERT INTO auth_events_log (user_id, event_type, ip_address, user_agent, " +
                        "device_fingerprint, location_data, additional_data, session_id, risk_score) " +
                        "VALUES (?, ?, ?::inet, ?, ?, ?::jsonb, ?::jsonb, ?, ?)";
            
            dbManager.executeUpdate(sql, userId, eventType, ipAddress, userAgent, deviceFingerprint,
                locationJson, additionalJson, sessionId, riskScore);
            
            logger.debug("Logged authentication event: {} for user: {}", eventType, userId);
        } catch (Exception e) {
            logger.error("Error logging authentication event", e);
        }
    }
    
    // ==================== REPORTING QUERIES ====================
    
    /**
     * Get test execution summary for a date range
     */
    public List<Map<String, Object>> getTestExecutionSummary(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM test_execution_summary WHERE execution_date BETWEEN ? AND ? " +
                    "ORDER BY execution_date DESC";
        return dbManager.executeQuery(sql, startDate.toLocalDate(), endDate.toLocalDate());
    }
    
    /**
     * Get security vulnerability summary
     */
    public List<Map<String, Object>> getSecurityVulnerabilitySummary(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM security_vulnerability_summary WHERE scan_date BETWEEN ? AND ? " +
                    "ORDER BY scan_date DESC, severity_level";
        return dbManager.executeQuery(sql, startDate.toLocalDate(), endDate.toLocalDate());
    }
    
    /**
     * Get test trend data for reporting
     */
    public List<Map<String, Object>> getTestTrendData(String testSuite, int days) {
        String sql = "SELECT * FROM test_trend_data WHERE test_suite = ? " +
                    "AND execution_date >= CURRENT_DATE - INTERVAL '? days' " +
                    "ORDER BY execution_date DESC";
        return dbManager.executeQuery(sql, testSuite, days);
    }
    
    /**
     * Clean up old test data (retention policy)
     */
    public void cleanupOldTestData(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        String[] cleanupQueries = {
            "DELETE FROM performance_metrics WHERE timestamp < ?",
            "DELETE FROM security_test_results WHERE found_at < ?",
            "DELETE FROM auth_events_log WHERE timestamp < ?",
            "DELETE FROM test_executions WHERE start_time < ?"
        };
        
        for (String sql : cleanupQueries) {
            int deletedRows = dbManager.executeUpdate(sql, Timestamp.valueOf(cutoffDate));
            logger.info("Cleaned up {} old records from: {}", deletedRows, sql.split(" ")[2]);
        }
    }
}