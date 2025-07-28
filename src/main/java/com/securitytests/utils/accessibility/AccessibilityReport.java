package com.securitytests.utils.accessibility;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Report containing accessibility issues found during testing
 */
public class AccessibilityReport {
    private final String screenName;
    private final List<AccessibilityIssue> issues;
    private final Date timestamp;
    
    /**
     * Create a new accessibility report
     * @param screenName Name of the screen tested
     * @param issues List of issues found
     */
    public AccessibilityReport(String screenName, List<AccessibilityIssue> issues) {
        this.screenName = screenName;
        this.issues = Collections.unmodifiableList(issues);
        this.timestamp = new Date();
    }
    
    /**
     * Get the name of the screen tested
     * @return Screen name
     */
    public String getScreenName() {
        return screenName;
    }
    
    /**
     * Get all issues found
     * @return List of issues
     */
    public List<AccessibilityIssue> getIssues() {
        return issues;
    }
    
    /**
     * Get the timestamp when this report was created
     * @return Report timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the total number of issues found
     * @return Issue count
     */
    public int getIssueCount() {
        return issues.size();
    }
    
    /**
     * Check if the report has any issues
     * @return True if issues were found, false otherwise
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
    
    /**
     * Get issues grouped by severity
     * @return Map of severity to list of issues
     */
    public Map<AccessibilityIssue.Severity, List<AccessibilityIssue>> getIssuesBySeverity() {
        return issues.stream().collect(Collectors.groupingBy(AccessibilityIssue::getSeverity));
    }
    
    /**
     * Get issues grouped by type
     * @return Map of issue type to list of issues
     */
    public Map<String, List<AccessibilityIssue>> getIssuesByType() {
        return issues.stream().collect(Collectors.groupingBy(AccessibilityIssue::getType));
    }
    
    /**
     * Check if the report has issues of a specific severity or higher
     * @param severity The minimum severity to check for
     * @return True if issues of the specified severity or higher were found
     */
    public boolean hasIssuesOfSeverity(AccessibilityIssue.Severity severity) {
        return issues.stream().anyMatch(issue -> issue.getSeverity().ordinal() >= severity.ordinal());
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Accessibility Report: ").append(screenName).append("\n");
        builder.append("Generated: ").append(timestamp).append("\n");
        builder.append("Total issues: ").append(issues.size()).append("\n\n");
        
        // Group issues by severity
        Map<AccessibilityIssue.Severity, List<AccessibilityIssue>> bySeverity = getIssuesBySeverity();
        
        // Output issues by severity (highest first)
        for (AccessibilityIssue.Severity severity : AccessibilityIssue.Severity.values()) {
            List<AccessibilityIssue> severityIssues = bySeverity.get(severity);
            if (severityIssues != null && !severityIssues.isEmpty()) {
                builder.append(severity).append(" issues (").append(severityIssues.size()).append("):\n");
                severityIssues.forEach(issue -> builder.append("- ").append(issue).append("\n"));
                builder.append("\n");
            }
        }
        
        // Add summary
        if (issues.isEmpty()) {
            builder.append("No accessibility issues detected.\n");
        } else {
            builder.append("Summary: Found ")
                   .append(issues.size())
                   .append(" accessibility issues on screen ")
                   .append(screenName)
                   .append(".\n");
        }
        
        return builder.toString();
    }
}
