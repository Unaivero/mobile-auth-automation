package com.securitytests.utils.visual;

import com.securitytests.utils.logging.StructuredLogger;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for visual validation of UI elements and screens
 */
public class VisualValidator {
    private final AppiumDriver driver;
    private static final StructuredLogger logger = new StructuredLogger(VisualValidator.class);
    private final String baselineDir;
    private final String resultsDir;
    private final ImageDiffer imageDiffer;
    
    /**
     * Create a new visual validator
     * @param driver The AppiumDriver instance
     */
    public VisualValidator(AppiumDriver driver) {
        this.driver = driver;
        this.baselineDir = "target/visual-baselines";
        this.resultsDir = "target/visual-results";
        this.imageDiffer = new ImageDiffer();
        
        // Create directories if they don't exist
        createDirectories();
    }
    
    /**
     * Create a new visual validator with custom directories
     * @param driver The AppiumDriver instance
     * @param baselineDir Directory to store baseline images
     * @param resultsDir Directory to store result images
     */
    public VisualValidator(AppiumDriver driver, String baselineDir, String resultsDir) {
        this.driver = driver;
        this.baselineDir = baselineDir;
        this.resultsDir = resultsDir;
        this.imageDiffer = new ImageDiffer();
        
        // Create directories if they don't exist
        createDirectories();
    }
    
    /**
     * Create the required directories
     */
    private void createDirectories() {
        try {
            Files.createDirectories(Paths.get(baselineDir));
            Files.createDirectories(Paths.get(resultsDir));
            Files.createDirectories(Paths.get(resultsDir + "/diffs"));
            
            logger.info("Created visual testing directories: {} and {}", baselineDir, resultsDir);
        } catch (IOException e) {
            logger.error("Failed to create visual testing directories: {}", e.getMessage());
        }
    }
    
    /**
     * Capture screenshot of the entire screen
     * @return Screenshot object
     */
    public Screenshot captureScreen() {
        logger.info("Capturing full screen screenshot");
        return new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(1000))
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(driver);
    }
    
    /**
     * Capture screenshot of a specific element
     * @param element The WebElement to capture
     * @return Screenshot object
     */
    public Screenshot captureElement(WebElement element) {
        logger.info("Capturing element screenshot");
        return new AShot()
                .coordsProvider(new WebDriverCoordsProvider())
                .takeScreenshot(driver, element);
    }
    
    /**
     * Save a screenshot as a baseline image
     * @param screenshot The Screenshot object
     * @param baselineName Name of the baseline
     * @return Path to the saved baseline image
     */
    public String saveBaseline(Screenshot screenshot, String baselineName) {
        String fileName = baselineName + ".png";
        String filePath = baselineDir + "/" + fileName;
        
        try {
            ImageIO.write(screenshot.getImage(), "PNG", new File(filePath));
            logger.info("Saved baseline image: {}", filePath);
            return filePath;
        } catch (IOException e) {
            logger.error("Failed to save baseline image: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Compare a screenshot with a baseline image
     * @param screenshot The Screenshot object
     * @param baselineName Name of the baseline to compare against
     * @param testName Name of the test being run
     * @return VisualComparisonResult containing comparison results
     */
    @Step("Compare screenshot with baseline {baselineName}")
    public VisualComparisonResult compareWithBaseline(
            Screenshot screenshot, String baselineName, String testName) {
        String baselineFilePath = baselineDir + "/" + baselineName + ".png";
        File baselineFile = new File(baselineFilePath);
        
        // Check if baseline exists
        if (!baselineFile.exists()) {
            logger.warn("Baseline image does not exist: {}", baselineFilePath);
            String newBaseline = saveBaseline(screenshot, baselineName);
            return new VisualComparisonResult(false, 0, null, newBaseline, null);
        }
        
        try {
            // Load baseline image
            Screenshot baseline = new Screenshot(ImageIO.read(baselineFile));
            
            // Compare images
            ImageDiff diff = imageDiffer.makeDiff(baseline, screenshot);
            
            // Save actual screenshot
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String actualFilePath = resultsDir + "/" + baselineName + "_" + timestamp + ".png";
            ImageIO.write(screenshot.getImage(), "PNG", new File(actualFilePath));
            
            // If there's a difference, save the diff image
            String diffFilePath = null;
            if (diff.hasDiff()) {
                diffFilePath = resultsDir + "/diffs/" + baselineName + "_" + timestamp + "_diff.png";
                ImageIO.write(diff.getMarkedImage(), "PNG", new File(diffFilePath));
                logger.warn("Visual difference detected for {}, diff saved to {}", baselineName, diffFilePath);
            } else {
                logger.info("No visual difference detected for {}", baselineName);
            }
            
            // Create comparison result
            VisualComparisonResult result = new VisualComparisonResult(
                    diff.hasDiff(), 
                    diff.getDiffSize(),
                    baselineFilePath, 
                    actualFilePath, 
                    diffFilePath
            );
            
            // Attach images to Allure report
            if (diff.hasDiff()) {
                attachScreenshotToAllure("Baseline", baselineFile);
                attachScreenshotToAllure("Actual", new File(actualFilePath));
                attachScreenshotToAllure("Difference", new File(diffFilePath));
            }
            
            return result;
            
        } catch (IOException e) {
            logger.error("Failed to compare images: {}", e.getMessage());
            return new VisualComparisonResult(true, 0, baselineFilePath, null, null);
        }
    }
    
    /**
     * Validate a specific element against a baseline
     * @param element The WebElement to validate
     * @param baselineName Name of the baseline
     * @param testName Name of the test being run
     * @return VisualComparisonResult containing comparison results
     */
    @Step("Validate element against baseline {baselineName}")
    public VisualComparisonResult validateElement(WebElement element, String baselineName, String testName) {
        Screenshot screenshot = captureElement(element);
        return compareWithBaseline(screenshot, baselineName, testName);
    }
    
    /**
     * Validate the current screen against a baseline
     * @param baselineName Name of the baseline
     * @param testName Name of the test being run
     * @return VisualComparisonResult containing comparison results
     */
    @Step("Validate screen against baseline {baselineName}")
    public VisualComparisonResult validateScreen(String baselineName, String testName) {
        Screenshot screenshot = captureScreen();
        return compareWithBaseline(screenshot, baselineName, testName);
    }
    
    /**
     * Capture and attach a screenshot to the Allure report
     * @param name Name of the attachment
     * @return Byte array of the screenshot
     */
    @Attachment(value = "{name}", type = "image/png")
    public byte[] captureAndAttachScreenshot(String name) {
        try {
            Screenshot screenshot = captureScreen();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenshot.getImage(), "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to capture and attach screenshot: {}", e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Attach a screenshot file to the Allure report
     * @param name Name of the attachment
     * @param file The screenshot file
     * @return Byte array of the screenshot
     */
    @Attachment(value = "{name}", type = "image/png")
    private byte[] attachScreenshotToAllure(String name, File file) {
        try {
            return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            logger.error("Failed to attach screenshot to Allure report: {}", e.getMessage());
            return new byte[0];
        }
    }
    
    /**
     * Create a mask for ignoring dynamic areas in screenshots
     * @param screenshot The original screenshot
     * @param regions Array of rectangles defining areas to ignore
     * @return Screenshot with masked regions
     */
    public Screenshot createMaskedScreenshot(Screenshot screenshot, Rectangle[] regions) {
        BufferedImage image = screenshot.getImage();
        Graphics2D g = image.createGraphics();
        
        g.setColor(Color.BLACK);
        for (Rectangle region : regions) {
            g.fillRect(region.x, region.y, region.width, region.height);
        }
        
        g.dispose();
        return new Screenshot(image);
    }
    
    /**
     * Clear all baselines - USE WITH CAUTION
     */
    public void clearBaselines() {
        File dir = new File(baselineDir);
        try {
            FileUtils.cleanDirectory(dir);
            logger.info("Cleared all baseline images from {}", baselineDir);
        } catch (IOException e) {
            logger.error("Failed to clear baseline images: {}", e.getMessage());
        }
    }
    
    /**
     * Generate a unique identifier for a visual test
     * @param baseName Base name for the ID
     * @return Unique identifier string
     */
    public static String generateVisualTestId(String baseName) {
        return baseName + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
