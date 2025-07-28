package com.securitytests.utils.security;

import org.zaproxy.clientapi.core.ApiResponseSet;
import java.util.UUID;

/**
 * Represents a security alert detected during a security scan
 */
public class SecurityAlert {
    private final String id;
    private final String name;
    private final String description;
    private final AlertRiskLevel riskLevel;
    private final AlertConfidence confidence;
    private final String url;
    private final String method;
    private final String param;
    private final String evidence;
    private final String solution;
    private final String reference;
    private final String cweId;
    private final String wascId;
    
    /**
     * Create a security alert from ZAP API response
     * @param alertItem ZAP API response set containing alert data
     */
    public SecurityAlert(ApiResponseSet<?> alertItem) {
        this.id = UUID.randomUUID().toString();
        this.name = alertItem.getStringValue("name");
        this.description = alertItem.getStringValue("description");
        this.riskLevel = AlertRiskLevel.fromString(alertItem.getStringValue("risk"));
        this.confidence = AlertConfidence.fromString(alertItem.getStringValue("confidence"));
        this.url = alertItem.getStringValue("url");
        this.method = alertItem.getStringValue("method");
        this.param = alertItem.getStringValue("param");
        this.evidence = alertItem.getStringValue("evidence");
        this.solution = alertItem.getStringValue("solution");
        this.reference = alertItem.getStringValue("reference");
        this.cweId = alertItem.getStringValue("cweid");
        this.wascId = alertItem.getStringValue("wascid");
    }
    
    /**
     * Create a security alert manually
     * @param name Alert name
     * @param description Alert description
     * @param riskLevel Alert risk level
     * @param confidence Alert confidence
     * @param url Affected URL
     * @param method HTTP method
     * @param param Affected parameter
     * @param evidence Evidence found
     * @param solution Suggested solution
     * @param reference Reference links
     * @param cweId CWE ID
     * @param wascId WASC ID
     */
    public SecurityAlert(String name, String description, AlertRiskLevel riskLevel,
                      AlertConfidence confidence, String url, String method,
                      String param, String evidence, String solution,
                      String reference, String cweId, String wascId) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.riskLevel = riskLevel;
        this.confidence = confidence;
        this.url = url;
        this.method = method;
        this.param = param;
        this.evidence = evidence;
        this.solution = solution;
        this.reference = reference;
        this.cweId = cweId;
        this.wascId = wascId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AlertRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public AlertConfidence getConfidence() {
        return confidence;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getParam() {
        return param;
    }

    public String getEvidence() {
        return evidence;
    }

    public String getSolution() {
        return solution;
    }

    public String getReference() {
        return reference;
    }

    public String getCweId() {
        return cweId;
    }

    public String getWascId() {
        return wascId;
    }
    
    /**
     * Check if this is a high risk alert
     * @return true if high or critical risk
     */
    public boolean isHighRisk() {
        return riskLevel == AlertRiskLevel.HIGH || riskLevel == AlertRiskLevel.CRITICAL;
    }
    
    /**
     * Check if this is a medium risk alert
     * @return true if medium risk
     */
    public boolean isMediumRisk() {
        return riskLevel == AlertRiskLevel.MEDIUM;
    }
    
    /**
     * Check if this alert has high confidence
     * @return true if high or confirmed confidence
     */
    public boolean isHighConfidence() {
        return confidence == AlertConfidence.HIGH || confidence == AlertConfidence.CONFIRMED;
    }
    
    /**
     * Get a short summary of the alert
     * @return Alert summary string
     */
    public String getSummary() {
        return String.format("[%s] %s (%s confidence) - %s", 
                riskLevel, name, confidence, url);
    }
    
    @Override
    public String toString() {
        return String.format("SecurityAlert{id='%s', name='%s', riskLevel=%s, confidence=%s, url='%s'}",
                id, name, riskLevel, confidence, url);
    }
}
