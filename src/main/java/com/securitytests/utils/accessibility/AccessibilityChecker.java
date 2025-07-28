package com.securitytests.utils.accessibility;

import com.securitytests.utils.logging.StructuredLogger;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to check mobile app screens for accessibility compliance
 */
public class AccessibilityChecker {
    private final AppiumDriver driver;
    private static final StructuredLogger logger = new StructuredLogger(AccessibilityChecker.class);
    
    // Accessibility check types and descriptions
    private static final Map<String, String> CHECK_DESCRIPTIONS = new HashMap<>();
    
    static {
        CHECK_DESCRIPTIONS.put("contentDesc", "Missing content description for interactive element");
        CHECK_DESCRIPTIONS.put("textSize", "Text size too small for readability");
        CHECK_DESCRIPTIONS.put("contrast", "Insufficient contrast ratio");
        CHECK_DESCRIPTIONS.put("touchTarget", "Touch target size too small");
        CHECK_DESCRIPTIONS.put("labeledBy", "Element not properly labeled");
    }
    
    /**
     * Create a new accessibility checker
     * @param driver The AppiumDriver instance
     */
    public AccessibilityChecker(AppiumDriver driver) {
        this.driver = driver;
    }
    
    /**
     * Run a full accessibility audit on the current screen
     * @param screenName Name of the screen being tested
     * @return AccessibilityReport containing issues found
     */
    @Step("Run accessibility audit on {screenName} screen")
    public AccessibilityReport auditScreen(String screenName) {
        logger.info("Running accessibility audit on screen: {}", screenName);
        
        List<AccessibilityIssue> issues = new ArrayList<>();
        
        // Check content descriptions on interactive elements
        issues.addAll(checkContentDescriptions());
        
        // Check text sizes
        issues.addAll(checkTextSizes());
        
        // Check touch target sizes
        issues.addAll(checkTouchTargetSizes());
        
        // Check contrast ratios if platform supports it
        issues.addAll(checkContrastRatios());
        
        // Create and return the report
        AccessibilityReport report = new AccessibilityReport(screenName, issues);
        
        // Log summary
        int issueCount = issues.size();
        if (issueCount > 0) {
            logger.warn("Found {} accessibility issues on {} screen", issueCount, screenName);
        } else {
            logger.info("No accessibility issues found on {} screen", screenName);
        }
        
        // Attach report to Allure
        attachAccessibilityReport(report);
        
        return report;
    }
    
    /**
     * Check content descriptions on interactive elements
     * @return List of accessibility issues found
     */
    private List<AccessibilityIssue> checkContentDescriptions() {
        List<AccessibilityIssue> issues = new ArrayList<>();
        
        try {
            // For Android
            if (driver.getPlatformName().toLowerCase().contains("android")) {
                // Get all interactive elements without content descriptions
                @SuppressWarnings("unchecked")
                List<WebElement> elements = (List<WebElement>) driver.executeScript(
                    "mobile: findElementsWithAttribute", 
                    Map.of(
                        "attribute", "clickable", 
                        "value", "true"
                    )
                );
                
                for (WebElement element : elements) {
                    String contentDesc = element.getAttribute("content-desc");
                    if (contentDesc == null || contentDesc.trim().isEmpty()) {
                        issues.add(new AccessibilityIssue(
                            "contentDesc",
                            CHECK_DESCRIPTIONS.get("contentDesc"),
                            "Interactive element missing content description",
                            AccessibilityIssue.Severity.MEDIUM
                        ));
                    }
                }
            } 
            // For iOS
            else {
                @SuppressWarnings("unchecked")
                List<WebElement> elements = (List<WebElement>) driver.executeScript(
                    "mobile: findElementsByPredicateString",
                    "isAccessibilityElement == true AND label == ''"
                );
                
                for (WebElement element : elements) {
                    issues.add(new AccessibilityIssue(
                        "contentDesc",
                        CHECK_DESCRIPTIONS.get("contentDesc"),
                        "Interactive element missing accessibility label",
                        AccessibilityIssue.Severity.MEDIUM
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Error checking content descriptions: {}", e.getMessage());
        }
        
        return issues;
    }
    
    /**
     * Check text sizes for readability
     * @return List of accessibility issues found
     */
    private List<AccessibilityIssue> checkTextSizes() {
        List<AccessibilityIssue> issues = new ArrayList<>();
        
        try {
            // For Android
            if (driver.getPlatformName().toLowerCase().contains("android")) {
                @SuppressWarnings("unchecked")
                List<WebElement> textElements = (List<WebElement>) driver.findElementsByClassName("android.widget.TextView");
                
                for (WebElement element : textElements) {
                    String textSize = element.getAttribute("text-size");
                    if (textSize != null) {
                        try {
                            // Extract numeric value from text size (e.g. "14sp" -> 14)
                            float size = Float.parseFloat(textSize.replaceAll("[^\\d.]", ""));
                            if (size < 12) {  // Consider 12sp as minimum for readability
                                issues.add(new AccessibilityIssue(
                                    "textSize",
                                    CHECK_DESCRIPTIONS.get("textSize"),
                                    "Text size too small: " + textSize,
                                    AccessibilityIssue.Severity.LOW
                                ));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            // iOS text size checks would go here
        } catch (Exception e) {
            logger.error("Error checking text sizes: {}", e.getMessage());
        }
        
        return issues;
    }
    
    /**
     * Check touch target sizes for usability
     * @return List of accessibility issues found
     */
    private List<AccessibilityIssue> checkTouchTargetSizes() {
        List<AccessibilityIssue> issues = new ArrayList<>();
        
        try {
            // For Android
            if (driver.getPlatformName().toLowerCase().contains("android")) {
                @SuppressWarnings("unchecked")
                List<WebElement> clickableElements = (List<WebElement>) driver.executeScript(
                    "mobile: findElementsWithAttribute", 
                    Map.of(
                        "attribute", "clickable", 
                        "value", "true"
                    )
                );
                
                for (WebElement element : clickableElements) {
                    // Get element dimensions
                    int width = element.getSize().getWidth();
                    int height = element.getSize().getHeight();
                    
                    // Minimum touch target size should be 48x48dp (per accessibility guidelines)
                    if (width < 48 || height < 48) {
                        issues.add(new AccessibilityIssue(
                            "touchTarget",
                            CHECK_DESCRIPTIONS.get("touchTarget"),
                            String.format("Touch target size too small: %dx%d (should be at least 48x48)", width, height),
                            AccessibilityIssue.Severity.MEDIUM
                        ));
                    }
                }
            }
            // iOS touch target checks would go here
        } catch (Exception e) {
            logger.error("Error checking touch target sizes: {}", e.getMessage());
        }
        
        return issues;
    }
    
    /**
     * Check contrast ratios for visibility
     * @return List of accessibility issues found
     */
    private List<AccessibilityIssue> checkContrastRatios() {
        // This is a placeholder for contrast ratio checks
        // In a real implementation, this would use platform-specific APIs 
        // to check color contrast between text and background
        
        return new ArrayList<>();
    }
    
    /**
     * Attach accessibility report to Allure
     * @param report The accessibility report
     * @return The report as a string
     */
    @Attachment(value = "Accessibility Report - {0}", type = "text/plain")
    private String attachAccessibilityReport(AccessibilityReport report) {
        return report.toString();
    }
}
