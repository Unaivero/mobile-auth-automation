package com.securitytests.utils.docs;

import com.securitytests.utils.config.SecurityTestConfig;
import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Allure;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts BDD feature files into human-readable HTML documentation
 * for stakeholder communication and living documentation.
 */
public class LivingDocumentationGenerator {
    private static final StructuredLogger logger = new StructuredLogger(LivingDocumentationGenerator.class);
    
    // Patterns for parsing feature files
    private static final Pattern FEATURE_PATTERN = Pattern.compile("Feature:\\s*(.+)");
    private static final Pattern SCENARIO_PATTERN = Pattern.compile("\\s*Scenario(?:| Outline):\\s*(.+)");
    private static final Pattern TAG_PATTERN = Pattern.compile("\\s*@([\\w-]+)");
    
    private String sourceDir;
    private String outputDir;
    private Map<String, String> tagColors;
    
    /**
     * Main entry point for command line usage
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: LivingDocumentationGenerator <feature-directory> <output-directory>");
            System.exit(1);
        }
        
        String featureDir = args[0];
        String outputDir = args[1];
        
        LivingDocumentationGenerator generator = new LivingDocumentationGenerator(featureDir, outputDir);
        generator.generateDocumentation();
    }
    
    /**
     * Constructor
     * 
     * @param sourceDir Directory containing feature files
     * @param outputDir Directory for generated documentation
     */
    public LivingDocumentationGenerator(String sourceDir, String outputDir) {
        this.sourceDir = sourceDir;
        this.outputDir = outputDir;
        
        // Initialize tag colors for styling documentation
        this.tagColors = new HashMap<>();
        tagColors.put("security", "#e53935"); // Red
        tagColors.put("authentication", "#1e88e5"); // Blue
        tagColors.put("session", "#43a047"); // Green
        tagColors.put("token", "#fb8c00"); // Orange
        tagColors.put("headers", "#8e24aa"); // Purple
        tagColors.put("severity-critical", "#d50000"); // Bright red
        tagColors.put("severity-high", "#ff6d00"); // Bright orange
        tagColors.put("severity-medium", "#ffab00"); // Amber
        tagColors.put("severity-low", "#558b2f"); // Light green
    }
    
    /**
     * Generate documentation from feature files
     */
    public void generateDocumentation() {
        logger.info("Generating living documentation from {} to {}", sourceDir, outputDir);
        
        try {
            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputDir));
            
            // Copy CSS and JS resources
            createResources();
            
            // Process feature files and generate HTML docs
            List<FeatureDoc> features = processFeatureFiles();
            generateHtml(features);
            
            // Generate index file
            generateIndex(features);
            
            logger.info("Living documentation generated successfully in {}", outputDir);
            
            // Add to Allure if running in test context
            try {
                Allure.addAttachment("Living Documentation Generated", "text/plain", 
                        "Documentation for " + features.size() + " features generated at " + outputDir);
            } catch (Exception e) {
                // Ignore if Allure is not available
            }
        } catch (IOException e) {
            logger.error("Error generating living documentation", e);
            throw new RuntimeException("Failed to generate living documentation", e);
        }
    }
    
    /**
     * Process all feature files and convert to documentation model
     */
    private List<FeatureDoc> processFeatureFiles() throws IOException {
        List<FeatureDoc> features = new ArrayList<>();
        
        // Find all feature files in source directory
        try (Stream<Path> paths = Files.walk(Paths.get(sourceDir))) {
            List<Path> featureFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".feature"))
                    .collect(Collectors.toList());
            
            logger.info("Found {} feature files to process", featureFiles.size());
            
            for (Path featureFile : featureFiles) {
                FeatureDoc feature = processFeatureFile(featureFile);
                if (feature != null) {
                    features.add(feature);
                }
            }
        }
        
        return features;
    }
    
    /**
     * Process a single feature file
     */
    private FeatureDoc processFeatureFile(Path featureFile) throws IOException {
        logger.info("Processing feature file: {}", featureFile);
        
        List<String> lines = Files.readAllLines(featureFile);
        if (lines.isEmpty()) {
            return null;
        }
        
        FeatureDoc feature = new FeatureDoc();
        feature.setFilePath(featureFile.toString());
        feature.setFileName(featureFile.getFileName().toString());
        
        // Process feature name and description
        List<String> featureDescription = new ArrayList<>();
        List<String> scenarioLines = new ArrayList<>();
        List<String> currentTags = new ArrayList<>();
        ScenarioDoc currentScenario = null;
        
        boolean inFeatureDesc = false;
        
        for (String line : lines) {
            // Process feature name
            Matcher featureMatcher = FEATURE_PATTERN.matcher(line);
            if (featureMatcher.find()) {
                feature.setName(featureMatcher.group(1).trim());
                inFeatureDesc = true;
                continue;
            }
            
            // Process tags
            Matcher tagMatcher = TAG_PATTERN.matcher(line);
            if (tagMatcher.find()) {
                String tag = tagMatcher.group(1).trim();
                currentTags.add(tag);
                continue;
            }
            
            // Process scenario
            Matcher scenarioMatcher = SCENARIO_PATTERN.matcher(line);
            if (scenarioMatcher.find()) {
                // If we were processing a scenario, save it before starting a new one
                if (currentScenario != null && !scenarioLines.isEmpty()) {
                    currentScenario.setContent(String.join("\n", scenarioLines));
                    feature.getScenarios().add(currentScenario);
                    scenarioLines.clear();
                }
                
                // Start new scenario
                currentScenario = new ScenarioDoc();
                currentScenario.setName(scenarioMatcher.group(1).trim());
                currentScenario.getTags().addAll(currentTags);
                currentTags.clear();
                inFeatureDesc = false;
                continue;
            }
            
            // Add line to current section
            if (inFeatureDesc) {
                if (!line.trim().isEmpty()) {
                    featureDescription.add(line.trim());
                }
            } else if (currentScenario != null) {
                scenarioLines.add(line);
            }
        }
        
        // Save the last scenario if any
        if (currentScenario != null && !scenarioLines.isEmpty()) {
            currentScenario.setContent(String.join("\n", scenarioLines));
            feature.getScenarios().add(currentScenario);
        }
        
        // Set feature description
        if (!featureDescription.isEmpty()) {
            feature.setDescription(String.join("\n", featureDescription));
        }
        
        return feature;
    }
    
    /**
     * Generate HTML files for each feature
     */
    private void generateHtml(List<FeatureDoc> features) throws IOException {
        for (FeatureDoc feature : features) {
            String outputFile = outputDir + File.separator + 
                    feature.getFileName().replace(".feature", ".html");
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.println("<!DOCTYPE html>");
                writer.println("<html lang=\"en\">");
                writer.println("<head>");
                writer.println("    <meta charset=\"UTF-8\">");
                writer.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
                writer.println("    <title>" + escapeHtml(feature.getName()) + " - Security Test Documentation</title>");
                writer.println("    <link rel=\"stylesheet\" href=\"styles.css\">");
                writer.println("</head>");
                writer.println("<body>");
                writer.println("    <div class=\"container\">");
                writer.println("        <header>");
                writer.println("            <h1>" + escapeHtml(feature.getName()) + "</h1>");
                writer.println("            <p class=\"feature-path\">Source: " + escapeHtml(feature.getFilePath()) + "</p>");
                writer.println("            <div class=\"feature-description\">");
                writer.println("                <p>" + escapeHtml(feature.getDescription()).replace("\n", "<br>") + "</p>");
                writer.println("            </div>");
                writer.println("        </header>");
                
                writer.println("        <div class=\"scenarios\">");
                for (ScenarioDoc scenario : feature.getScenarios()) {
                    writer.println("            <div class=\"scenario\">");
                    
                    // Generate tags
                    if (!scenario.getTags().isEmpty()) {
                        writer.println("                <div class=\"tags\">");
                        for (String tag : scenario.getTags()) {
                            String color = tagColors.getOrDefault(tag, "#9e9e9e");
                            writer.println("                    <span class=\"tag\" style=\"background-color: " + 
                                    color + ";\">" + escapeHtml(tag) + "</span>");
                        }
                        writer.println("                </div>");
                    }
                    
                    writer.println("                <h2>" + escapeHtml(scenario.getName()) + "</h2>");
                    
                    // Format scenario content with syntax highlighting
                    writer.println("                <div class=\"scenario-content\">");
                    writer.println("                    <pre><code>" + formatGherkin(scenario.getContent()) + "</code></pre>");
                    writer.println("                </div>");
                    writer.println("            </div>");
                }
                writer.println("        </div>");
                
                writer.println("        <footer>");
                writer.println("            <p>Generated by LivingDocumentationGenerator on " + new Date() + "</p>");
                writer.println("            <p><a href=\"index.html\">Back to Index</a></p>");
                writer.println("        </footer>");
                writer.println("    </div>");
                writer.println("    <script src=\"script.js\"></script>");
                writer.println("</body>");
                writer.println("</html>");
            }
            
            logger.info("Generated documentation for feature: {}", feature.getName());
        }
    }
    
    /**
     * Generate index file listing all features
     */
    private void generateIndex(List<FeatureDoc> features) throws IOException {
        String indexFile = outputDir + File.separator + "index.html";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(indexFile))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html lang=\"en\">");
            writer.println("<head>");
            writer.println("    <meta charset=\"UTF-8\">");
            writer.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            writer.println("    <title>Security Test Documentation</title>");
            writer.println("    <link rel=\"stylesheet\" href=\"styles.css\">");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("    <div class=\"container\">");
            writer.println("        <header>");
            writer.println("            <h1>Mobile Authentication Security Tests</h1>");
            writer.println("            <p>Living Documentation for Security Test Scenarios</p>");
            writer.println("        </header>");
            
            writer.println("        <div class=\"feature-list\">");
            writer.println("            <h2>Available Feature Documentation</h2>");
            writer.println("            <ul>");
            
            for (FeatureDoc feature : features) {
                String htmlFile = feature.getFileName().replace(".feature", ".html");
                writer.println("                <li><a href=\"" + htmlFile + "\">" + 
                        escapeHtml(feature.getName()) + "</a> (" + 
                        feature.getScenarios().size() + " scenarios)</li>");
            }
            
            writer.println("            </ul>");
            writer.println("        </div>");
            
            // Add statistics section
            writer.println("        <div class=\"statistics\">");
            writer.println("            <h2>Test Coverage Statistics</h2>");
            
            Map<String, Integer> tagCounts = new HashMap<>();
            for (FeatureDoc feature : features) {
                for (ScenarioDoc scenario : feature.getScenarios()) {
                    for (String tag : scenario.getTags()) {
                        tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + 1);
                    }
                }
            }
            
            writer.println("            <div class=\"stat-box\">");
            writer.println("                <div class=\"stat-item\">");
            writer.println("                    <div class=\"stat-value\">" + features.size() + "</div>");
            writer.println("                    <div class=\"stat-label\">Features</div>");
            writer.println("                </div>");
            
            int totalScenarios = features.stream()
                    .mapToInt(f -> f.getScenarios().size())
                    .sum();
            writer.println("                <div class=\"stat-item\">");
            writer.println("                    <div class=\"stat-value\">" + totalScenarios + "</div>");
            writer.println("                    <div class=\"stat-label\">Scenarios</div>");
            writer.println("                </div>");
            
            // Critical and high severity counts
            int criticalCount = tagCounts.getOrDefault("severity-critical", 0);
            int highCount = tagCounts.getOrDefault("severity-high", 0);
            
            writer.println("                <div class=\"stat-item\">");
            writer.println("                    <div class=\"stat-value\">" + criticalCount + "</div>");
            writer.println("                    <div class=\"stat-label\">Critical Tests</div>");
            writer.println("                </div>");
            
            writer.println("                <div class=\"stat-item\">");
            writer.println("                    <div class=\"stat-value\">" + highCount + "</div>");
            writer.println("                    <div class=\"stat-label\">High Tests</div>");
            writer.println("                </div>");
            writer.println("            </div>");
            
            // Add tag distribution chart
            writer.println("            <div class=\"tag-stats\">");
            writer.println("                <h3>Test Tag Distribution</h3>");
            writer.println("                <div class=\"tag-chart\">");
            
            List<Map.Entry<String, Integer>> sortedTags = tagCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());
            
            for (Map.Entry<String, Integer> entry : sortedTags) {
                String tag = entry.getKey();
                int count = entry.getValue();
                String color = tagColors.getOrDefault(tag, "#9e9e9e");
                
                writer.println("                    <div class=\"tag-bar\">");
                writer.println("                        <div class=\"tag-label\">" + escapeHtml(tag) + "</div>");
                writer.println("                        <div class=\"tag-bar-outer\">");
                int percentage = (count * 100) / totalScenarios;
                writer.println("                            <div class=\"tag-bar-inner\" style=\"width: " + 
                        percentage + "%; background-color: " + color + ";\">");
                writer.println("                                <span class=\"tag-count\">" + count + "</span>");
                writer.println("                            </div>");
                writer.println("                        </div>");
                writer.println("                    </div>");
            }
            
            writer.println("                </div>");
            writer.println("            </div>");
            writer.println("        </div>");
            
            writer.println("        <footer>");
            writer.println("            <p>Generated by LivingDocumentationGenerator on " + new Date() + "</p>");
            writer.println("        </footer>");
            writer.println("    </div>");
            writer.println("    <script src=\"script.js\"></script>");
            writer.println("</body>");
            writer.println("</html>");
        }
        
        logger.info("Generated index page listing {} features", features.size());
    }
    
    /**
     * Create CSS and JS resources for documentation styling
     */
    private void createResources() throws IOException {
        // Create CSS file
        String cssFile = outputDir + File.separator + "styles.css";
        try (PrintWriter writer = new PrintWriter(new FileWriter(cssFile))) {
            writer.println("/* Generated Styles for Living Documentation */");
            writer.println("body {");
            writer.println("    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;");
            writer.println("    line-height: 1.6;");
            writer.println("    color: #333;");
            writer.println("    margin: 0;");
            writer.println("    padding: 0;");
            writer.println("    background-color: #f5f5f5;");
            writer.println("}");
            writer.println(".container {");
            writer.println("    max-width: 1200px;");
            writer.println("    margin: 0 auto;");
            writer.println("    padding: 20px;");
            writer.println("}");
            writer.println("header {");
            writer.println("    background-color: #2c3e50;");
            writer.println("    color: white;");
            writer.println("    padding: 20px;");
            writer.println("    border-radius: 5px;");
            writer.println("    margin-bottom: 20px;");
            writer.println("}");
            writer.println("header h1 {");
            writer.println("    margin: 0;");
            writer.println("}");
            writer.println(".feature-path {");
            writer.println("    color: #ccc;");
            writer.println("    font-style: italic;");
            writer.println("}");
            writer.println(".feature-description {");
            writer.println("    margin-top: 10px;");
            writer.println("    padding-top: 10px;");
            writer.println("    border-top: 1px solid rgba(255, 255, 255, 0.2);");
            writer.println("}");
            writer.println(".scenario {");
            writer.println("    background-color: white;");
            writer.println("    border-radius: 5px;");
            writer.println("    margin-bottom: 20px;");
            writer.println("    padding: 20px;");
            writer.println("    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);");
            writer.println("}");
            writer.println(".tags {");
            writer.println("    margin-bottom: 10px;");
            writer.println("}");
            writer.println(".tag {");
            writer.println("    display: inline-block;");
            writer.println("    padding: 4px 8px;");
            writer.println("    margin-right: 5px;");
            writer.println("    margin-bottom: 5px;");
            writer.println("    border-radius: 3px;");
            writer.println("    color: white;");
            writer.println("    font-size: 12px;");
            writer.println("    font-weight: bold;");
            writer.println("}");
            writer.println(".scenario-content {");
            writer.println("    background-color: #f8f8f8;");
            writer.println("    border-radius: 5px;");
            writer.println("    padding: 15px;");
            writer.println("    margin-top: 10px;");
            writer.println("    font-family: monospace;");
            writer.println("    white-space: pre-wrap;");
            writer.println("}");
            writer.println(".feature-list ul {");
            writer.println("    list-style-type: none;");
            writer.println("    padding: 0;");
            writer.println("}");
            writer.println(".feature-list li {");
            writer.println("    padding: 15px;");
            writer.println("    margin-bottom: 10px;");
            writer.println("    background-color: white;");
            writer.println("    border-radius: 5px;");
            writer.println("    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);");
            writer.println("}");
            writer.println("footer {");
            writer.println("    margin-top: 30px;");
            writer.println("    padding-top: 20px;");
            writer.println("    border-top: 1px solid #eee;");
            writer.println("    text-align: center;");
            writer.println("    color: #777;");
            writer.println("}");
            writer.println(".statistics {");
            writer.println("    margin: 30px 0;");
            writer.println("    padding: 20px;");
            writer.println("    background-color: white;");
            writer.println("    border-radius: 5px;");
            writer.println("    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);");
            writer.println("}");
            writer.println(".stat-box {");
            writer.println("    display: flex;");
            writer.println("    justify-content: space-around;");
            writer.println("    flex-wrap: wrap;");
            writer.println("    margin: 20px 0;");
            writer.println("}");
            writer.println(".stat-item {");
            writer.println("    text-align: center;");
            writer.println("    padding: 15px;");
            writer.println("    min-width: 100px;");
            writer.println("}");
            writer.println(".stat-value {");
            writer.println("    font-size: 36px;");
            writer.println("    font-weight: bold;");
            writer.println("    color: #2c3e50;");
            writer.println("}");
            writer.println(".stat-label {");
            writer.println("    margin-top: 5px;");
            writer.println("    color: #666;");
            writer.println("}");
            writer.println(".tag-chart {");
            writer.println("    margin-top: 20px;");
            writer.println("}");
            writer.println(".tag-bar {");
            writer.println("    display: flex;");
            writer.println("    margin-bottom: 10px;");
            writer.println("    align-items: center;");
            writer.println("}");
            writer.println(".tag-label {");
            writer.println("    width: 150px;");
            writer.println("    text-align: right;");
            writer.println("    padding-right: 10px;");
            writer.println("    font-weight: bold;");
            writer.println("}");
            writer.println(".tag-bar-outer {");
            writer.println("    flex-grow: 1;");
            writer.println("    height: 30px;");
            writer.println("    background-color: #f0f0f0;");
            writer.println("    border-radius: 5px;");
            writer.println("}");
            writer.println(".tag-bar-inner {");
            writer.println("    height: 100%;");
            writer.println("    border-radius: 5px;");
            writer.println("    display: flex;");
            writer.println("    align-items: center;");
            writer.println("    padding-left: 10px;");
            writer.println("    color: white;");
            writer.println("    font-weight: bold;");
            writer.println("    min-width: 30px;");
            writer.println("}");
            writer.println(".keyword { color: #07a; font-weight: bold; }");
            writer.println(".comment { color: #998; font-style: italic; }");
            writer.println(".string { color: #d14; }");
            writer.println(".step { color: #0086b3; }");
        }
        
        // Create JS file
        String jsFile = outputDir + File.separator + "script.js";
        try (PrintWriter writer = new PrintWriter(new FileWriter(jsFile))) {
            writer.println("// JavaScript for Living Documentation");
            writer.println("document.addEventListener('DOMContentLoaded', function() {");
            writer.println("    // Add any interactive functionality here");
            writer.println("});");
        }
    }
    
    /**
     * Format Gherkin syntax with highlighting
     */
    private String formatGherkin(String content) {
        String formatted = escapeHtml(content);
        
        // Highlight keywords
        formatted = formatted.replaceAll("(?m)^\\s*(Given|When|Then|And|But)\\s", 
                "<span class=\"keyword\">$1</span> <span class=\"step\">");
        formatted = formatted.replaceAll("(?m)(\\|.+\\|)", "<span class=\"string\">$1</span>");
        
        // Close any opened span tags at the end of each line
        formatted = formatted.replaceAll("(?m)$", "</span>");
        
        return formatted;
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
     * Data class for feature documentation
     */
    private static class FeatureDoc {
        private String name;
        private String description;
        private String fileName;
        private String filePath;
        private List<ScenarioDoc> scenarios;
        
        public FeatureDoc() {
            scenarios = new ArrayList<>();
            description = "";
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public List<ScenarioDoc> getScenarios() { return scenarios; }
    }
    
    /**
     * Data class for scenario documentation
     */
    private static class ScenarioDoc {
        private String name;
        private String content;
        private List<String> tags;
        
        public ScenarioDoc() {
            tags = new ArrayList<>();
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public List<String> getTags() { return tags; }
    }
}
