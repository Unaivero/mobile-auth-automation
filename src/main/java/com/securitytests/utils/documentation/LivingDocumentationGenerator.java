package com.securitytests.utils.documentation;

import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Allure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates living documentation from Gherkin feature files
 */
public class LivingDocumentationGenerator {
    private static final StructuredLogger logger = new StructuredLogger(LivingDocumentationGenerator.class);
    
    private final String featuresDir;
    private final String outputDir;
    
    /**
     * Constructor
     * 
     * @param featuresDir Directory containing feature files
     * @param outputDir Directory to output documentation
     */
    public LivingDocumentationGenerator(String featuresDir, String outputDir) {
        this.featuresDir = featuresDir;
        this.outputDir = outputDir;
    }
    
    /**
     * Generate living documentation from feature files
     */
    public void generateDocumentation() {
        logger.info("Generating living documentation from feature files");
        Allure.step("Generating living documentation");
        
        try {
            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputDir));
            
            // Process all feature files
            List<FeatureFile> features = parseFeatureFiles();
            
            // Generate documentation files
            generateIndexPage(features);
            features.forEach(this::generateFeaturePage);
            generateCssFile();
            
            logger.info("Living documentation generated successfully in {}", outputDir);
            Allure.addAttachment("Living Documentation", "text/plain", "Generated in: " + outputDir);
        } catch (IOException e) {
            logger.error("Failed to generate living documentation", e);
            Allure.addAttachment("Documentation Generation Error", "text/plain", e.toString());
        }
    }
    
    /**
     * Parse all feature files in the features directory
     * 
     * @return List of parsed feature files
     * @throws IOException If error reading files
     */
    private List<FeatureFile> parseFeatureFiles() throws IOException {
        List<FeatureFile> features = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(Paths.get(featuresDir))) {
            List<Path> featurePaths = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".feature"))
                    .collect(Collectors.toList());
            
            for (Path path : featurePaths) {
                FeatureFile feature = parseFeatureFile(path);
                features.add(feature);
            }
        }
        
        return features;
    }
    
    /**
     * Parse a single feature file
     * 
     * @param path Path to feature file
     * @return Parsed feature file
     * @throws IOException If error reading file
     */
    private FeatureFile parseFeatureFile(Path path) throws IOException {
        String content = Files.readString(path);
        
        // Extract feature title and description
        Pattern featurePattern = Pattern.compile("Feature:\\s*(.+?)\\s*(?:\n|$)((?:(?!\\s*Scenario:|\\s*Background:).+\\s*)+)?");
        Matcher featureMatcher = featurePattern.matcher(content);
        
        String title = path.getFileName().toString().replace(".feature", "");
        String description = "";
        
        if (featureMatcher.find()) {
            title = featureMatcher.group(1).trim();
            if (featureMatcher.group(2) != null) {
                description = featureMatcher.group(2).trim();
            }
        }
        
        // Extract scenarios
        List<Scenario> scenarios = new ArrayList<>();
        Pattern scenarioPattern = Pattern.compile("\\s*(Scenario Outline:|Scenario:)\\s*(.+?)\\s*(?:\n|$)((?:(?!\\s*Scenario:|\\s*Examples:).+\\s*)+)(?:\\s*Examples:\\s*(?:\n|$)((?:(?!\\s*Scenario:).+\\s*)+))?");
        Matcher scenarioMatcher = scenarioPattern.matcher(content);
        
        while (scenarioMatcher.find()) {
            boolean isOutline = "Scenario Outline:".equals(scenarioMatcher.group(1).trim());
            String scenarioTitle = scenarioMatcher.group(2).trim();
            String scenarioSteps = scenarioMatcher.group(3).trim();
            String examples = scenarioMatcher.group(4);
            
            Scenario scenario = new Scenario(scenarioTitle, scenarioSteps, isOutline);
            if (examples != null) {
                scenario.setExamples(examples.trim());
            }
            
            // Extract tags
            Pattern tagPattern = Pattern.compile("(@\\w+)(?:\\s+|$)");
            Matcher tagMatcher = tagPattern.matcher(content.substring(0, scenarioMatcher.start()));
            while (tagMatcher.find()) {
                scenario.addTag(tagMatcher.group(1));
            }
            
            scenarios.add(scenario);
        }
        
        String relativePath = featuresDir.isEmpty() ? 
                path.toString() : 
                path.toString().substring(featuresDir.length());
                
        return new FeatureFile(title, description, relativePath.replace("\\", "/"), scenarios);
    }
    
    /**
     * Generate main index page
     * 
     * @param features List of features to include
     * @throws IOException If error writing file
     */
    private void generateIndexPage(List<FeatureFile> features) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>Mobile Auth Security Test Documentation</title>\n")
            .append("    <link rel=\"stylesheet\" href=\"styles.css\">\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <header>\n")
            .append("        <h1>Mobile Auth Security Test Documentation</h1>\n")
            .append("        <p>Living documentation generated from BDD feature files</p>\n")
            .append("    </header>\n")
            .append("    <div class=\"container\">\n")
            .append("        <section class=\"features-list\">\n")
            .append("            <h2>Features</h2>\n")
            .append("            <ul>\n");
            
        // Add feature links
        Map<String, List<FeatureFile>> featuresByDir = groupFeaturesByDirectory(features);
        for (Map.Entry<String, List<FeatureFile>> entry : featuresByDir.entrySet()) {
            String directory = entry.getKey();
            if (!directory.isEmpty()) {
                html.append("                <li class=\"feature-group\">\n")
                    .append("                    <h3>").append(directory).append("</h3>\n")
                    .append("                    <ul>\n");
                    
                for (FeatureFile feature : entry.getValue()) {
                    String htmlFilename = getHtmlFilename(feature.getPath());
                    html.append("                        <li><a href=\"").append(htmlFilename).append("\">")
                        .append(feature.getTitle()).append("</a></li>\n");
                }
                
                html.append("                    </ul>\n")
                    .append("                </li>\n");
            } else {
                for (FeatureFile feature : entry.getValue()) {
                    String htmlFilename = getHtmlFilename(feature.getPath());
                    html.append("                <li><a href=\"").append(htmlFilename).append("\">")
                        .append(feature.getTitle()).append("</a></li>\n");
                }
            }
        }
        
        html.append("            </ul>\n")
            .append("        </section>\n")
            .append("        <section class=\"summary\">\n")
            .append("            <h2>Summary</h2>\n")
            .append("            <p>Total Features: ").append(features.size()).append("</p>\n")
            .append("            <p>Total Scenarios: ").append(features.stream().mapToInt(f -> f.getScenarios().size()).sum()).append("</p>\n")
            .append("        </section>\n")
            .append("    </div>\n")
            .append("    <footer>\n")
            .append("        <p>Generated on: ").append(java.time.LocalDateTime.now()).append("</p>\n")
            .append("    </footer>\n")
            .append("</body>\n")
            .append("</html>");
        
        try (FileWriter writer = new FileWriter(new File(outputDir, "index.html"))) {
            writer.write(html.toString());
        }
    }
    
    /**
     * Group features by directory for better organization
     * 
     * @param features List of features
     * @return Map of directory names to features
     */
    private Map<String, List<FeatureFile>> groupFeaturesByDirectory(List<FeatureFile> features) {
        Map<String, List<FeatureFile>> featuresByDir = new HashMap<>();
        
        for (FeatureFile feature : features) {
            String path = feature.getPath();
            String directory = "";
            
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash > 0) {
                directory = path.substring(0, lastSlash);
            }
            
            featuresByDir.computeIfAbsent(directory, k -> new ArrayList<>()).add(feature);
        }
        
        return featuresByDir;
    }
    
    /**
     * Generate individual feature page
     * 
     * @param feature Feature to generate page for
     * @throws IOException If error writing file
     */
    private void generateFeaturePage(FeatureFile feature) throws IOException {
        String htmlFilename = getHtmlFilename(feature.getPath());
        File outputFile = new File(outputDir, htmlFilename);
        
        // Create parent directory if it doesn't exist
        outputFile.getParentFile().mkdirs();
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>").append(feature.getTitle()).append(" - Mobile Auth Security Test Documentation</title>\n")
            .append("    <link rel=\"stylesheet\" href=\"").append(getRelativePath(feature.getPath())).append("styles.css\">\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <header>\n")
            .append("        <h1>").append(feature.getTitle()).append("</h1>\n");
        
        if (!feature.getDescription().isEmpty()) {
            html.append("        <div class=\"feature-description\">\n")
                .append("            <p>").append(feature.getDescription().replace("\n", "</p>\n            <p>")).append("</p>\n")
                .append("        </div>\n");
        }
        
        html.append("    </header>\n")
            .append("    <div class=\"container\">\n")
            .append("        <a href=\"").append(getRelativePath(feature.getPath())).append("index.html\" class=\"back-link\">&larr; Back to Index</a>\n")
            .append("        <section class=\"scenarios\">\n");
        
        // Add scenarios
        for (Scenario scenario : feature.getScenarios()) {
            html.append("            <div class=\"scenario\">\n")
                .append("                <h2>").append(scenario.isOutline() ? "Scenario Outline: " : "Scenario: ")
                .append(scenario.getTitle()).append("</h2>\n");
            
            // Add tags
            if (!scenario.getTags().isEmpty()) {
                html.append("                <div class=\"tags\">\n");
                for (String tag : scenario.getTags()) {
                    html.append("                    <span class=\"tag").append(getTagClass(tag)).append("\">")
                        .append(tag).append("</span>\n");
                }
                html.append("                </div>\n");
            }
            
            // Add steps
            html.append("                <div class=\"steps\">\n")
                .append("                    <pre>").append(formatSteps(scenario.getSteps())).append("</pre>\n")
                .append("                </div>\n");
            
            // Add examples
            if (scenario.getExamples() != null && !scenario.getExamples().isEmpty()) {
                html.append("                <div class=\"examples\">\n")
                    .append("                    <h3>Examples:</h3>\n")
                    .append("                    <pre>").append(formatExamples(scenario.getExamples())).append("</pre>\n")
                    .append("                </div>\n");
            }
            
            html.append("            </div>\n");
        }
        
        html.append("        </section>\n")
            .append("    </div>\n")
            .append("    <footer>\n")
            .append("        <p>Generated on: ").append(java.time.LocalDateTime.now()).append("</p>\n")
            .append("    </footer>\n")
            .append("</body>\n")
            .append("</html>");
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }
    }
    
    /**
     * Generate CSS file for styling the documentation
     * 
     * @throws IOException If error writing file
     */
    private void generateCssFile() throws IOException {
        String css = "* {\n" +
                "    box-sizing: border-box;\n" +
                "    margin: 0;\n" +
                "    padding: 0;\n" +
                "}\n\n" +
                "body {\n" +
                "    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "    line-height: 1.6;\n" +
                "    color: #333;\n" +
                "    background-color: #f8f9fa;\n" +
                "}\n\n" +
                "header {\n" +
                "    background-color: #343a40;\n" +
                "    color: #fff;\n" +
                "    padding: 2rem;\n" +
                "    text-align: center;\n" +
                "}\n\n" +
                ".container {\n" +
                "    max-width: 1200px;\n" +
                "    margin: 0 auto;\n" +
                "    padding: 2rem;\n" +
                "}\n\n" +
                "h1, h2, h3 {\n" +
                "    margin-bottom: 1rem;\n" +
                "}\n\n" +
                "ul {\n" +
                "    list-style-type: none;\n" +
                "}\n\n" +
                "a {\n" +
                "    color: #007bff;\n" +
                "    text-decoration: none;\n" +
                "}\n\n" +
                "a:hover {\n" +
                "    text-decoration: underline;\n" +
                "}\n\n" +
                ".back-link {\n" +
                "    display: inline-block;\n" +
                "    margin-bottom: 1rem;\n" +
                "}\n\n" +
                ".feature-group h3 {\n" +
                "    margin-top: 1rem;\n" +
                "    color: #555;\n" +
                "}\n\n" +
                ".feature-description {\n" +
                "    margin-top: 1rem;\n" +
                "    font-style: italic;\n" +
                "    max-width: 800px;\n" +
                "    margin-left: auto;\n" +
                "    margin-right: auto;\n" +
                "}\n\n" +
                ".scenario {\n" +
                "    background-color: #fff;\n" +
                "    border-radius: 5px;\n" +
                "    padding: 1.5rem;\n" +
                "    margin-bottom: 2rem;\n" +
                "    box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "}\n\n" +
                ".tags {\n" +
                "    margin-bottom: 1rem;\n" +
                "}\n\n" +
                ".tag {\n" +
                "    display: inline-block;\n" +
                "    padding: 0.25rem 0.5rem;\n" +
                "    font-size: 0.8rem;\n" +
                "    border-radius: 4px;\n" +
                "    margin-right: 0.5rem;\n" +
                "    margin-bottom: 0.5rem;\n" +
                "    background-color: #e9ecef;\n" +
                "}\n\n" +
                ".tag-security {\n" +
                "    background-color: #dc3545;\n" +
                "    color: #fff;\n" +
                "}\n\n" +
                ".tag-smoke {\n" +
                "    background-color: #28a745;\n" +
                "    color: #fff;\n" +
                "}\n\n" +
                ".tag-regression {\n" +
                "    background-color: #007bff;\n" +
                "    color: #fff;\n" +
                "}\n\n" +
                ".tag-performance {\n" +
                "    background-color: #ffc107;\n" +
                "    color: #212529;\n" +
                "}\n\n" +
                ".tag-accessibility {\n" +
                "    background-color: #6f42c1;\n" +
                "    color: #fff;\n" +
                "}\n\n" +
                ".tag-critical {\n" +
                "    background-color: #fd7e14;\n" +
                "    color: #fff;\n" +
                "}\n\n" +
                ".steps, .examples {\n" +
                "    margin-top: 1rem;\n" +
                "    background-color: #f8f9fa;\n" +
                "    border-radius: 4px;\n" +
                "    overflow-x: auto;\n" +
                "}\n\n" +
                "pre {\n" +
                "    padding: 1rem;\n" +
                "    font-family: 'Courier New', Courier, monospace;\n" +
                "    white-space: pre-wrap;\n" +
                "}\n\n" +
                "footer {\n" +
                "    text-align: center;\n" +
                "    padding: 1rem;\n" +
                "    background-color: #343a40;\n" +
                "    color: #fff;\n" +
                "    font-size: 0.8rem;\n" +
                "}\n\n" +
                ".summary {\n" +
                "    margin-top: 2rem;\n" +
                "    padding: 1rem;\n" +
                "    background-color: #fff;\n" +
                "    border-radius: 5px;\n" +
                "    box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
                "}\n";
        
        try (FileWriter writer = new FileWriter(new File(outputDir, "styles.css"))) {
            writer.write(css);
        }
    }
    
    /**
     * Get the HTML filename for a feature file path
     * 
     * @param featurePath Path to feature file
     * @return HTML filename
     */
    private String getHtmlFilename(String featurePath) {
        return featurePath.replace(".feature", ".html");
    }
    
    /**
     * Get relative path to root directory
     * 
     * @param path Feature file path
     * @return Relative path to root
     */
    private String getRelativePath(String path) {
        int depth = path.split("/").length - 1;
        StringBuilder relativePath = new StringBuilder();
        
        for (int i = 0; i < depth; i++) {
            relativePath.append("../");
        }
        
        return relativePath.toString();
    }
    
    /**
     * Get CSS class for a tag
     * 
     * @param tag Tag name
     * @return CSS class name
     */
    private String getTagClass(String tag) {
        String tagName = tag.substring(1).toLowerCase();
        switch (tagName) {
            case "security":
            case "smoke":
            case "regression":
            case "performance":
            case "accessibility":
            case "critical":
                return " tag-" + tagName;
            default:
                return "";
        }
    }
    
    /**
     * Format steps with syntax highlighting
     * 
     * @param steps Steps string
     * @return Formatted steps
     */
    private String formatSteps(String steps) {
        return steps.replaceAll("Given ", "<span style=\"color: #28a745;\">Given </span>")
                   .replaceAll("When ", "<span style=\"color: #007bff;\">When </span>")
                   .replaceAll("Then ", "<span style=\"color: #dc3545;\">Then </span>")
                   .replaceAll("And ", "<span style=\"color: #6c757d;\">And </span>")
                   .replaceAll("But ", "<span style=\"color: #fd7e14;\">But </span>")
                   .replaceAll("\"([^\"]*)\"", "<span style=\"color: #6f42c1;\">\"$1\"</span>");
    }
    
    /**
     * Format examples with table styling
     * 
     * @param examples Examples string
     * @return Formatted examples
     */
    private String formatExamples(String examples) {
        return examples.replaceAll("\\|", "<span style=\"color: #6c757d;\">|</span>");
    }
    
    /**
     * Model class for a feature file
     */
    private static class FeatureFile {
        private final String title;
        private final String description;
        private final String path;
        private final List<Scenario> scenarios;
        
        public FeatureFile(String title, String description, String path, List<Scenario> scenarios) {
            this.title = title;
            this.description = description;
            this.path = path;
            this.scenarios = scenarios;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getPath() {
            return path;
        }
        
        public List<Scenario> getScenarios() {
            return scenarios;
        }
    }
    
    /**
     * Model class for a scenario
     */
    private static class Scenario {
        private final String title;
        private final String steps;
        private final boolean outline;
        private String examples;
        private final List<String> tags = new ArrayList<>();
        
        public Scenario(String title, String steps, boolean outline) {
            this.title = title;
            this.steps = steps;
            this.outline = outline;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getSteps() {
            return steps;
        }
        
        public boolean isOutline() {
            return outline;
        }
        
        public String getExamples() {
            return examples;
        }
        
        public void setExamples(String examples) {
            this.examples = examples;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void addTag(String tag) {
            tags.add(tag);
        }
    }
}
