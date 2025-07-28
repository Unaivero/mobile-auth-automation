package com.securitytests.utils.reporting;

import com.securitytests.utils.config.SecurityTestConfig;
import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Allure;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates security compliance reports from test results
 * for audit and compliance documentation purposes
 */
public class SecurityComplianceReporter {
    private static final StructuredLogger logger = new StructuredLogger(SecurityComplianceReporter.class);
    
    private final String testResultsDir;
    private final String outputDir;
    private final SecurityTestConfig securityConfig;
    private final List<ComplianceRequirement> requirements;
    
    // Common security requirement categories
    private static final String[] REQUIREMENT_CATEGORIES = {
        "Authentication", "Session Management", "Access Control", "Input Validation",
        "Cryptography", "Error Handling", "Data Protection", "Communication Security"
    };
    
    /**
     * Constructor
     * 
     * @param testResultsDir Directory containing test results (Cucumber JSON reports)
     * @param outputDir Directory for generated compliance reports
     */
    public SecurityComplianceReporter(String testResultsDir, String outputDir) {
        this.testResultsDir = testResultsDir;
        this.outputDir = outputDir;
        this.securityConfig = SecurityTestConfig.getInstance();
        this.requirements = initRequirements();
        
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            logger.error("Failed to create output directory: {}", outputDir, e);
        }
    }
    
    /**
     * Main entry point for command line usage
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: SecurityComplianceReporter <test-results-dir> <output-dir>");
            System.exit(1);
        }
        
        String testResultsDir = args[0];
        String outputDir = args[1];
        
        SecurityComplianceReporter reporter = new SecurityComplianceReporter(testResultsDir, outputDir);
        reporter.generateComplianceReport();
    }
    
    /**
     * Generate compliance report from test results
     */
    public void generateComplianceReport() {
        logger.info("Generating security compliance report from {} to {}", testResultsDir, outputDir);
        
        try {
            // Parse test results
            Map<String, TestResultSummary> testResults = parseTestResults();
            
            // Map test results to compliance requirements
            mapResultsToRequirements(testResults);
            
            // Generate HTML report
            generateHtmlReport();
            
            // Generate PDF report (optional)
            // generatePdfReport();
            
            // Generate machine-readable JSON report
            generateJsonReport();
            
            logger.info("Security compliance report generated successfully");
            
            // Add to Allure if running in test context
            try {
                Allure.addAttachment("Security Compliance Report", "text/plain", 
                        "Compliance report generated at " + outputDir);
            } catch (Exception e) {
                // Ignore if Allure is not available
            }
        } catch (Exception e) {
            logger.error("Error generating compliance report", e);
            throw new RuntimeException("Failed to generate compliance report", e);
        }
    }
    
    /**
     * Initialize compliance requirements
     */
    private List<ComplianceRequirement> initRequirements() {
        List<ComplianceRequirement> reqs = new ArrayList<>();
        
        // Authentication requirements
        reqs.add(new ComplianceRequirement(
                "AUTH-01", 
                "Authentication Security",
                "Authentication mechanisms must protect against brute force and credential stuffing attacks",
                "OWASP ASVS 2.2.1, NIST 800-63B",
                Arrays.asList("authentication", "security", "severity-critical")));
        
        reqs.add(new ComplianceRequirement(
                "AUTH-02", 
                "Password Security", 
                "Password handling must enforce minimum complexity and secure storage",
                "OWASP ASVS 2.1.1, NIST 800-63B",
                Arrays.asList("authentication", "security", "severity-high")));
        
        reqs.add(new ComplianceRequirement(
                "AUTH-03", 
                "Multi-Factor Authentication", 
                "Multi-factor authentication must be available for sensitive operations",
                "OWASP ASVS 2.8.1, NIST 800-63B",
                Arrays.asList("authentication", "security", "severity-high")));
        
        // Session management requirements
        reqs.add(new ComplianceRequirement(
                "SESS-01", 
                "Session Timeout", 
                "Sessions must have a defined inactivity timeout",
                "OWASP ASVS 3.3.1",
                Arrays.asList("session", "security", "severity-critical")));
        
        reqs.add(new ComplianceRequirement(
                "SESS-02", 
                "Session Fixation", 
                "Session identifiers must change upon authentication",
                "OWASP ASVS 3.2.2",
                Arrays.asList("session", "security", "severity-critical")));
        
        reqs.add(new ComplianceRequirement(
                "SESS-03", 
                "Cookie Security", 
                "Session cookies must have secure attributes",
                "OWASP ASVS 3.4.1",
                Arrays.asList("session", "security", "severity-high")));
        
        // Token security requirements
        reqs.add(new ComplianceRequirement(
                "TOKEN-01", 
                "JWT Security", 
                "JWT tokens must be securely implemented and validated",
                "OWASP ASVS 3.5.1",
                Arrays.asList("token", "security", "severity-critical")));
        
        reqs.add(new ComplianceRequirement(
                "TOKEN-02", 
                "Token Integrity", 
                "Tokens must be protected from tampering",
                "OWASP ASVS 3.5.3",
                Arrays.asList("token", "security", "severity-high")));
        
        // Security headers
        reqs.add(new ComplianceRequirement(
                "SEC-01", 
                "Security Headers", 
                "Proper security headers must be implemented",
                "OWASP ASVS 14.4.1",
                Arrays.asList("headers", "security", "severity-medium")));
        
        return reqs;
    }
    
    /**
     * Parse test results from Cucumber JSON reports
     */
    private Map<String, TestResultSummary> parseTestResults() throws IOException {
        Map<String, TestResultSummary> results = new HashMap<>();
        
        try (Stream<Path> paths = Files.walk(Paths.get(testResultsDir))) {
            List<Path> jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .collect(Collectors.toList());
            
            logger.info("Found {} test result JSON files to process", jsonFiles.size());
            
            for (Path jsonFile : jsonFiles) {
                parseResultFile(jsonFile, results);
            }
        }
        
        return results;
    }
    
    /**
     * Parse individual test result file
     */
    private void parseResultFile(Path jsonFile, Map<String, TestResultSummary> results) throws IOException {
        String content = new String(Files.readAllBytes(jsonFile));
        
        try {
            JSONArray features = new JSONArray(content);
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                String featureName = feature.getString("name");
                
                JSONArray elements = feature.getJSONArray("elements");
                for (int j = 0; j < elements.length(); j++) {
                    JSONObject element = elements.getJSONObject(j);
                    
                    // Only process scenarios (not backgrounds)
                    if ("scenario".equals(element.getString("type"))) {
                        String scenarioName = element.getString("name");
                        
                        // Extract tags
                        List<String> tags = new ArrayList<>();
                        if (element.has("tags")) {
                            JSONArray tagsJson = element.getJSONArray("tags");
                            for (int k = 0; k < tagsJson.length(); k++) {
                                JSONObject tag = tagsJson.getJSONObject(k);
                                String tagName = tag.getString("name").substring(1); // Remove @ prefix
                                tags.add(tagName);
                            }
                        }
                        
                        // Calculate result status
                        boolean passed = true;
                        if (element.has("steps")) {
                            JSONArray steps = element.getJSONArray("steps");
                            for (int k = 0; k < steps.length(); k++) {
                                JSONObject step = steps.getJSONObject(k);
                                if (step.has("result")) {
                                    JSONObject result = step.getJSONObject("result");
                                    if (!"passed".equals(result.getString("status"))) {
                                        passed = false;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Create or update test result summary
                        String key = featureName + ": " + scenarioName;
                        TestResultSummary summary = new TestResultSummary(featureName, scenarioName, tags, passed);
                        results.put(key, summary);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing test result file: {}", jsonFile, e);
            throw new IOException("Failed to parse test result file: " + jsonFile, e);
        }
    }
    
    /**
     * Map test results to compliance requirements
     */
    private void mapResultsToRequirements(Map<String, TestResultSummary> testResults) {
        logger.info("Mapping {} test results to {} compliance requirements", testResults.size(), requirements.size());
        
        for (Map.Entry<String, TestResultSummary> entry : testResults.entrySet()) {
            TestResultSummary result = entry.getValue();
            
            // Find matching requirements based on tags
            for (ComplianceRequirement req : requirements) {
                if (tagMatch(req.getTags(), result.getTags())) {
                    req.getTestResults().add(result);
                }
            }
        }
        
        // Log mapping results
        for (ComplianceRequirement req : requirements) {
            logger.info("Requirement {}: mapped {} test results", req.getId(), req.getTestResults().size());
        }
    }
    
    /**
     * Check if there's a match between requirement tags and result tags
     */
    private boolean tagMatch(List<String> reqTags, List<String> resultTags) {
        // Simple matching strategy: if any tag matches, it's a match
        for (String reqTag : reqTags) {
            if (resultTags.contains(reqTag)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Generate HTML compliance report
     */
    private void generateHtmlReport() throws IOException {
        String reportFile = outputDir + File.separator + "compliance-report.html";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"en\">");
            writer.println("<head>");
            writer.println("    <meta charset=\"UTF-8\">");
            writer.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("    <title>Security Compliance Report</title>");
            writer.println("    <style>");
            writer.println("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 0; color: #333; background-color: #f5f5f5; }");
            writer.println("        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }");
            writer.println("        header { background-color: #2c3e50; color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; }");
            writer.println("        header h1 { margin: 0; }");
            writer.println("        .summary { background-color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); }");
            writer.println("        .requirement { background-color: white; padding: 20px; border-radius: 5px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1); }");
            writer.println("        .requirement-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }");
            writer.println("        .requirement-id { font-weight: bold; padding: 5px 10px; border-radius: 3px; background-color: #f0f0f0; }");
            writer.println("        .status { padding: 5px 10px; border-radius: 3px; color: white; font-weight: bold; }");
            writer.println("        .status-pass { background-color: #4caf50; }");
            writer.println("        .status-fail { background-color: #f44336; }");
            writer.println("        .status-partial { background-color: #ff9800; }");
            writer.println("        .status-unknown { background-color: #9e9e9e; }");
            writer.println("        table { width: 100%; border-collapse: collapse; margin-top: 10px; }");
            writer.println("        th, td { text-align: left; padding: 12px; border-bottom: 1px solid #ddd; }");
            writer.println("        th { background-color: #f2f2f2; }");
            writer.println("        tr:hover { background-color: #f5f5f5; }");
            writer.println("        .test-result { margin-top: 5px; padding: 8px 12px; border-radius: 3px; }");
            writer.println("        .test-pass { background-color: #e8f5e9; border-left: 4px solid #4caf50; }");
            writer.println("        .test-fail { background-color: #ffebee; border-left: 4px solid #f44336; }");
            writer.println("        .tag { display: inline-block; padding: 3px 6px; margin-right: 5px; border-radius: 3px; background-color: #e0e0e0; font-size: 12px; }");
            writer.println("        .chart-container { display: flex; margin-top: 20px; }");
            writer.println("        .chart { flex: 1; margin: 0 10px; }");
            writer.println("    </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("    <div class=\"container\">");
            
            // Report header
            writer.println("        <header>");
            writer.println("            <h1>Mobile Authentication Security Compliance Report</h1>");
            writer.println("            <p>Generated on " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "</p>");
            writer.println("        </header>");
            
            // Summary section
            int passedCount = 0;
            int failedCount = 0;
            int partialCount = 0;
            int unknownCount = 0;
            
            for (ComplianceRequirement req : requirements) {
                String status = getRequirementStatus(req);
                switch (status) {
                    case "PASS": passedCount++; break;
                    case "FAIL": failedCount++; break;
                    case "PARTIAL": partialCount++; break;
                    default: unknownCount++;
                }
            }
            
            writer.println("        <div class=\"summary\">");
            writer.println("            <h2>Compliance Summary</h2>");
            writer.println("            <p>Environment: " + securityConfig.getTestEnvironment() + "</p>");
            writer.println("            <p>Security Test Level: " + securityConfig.getOwaspLevel() + "</p>");
            writer.println("            <div class=\"chart-container\">");
            writer.println("                <div class=\"chart\">");
            writer.println("                    <h3>Requirement Status</h3>");
            writer.println("                    <table>");
            writer.println("                        <tr><th>Status</th><th>Count</th><th>Percentage</th></tr>");
            writer.println("                        <tr><td><span class=\"status status-pass\">PASS</span></td><td>" + passedCount + "</td><td>" + 
                    calculatePercentage(passedCount, requirements.size()) + "%</td></tr>");
            writer.println("                        <tr><td><span class=\"status status-partial\">PARTIAL</span></td><td>" + partialCount + "</td><td>" + 
                    calculatePercentage(partialCount, requirements.size()) + "%</td></tr>");
            writer.println("                        <tr><td><span class=\"status status-fail\">FAIL</span></td><td>" + failedCount + "</td><td>" + 
                    calculatePercentage(failedCount, requirements.size()) + "%</td></tr>");
            writer.println("                        <tr><td><span class=\"status status-unknown\">UNKNOWN</span></td><td>" + unknownCount + "</td><td>" + 
                    calculatePercentage(unknownCount, requirements.size()) + "%</td></tr>");
            writer.println("                    </table>");
            writer.println("                </div>");
            writer.println("                <div class=\"chart\">");
            writer.println("                    <h3>Category Coverage</h3>");
            writer.println("                    <table>");
            writer.println("                        <tr><th>Category</th><th>Status</th></tr>");
            
            for (String category : REQUIREMENT_CATEGORIES) {
                int categoryReqs = (int) requirements.stream()
                        .filter(r -> r.getId().startsWith(category.substring(0, 3).toUpperCase()))
                        .count();
                int categoryPassed = (int) requirements.stream()
                        .filter(r -> r.getId().startsWith(category.substring(0, 3).toUpperCase()) && 
                               getRequirementStatus(r).equals("PASS"))
                        .count();
                
                String categoryStatus;
                String statusClass;
                
                if (categoryReqs == 0) {
                    categoryStatus = "N/A";
                    statusClass = "status-unknown";
                } else if (categoryPassed == categoryReqs) {
                    categoryStatus = "PASS";
                    statusClass = "status-pass";
                } else if (categoryPassed == 0) {
                    categoryStatus = "FAIL";
                    statusClass = "status-fail";
                } else {
                    categoryStatus = "PARTIAL";
                    statusClass = "status-partial";
                }
                
                writer.println("                        <tr><td>" + category + "</td><td><span class=\"status " + 
                        statusClass + "\">" + categoryStatus + "</span></td></tr>");
            }
            
            writer.println("                    </table>");
            writer.println("                </div>");
            writer.println("            </div>");
            writer.println("        </div>");
            
            // Requirements detail
            writer.println("        <h2>Security Requirements</h2>");
            
            for (ComplianceRequirement req : requirements) {
                String status = getRequirementStatus(req);
                String statusClass = "status-unknown";
                
                if ("PASS".equals(status)) {
                    statusClass = "status-pass";
                } else if ("FAIL".equals(status)) {
                    statusClass = "status-fail";
                } else if ("PARTIAL".equals(status)) {
                    statusClass = "status-partial";
                }
                
                writer.println("        <div class=\"requirement\">");
                writer.println("            <div class=\"requirement-header\">");
                writer.println("                <span class=\"requirement-id\">" + req.getId() + "</span>");
                writer.println("                <span class=\"status " + statusClass + "\">" + status + "</span>");
                writer.println("            </div>");
                writer.println("            <h3>" + escapeHtml(req.getName()) + "</h3>");
                writer.println("            <p>" + escapeHtml(req.getDescription()) + "</p>");
                writer.println("            <p><em>References: " + escapeHtml(req.getReferences()) + "</em></p>");
                writer.println("            <div>");
                
                for (String tag : req.getTags()) {
                    writer.println("                <span class=\"tag\">" + escapeHtml(tag) + "</span>");
                }
                
                writer.println("            </div>");
                
                // Test results for this requirement
                writer.println("            <h4>Test Results:</h4>");
                List<TestResultSummary> testResults = req.getTestResults();
                
                if (testResults.isEmpty()) {
                    writer.println("            <p>No test results available for this requirement.</p>");
                } else {
                    for (TestResultSummary result : testResults) {
                        String resultClass = result.isPassed() ? "test-pass" : "test-fail";
                        String resultStatus = result.isPassed() ? "Pass" : "Fail";
                        
                        writer.println("            <div class=\"test-result " + resultClass + "\">");
                        writer.println("                <strong>" + escapeHtml(result.getFeatureName()) + ": " + 
                                escapeHtml(result.getScenarioName()) + "</strong> - " + resultStatus);
                        writer.println("            </div>");
                    }
                }
                
                writer.println("        </div>");
            }
            
            // Footer
            writer.println("        <div>");
            writer.println("            <p>This report provides a summary of security compliance based on automated test results.</p>");
            writer.println("            <p>For detailed test evidence, refer to Allure test reports.</p>");
            writer.println("        </div>");
            
            writer.println("    </div>");
            writer.println("</body>");
            writer.println("</html>");
        }
        
        logger.info("HTML compliance report generated: {}", reportFile);
    }
    
    /**
     * Generate JSON compliance report for machine consumption
     */
    private void generateJsonReport() throws IOException {
        String reportFile = outputDir + File.separator + "compliance-report.json";
        
        JSONObject report = new JSONObject();
        report.put("generatedDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date()));
        report.put("environment", securityConfig.getTestEnvironment());
        report.put("securityLevel", securityConfig.getOwaspLevel());
        
        // Add summary
        JSONObject summary = new JSONObject();
        int passedCount = 0;
        int failedCount = 0;
        int partialCount = 0;
        int unknownCount = 0;
        
        for (ComplianceRequirement req : requirements) {
            String status = getRequirementStatus(req);
            switch (status) {
                case "PASS": passedCount++; break;
                case "FAIL": failedCount++; break;
                case "PARTIAL": partialCount++; break;
                default: unknownCount++;
            }
        }
        
        summary.put("total", requirements.size());
        summary.put("passed", passedCount);
        summary.put("failed", failedCount);
        summary.put("partial", partialCount);
        summary.put("unknown", unknownCount);
        
        report.put("summary", summary);
        
        // Add requirements
        JSONArray requirementsJson = new JSONArray();
        
        for (ComplianceRequirement req : requirements) {
            JSONObject reqJson = new JSONObject();
            reqJson.put("id", req.getId());
            reqJson.put("name", req.getName());
            reqJson.put("description", req.getDescription());
            reqJson.put("references", req.getReferences());
            reqJson.put("status", getRequirementStatus(req));
            
            // Add tags
            JSONArray tagsJson = new JSONArray();
            for (String tag : req.getTags()) {
                tagsJson.put(tag);
            }
            reqJson.put("tags", tagsJson);
            
            // Add test results
            JSONArray resultsJson = new JSONArray();
            for (TestResultSummary result : req.getTestResults()) {
                JSONObject resultJson = new JSONObject();
                resultJson.put("feature", result.getFeatureName());
                resultJson.put("scenario", result.getScenarioName());
                resultJson.put("passed", result.isPassed());
                
                // Add tags
                JSONArray resultTagsJson = new JSONArray();
                for (String tag : result.getTags()) {
                    resultTagsJson.put(tag);
                }
                resultJson.put("tags", resultTagsJson);
                
                resultsJson.put(resultJson);
            }
            reqJson.put("testResults", resultsJson);
            
            requirementsJson.put(reqJson);
        }
        
        report.put("requirements", requirementsJson);
        
        // Write to file
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(report.toString(4)); // Pretty print with 4-space indent
        }
        
        logger.info("JSON compliance report generated: {}", reportFile);
    }
    
    /**
     * Determine compliance status for a requirement
     */
    private String getRequirementStatus(ComplianceRequirement req) {
        List<TestResultSummary> testResults = req.getTestResults();
        
        if (testResults.isEmpty()) {
            return "UNKNOWN";
        }
        
        boolean anyFailed = false;
        boolean anyPassed = false;
        
        for (TestResultSummary result : testResults) {
            if (result.isPassed()) {
                anyPassed = true;
            } else {
                anyFailed = true;
            }
        }
        
        if (anyPassed && !anyFailed) {
            return "PASS";
        } else if (anyPassed && anyFailed) {
            return "PARTIAL";
        } else {
            return "FAIL";
        }
    }
    
    /**
     * Calculate percentage
     */
    private int calculatePercentage(int part, int total) {
        if (total == 0) return 0;
        return (int) Math.round((double) part / total * 100);
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
    
    /**
     * Compliance requirement data class
     */
    private static class ComplianceRequirement {
        private final String id;
        private final String name;
        private final String description;
        private final String references;
        private final List<String> tags;
        private final List<TestResultSummary> testResults;
        
        public ComplianceRequirement(String id, String name, String description, String references, List<String> tags) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.references = references;
            this.tags = tags;
            this.testResults = new ArrayList<>();
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getReferences() { return references; }
        public List<String> getTags() { return tags; }
        public List<TestResultSummary> getTestResults() { return testResults; }
    }
    
    /**
     * Test result summary data class
     */
    private static class TestResultSummary {
        private final String featureName;
        private final String scenarioName;
        private final List<String> tags;
        private final boolean passed;
        
        public TestResultSummary(String featureName, String scenarioName, List<String> tags, boolean passed) {
            this.featureName = featureName;
            this.scenarioName = scenarioName;
            this.tags = tags;
            this.passed = passed;
        }
        
        public String getFeatureName() { return featureName; }
        public String getScenarioName() { return scenarioName; }
        public List<String> getTags() { return tags; }
        public boolean isPassed() { return passed; }
    }
}
