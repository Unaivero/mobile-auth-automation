package com.securitytests.utils.biometric;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive result class for biometric security testing
 */
public class BiometricSecurityTestResult {
    private final LocalDateTime timestamp;
    private final Map<String, BiometricAuthResult> testResults;
    private final List<String> securityIssues;
    private final List<String> errors;
    private BiometricType testedBiometricType;
    private String testSubject;
    private int overallSecurityScore;
    
    public BiometricSecurityTestResult() {
        this.timestamp = LocalDateTime.now();
        this.testResults = new HashMap<>();
        this.securityIssues = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.overallSecurityScore = 0;
    }
    
    public void addTest(String testName, BiometricAuthResult result) {
        testResults.put(testName, result);
        updateSecurityScore();
    }
    
    public void addSecurityIssue(String issue) {
        securityIssues.add(issue);
        updateSecurityScore();
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Map<String, BiometricAuthResult> getTestResults() {
        return testResults;
    }
    
    public List<String> getSecurityIssues() {
        return securityIssues;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public BiometricType getTestedBiometricType() {
        return testedBiometricType;
    }
    
    public void setTestedBiometricType(BiometricType testedBiometricType) {
        this.testedBiometricType = testedBiometricType;
    }
    
    public String getTestSubject() {
        return testSubject;
    }
    
    public void setTestSubject(String testSubject) {
        this.testSubject = testSubject;
    }
    
    public int getOverallSecurityScore() {
        return overallSecurityScore;
    }
    
    /**
     * Calculate overall security score based on test results
     */
    private void updateSecurityScore() {
        if (testResults.isEmpty()) {
            overallSecurityScore = 0;
            return;
        }
        
        int totalScore = 100;
        int penalties = 0;
        
        // Deduct points for security issues
        penalties += securityIssues.size() * 20;
        
        // Deduct points for failed security tests
        for (BiometricAuthResult result : testResults.values()) {
            if (result.getErrorType() == BiometricAuthResult.ErrorType.SECURITY_BLOCKED) {
                // This is good - security measures are working
                continue;
            } else if (!result.isSuccess() && 
                      result.getScenario() != null && 
                      isSecurityRelatedScenario(result.getScenario())) {
                // Security test failed when it should have been blocked
                penalties += 15;
            }
        }
        
        overallSecurityScore = Math.max(0, totalScore - penalties);
    }
    
    private boolean isSecurityRelatedScenario(BiometricTestScenario scenario) {
        return scenario == BiometricTestScenario.PRESENTATION_ATTACK ||
               scenario == BiometricTestScenario.TEMPLATE_INJECTION ||
               scenario == BiometricTestScenario.REPLAY_ATTACK ||
               scenario == BiometricTestScenario.BRUTE_FORCE_ATTACK;
    }
    
    /**
     * Get security assessment summary
     */
    public SecurityAssessment getSecurityAssessment() {
        SecurityLevel level;
        String summary;
        
        if (overallSecurityScore >= 90) {
            level = SecurityLevel.HIGH;
            summary = "Biometric authentication has strong security measures";
        } else if (overallSecurityScore >= 70) {
            level = SecurityLevel.MEDIUM;
            summary = "Biometric authentication has adequate security with some improvements needed";
        } else if (overallSecurityScore >= 50) {
            level = SecurityLevel.LOW;
            summary = "Biometric authentication has significant security vulnerabilities";
        } else {
            level = SecurityLevel.CRITICAL;
            summary = "Biometric authentication has critical security issues that must be addressed";
        }
        
        return new SecurityAssessment(level, summary, securityIssues, getRecommendations());
    }
    
    /**
     * Get security recommendations based on test results
     */
    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        if (overallSecurityScore < 90) {
            recommendations.add("Implement presentation attack detection");
            recommendations.add("Add rate limiting for authentication attempts");
            recommendations.add("Enhance template security and encryption");
        }
        
        if (securityIssues.size() > 0) {
            recommendations.add("Address identified security vulnerabilities");
            recommendations.add("Conduct regular security assessments");
        }
        
        for (BiometricAuthResult result : testResults.values()) {
            if (result.getErrorType() == BiometricAuthResult.ErrorType.HARDWARE_ERROR) {
                recommendations.add("Improve hardware reliability and error handling");
            }
        }
        
        return recommendations;
    }
    
    /**
     * Generate detailed security report
     */
    public String generateSecurityReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== BIOMETRIC SECURITY TEST REPORT ===\n");
        report.append("Timestamp: ").append(timestamp).append("\n");
        report.append("Biometric Type: ").append(testedBiometricType != null ? testedBiometricType.getDisplayName() : "N/A").append("\n");
        report.append("Test Subject: ").append(testSubject != null ? testSubject : "N/A").append("\n");
        report.append("Overall Security Score: ").append(overallSecurityScore).append("/100\n\n");
        
        SecurityAssessment assessment = getSecurityAssessment();
        report.append("Security Level: ").append(assessment.getLevel()).append("\n");
        report.append("Assessment: ").append(assessment.getSummary()).append("\n\n");
        
        report.append("=== TEST RESULTS ===\n");
        for (Map.Entry<String, BiometricAuthResult> entry : testResults.entrySet()) {
            BiometricAuthResult result = entry.getValue();
            report.append(String.format("Test: %s - %s\n", 
                entry.getKey(), 
                result.isSuccess() ? "PASS" : "FAIL (" + result.getMessage() + ")")
            );
        }
        
        if (!securityIssues.isEmpty()) {
            report.append("\n=== SECURITY ISSUES ===\n");
            for (String issue : securityIssues) {
                report.append("- ").append(issue).append("\n");
            }
        }
        
        if (!errors.isEmpty()) {
            report.append("\n=== ERRORS ===\n");
            for (String error : errors) {
                report.append("- ").append(error).append("\n");
            }
        }
        
        List<String> recommendations = getRecommendations();
        if (!recommendations.isEmpty()) {
            report.append("\n=== RECOMMENDATIONS ===\n");
            for (String recommendation : recommendations) {
                report.append("- ").append(recommendation).append("\n");
            }
        }
        
        return report.toString();
    }
    
    @Override
    public String toString() {
        return String.format("BiometricSecurityTestResult{score=%d/100, tests=%d, issues=%d, errors=%d}",
            overallSecurityScore, testResults.size(), securityIssues.size(), errors.size());
    }
    
    /**
     * Security assessment data class
     */
    public static class SecurityAssessment {
        private final SecurityLevel level;
        private final String summary;
        private final List<String> issues;
        private final List<String> recommendations;
        
        public SecurityAssessment(SecurityLevel level, String summary, List<String> issues, List<String> recommendations) {
            this.level = level;
            this.summary = summary;
            this.issues = new ArrayList<>(issues);
            this.recommendations = new ArrayList<>(recommendations);
        }
        
        public SecurityLevel getLevel() {
            return level;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public List<String> getIssues() {
            return issues;
        }
        
        public List<String> getRecommendations() {
            return recommendations;
        }
    }
    
    /**
     * Security level enumeration
     */
    public enum SecurityLevel {
        CRITICAL("Critical", 4),
        LOW("Low", 3),
        MEDIUM("Medium", 2),
        HIGH("High", 1);
        
        private final String name;
        private final int priority;
        
        SecurityLevel(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
        
        public String getName() {
            return name;
        }
        
        public int getPriority() {
            return priority;
        }
    }
}