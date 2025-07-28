package com.securitytests.utils.security;

import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import java.util.*;

/**
 * Utility for analyzing security headers in HTTP responses
 */
public class SecurityHeadersAnalyzer {
    private static final StructuredLogger logger = new StructuredLogger(SecurityHeadersAnalyzer.class);
    
    // Required security headers for secure applications
    private static final Set<String> REQUIRED_SECURITY_HEADERS = new HashSet<>(Arrays.asList(
            "strict-transport-security",
            "x-content-type-options",
            "x-frame-options",
            "content-security-policy",
            "x-xss-protection"
    ));
    
    // Recommended security headers
    private static final Set<String> RECOMMENDED_SECURITY_HEADERS = new HashSet<>(Arrays.asList(
            "referrer-policy",
            "feature-policy",
            "permissions-policy",
            "cache-control",
            "pragma",
            "clear-site-data"
    ));
    
    // Headers that should not be present in responses
    private static final Set<String> INSECURE_HEADERS = new HashSet<>(Arrays.asList(
            "server",
            "x-powered-by",
            "x-aspnet-version",
            "x-aspnetmvc-version"
    ));
    
    /**
     * Analyze security headers in an HTTP response
     * 
     * @param response The HTTP response to analyze
     * @return SecurityHeadersReport containing analysis results
     */
    @Step("Analyze security headers")
    public SecurityHeadersReport analyzeHeaders(HttpResponse response) {
        logger.info("Analyzing security headers");
        
        if (response == null) {
            logger.error("Cannot analyze null response");
            return new SecurityHeadersReport(Collections.emptyMap(), 0);
        }
        
        Map<String, String> headersMap = new HashMap<>();
        Map<String, List<String>> issues = new HashMap<>();
        
        // Get all headers from the response
        Header[] headers = response.getAllHeaders();
        for (Header header : headers) {
            headersMap.put(header.getName().toLowerCase(), header.getValue());
        }
        
        // Check for missing required headers
        List<String> missingRequired = new ArrayList<>();
        for (String header : REQUIRED_SECURITY_HEADERS) {
            if (!headersMap.containsKey(header)) {
                missingRequired.add(header);
                logger.warn("Missing required security header: {}", header);
            }
        }
        
        if (!missingRequired.isEmpty()) {
            issues.put("Missing required security headers", missingRequired);
        }
        
        // Check for missing recommended headers
        List<String> missingRecommended = new ArrayList<>();
        for (String header : RECOMMENDED_SECURITY_HEADERS) {
            if (!headersMap.containsKey(header)) {
                missingRecommended.add(header);
                logger.info("Missing recommended security header: {}", header);
            }
        }
        
        if (!missingRecommended.isEmpty()) {
            issues.put("Missing recommended security headers", missingRecommended);
        }
        
        // Check for insecure headers
        List<String> foundInsecure = new ArrayList<>();
        for (String header : INSECURE_HEADERS) {
            if (headersMap.containsKey(header)) {
                foundInsecure.add(header);
                logger.warn("Found insecure header: {}", header);
            }
        }
        
        if (!foundInsecure.isEmpty()) {
            issues.put("Insecure headers present", foundInsecure);
        }
        
        // Check Strict-Transport-Security (HSTS)
        if (headersMap.containsKey("strict-transport-security")) {
            String hstsValue = headersMap.get("strict-transport-security");
            if (!hstsValue.contains("max-age=") || 
                    getMaxAgeValue(hstsValue) < 15768000) { // 6 months in seconds
                List<String> hstsIssues = new ArrayList<>();
                hstsIssues.add("HSTS max-age too short (should be at least 6 months)");
                issues.put("HSTS configuration issues", hstsIssues);
                logger.warn("HSTS max-age too short: {}", hstsValue);
            }
        }
        
        // Check Content-Security-Policy
        if (headersMap.containsKey("content-security-policy")) {
            String cspValue = headersMap.get("content-security-policy");
            List<String> cspIssues = analyzeCSP(cspValue);
            if (!cspIssues.isEmpty()) {
                issues.put("CSP configuration issues", cspIssues);
                for (String issue : cspIssues) {
                    logger.warn("CSP issue: {}", issue);
                }
            }
        }
        
        // Calculate security score (0-100)
        int securityScore = calculateSecurityScore(headersMap, issues);
        logger.info("Security headers analysis completed. Score: {}/100", securityScore);
        
        return new SecurityHeadersReport(headersMap, securityScore, issues);
    }
    
    /**
     * Extract max-age value from HSTS header
     */
    private long getMaxAgeValue(String hstsHeader) {
        if (hstsHeader == null) return 0;
        
        String[] parts = hstsHeader.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("max-age=")) {
                try {
                    return Long.parseLong(part.substring(8));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    /**
     * Analyze Content-Security-Policy header for security issues
     */
    private List<String> analyzeCSP(String cspValue) {
        List<String> issues = new ArrayList<>();
        
        if (cspValue == null || cspValue.isEmpty()) {
            issues.add("Empty CSP value");
            return issues;
        }
        
        // Check for unsafe CSP directives
        if (cspValue.contains("unsafe-inline")) {
            issues.add("CSP contains unsafe-inline which reduces XSS protection");
        }
        
        if (cspValue.contains("unsafe-eval")) {
            issues.add("CSP contains unsafe-eval which reduces XSS protection");
        }
        
        // Check if default-src is specified
        if (!cspValue.contains("default-src")) {
            issues.add("CSP missing default-src directive");
        }
        
        // Check if wildcard (*) sources are used
        if (cspValue.contains(" * ") || cspValue.contains(" *;")) {
            issues.add("CSP uses wildcard (*) sources which is less secure");
        }
        
        return issues;
    }
    
    /**
     * Calculate security score based on headers and issues
     */
    private int calculateSecurityScore(Map<String, String> headers, Map<String, List<String>> issues) {
        int score = 100; // Start with perfect score
        
        // Deduct for missing required headers
        List<String> missingRequired = issues.getOrDefault("Missing required security headers", Collections.emptyList());
        score -= missingRequired.size() * 15; // -15 points per missing required header
        
        // Deduct for missing recommended headers (less impact)
        List<String> missingRecommended = issues.getOrDefault("Missing recommended security headers", Collections.emptyList());
        score -= missingRecommended.size() * 5; // -5 points per missing recommended header
        
        // Deduct for insecure headers
        List<String> insecureHeaders = issues.getOrDefault("Insecure headers present", Collections.emptyList());
        score -= insecureHeaders.size() * 10; // -10 points per insecure header
        
        // Deduct for CSP issues
        List<String> cspIssues = issues.getOrDefault("CSP configuration issues", Collections.emptyList());
        score -= cspIssues.size() * 8; // -8 points per CSP issue
        
        // Deduct for HSTS issues
        List<String> hstsIssues = issues.getOrDefault("HSTS configuration issues", Collections.emptyList());
        score -= hstsIssues.size() * 10; // -10 points per HSTS issue
        
        // Ensure score stays within 0-100
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Security headers report class
     */
    public static class SecurityHeadersReport {
        private final Map<String, String> headers;
        private final int securityScore;
        private final Map<String, List<String>> issues;
        private final Date timestamp;
        
        public SecurityHeadersReport(Map<String, String> headers, int securityScore) {
            this(headers, securityScore, Collections.emptyMap());
        }
        
        public SecurityHeadersReport(Map<String, String> headers, int securityScore, 
                                   Map<String, List<String>> issues) {
            this.headers = headers;
            this.securityScore = securityScore;
            this.issues = issues;
            this.timestamp = new Date();
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public int getSecurityScore() {
            return securityScore;
        }
        
        public Map<String, List<String>> getIssues() {
            return issues;
        }
        
        public Date getTimestamp() {
            return timestamp;
        }
        
        public boolean hasIssues() {
            return !issues.isEmpty();
        }
        
        public boolean hasCriticalIssues() {
            return issues.containsKey("Missing required security headers") ||
                   !issues.getOrDefault("Missing required security headers", Collections.emptyList()).isEmpty();
        }
        
        /**
         * Get security grade (A, B, C, D, F) based on security score
         */
        public String getSecurityGrade() {
            if (securityScore >= 90) return "A";
            if (securityScore >= 80) return "B";
            if (securityScore >= 70) return "C";
            if (securityScore >= 60) return "D";
            return "F";
        }
        
        /**
         * Generate HTML report for Allure attachment
         */
        @Attachment(value = "Security Headers Report", type = "text/html")
        public String generateHtmlReport() {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><style>");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
            html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
            html.append("th, td { text-align: left; padding: 8px; border: 1px solid #ddd; }");
            html.append("th { background-color: #f2f2f2; }");
            html.append(".score { font-size: 24px; font-weight: bold; margin: 20px 0; }");
            html.append(".grade-A { color: #4CAF50; } .grade-B { color: #8BC34A; }");
            html.append(".grade-C { color: #FFC107; } .grade-D { color: #FF9800; }");
            html.append(".grade-F { color: #F44336; }");
            html.append(".issue { color: #F44336; }");
            html.append("</style></head><body>");
            
            // Header
            html.append("<h1>Security Headers Analysis</h1>");
            html.append("<p>Generated on: ").append(timestamp).append("</p>");
            
            // Security Score
            html.append("<div class='score grade-").append(getSecurityGrade()).append("'>");
            html.append("Security Grade: ").append(getSecurityGrade());
            html.append(" (").append(securityScore).append("/100)");
            html.append("</div>");
            
            // Issues
            if (hasIssues()) {
                html.append("<h2>Security Issues</h2>");
                html.append("<table>");
                html.append("<tr><th>Issue Type</th><th>Details</th></tr>");
                
                for (Map.Entry<String, List<String>> entry : issues.entrySet()) {
                    html.append("<tr>");
                    html.append("<td>").append(entry.getKey()).append("</td>");
                    html.append("<td><ul>");
                    for (String detail : entry.getValue()) {
                        html.append("<li class='issue'>").append(detail).append("</li>");
                    }
                    html.append("</ul></td>");
                    html.append("</tr>");
                }
                
                html.append("</table>");
            }
            
            // Headers
            html.append("<h2>Headers Present</h2>");
            html.append("<table>");
            html.append("<tr><th>Header Name</th><th>Value</th></tr>");
            
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                html.append("<tr>");
                html.append("<td>").append(entry.getKey()).append("</td>");
                html.append("<td>").append(entry.getValue()).append("</td>");
                html.append("</tr>");
            }
            
            html.append("</table>");
            html.append("</body></html>");
            
            return html.toString();
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Security Headers Report:\n");
            sb.append("Security Score: ").append(securityScore).append("/100 (Grade ").append(getSecurityGrade()).append(")\n");
            sb.append("Timestamp: ").append(timestamp).append("\n\n");
            
            if (hasIssues()) {
                sb.append("Issues Found:\n");
                for (Map.Entry<String, List<String>> entry : issues.entrySet()) {
                    sb.append("  ").append(entry.getKey()).append(":\n");
                    for (String detail : entry.getValue()) {
                        sb.append("    - ").append(detail).append("\n");
                    }
                }
                sb.append("\n");
            }
            
            sb.append("Headers Present:\n");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            
            return sb.toString();
        }
    }
}
