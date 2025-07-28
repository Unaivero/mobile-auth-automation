package com.securitytests.utils.accessibility;

import java.util.UUID;

/**
 * Represents an accessibility issue found during testing
 */
public class AccessibilityIssue {
    /**
     * Severity levels for accessibility issues
     */
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    private final String id;
    private final String type;
    private final String description;
    private final String details;
    private final Severity severity;
    
    /**
     * Create a new accessibility issue
     * @param type Type of accessibility issue (e.g., "contentDesc", "contrast")
     * @param description Brief description of the issue
     * @param details Detailed information about the issue
     * @param severity Severity level of the issue
     */
    public AccessibilityIssue(String type, String description, String details, Severity severity) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.description = description;
        this.details = details;
        this.severity = severity;
    }
    
    /**
     * Get the unique ID of this issue
     * @return Issue ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the type of this issue
     * @return Issue type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Get the description of this issue
     * @return Issue description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the details of this issue
     * @return Issue details
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * Get the severity of this issue
     * @return Issue severity
     */
    public Severity getSeverity() {
        return severity;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s - %s", 
            severity, type, description, details);
    }
}
