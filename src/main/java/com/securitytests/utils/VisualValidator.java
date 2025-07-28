package com.securitytests.utils;

import com.securitytests.pages.BasePage;
import com.securitytests.pages.LoginPage;
import io.qameta.allure.Attachment;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.comparison.ImageDiff;
import ru.yandex.qatools.ashot.comparison.ImageDiffer;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility for visual validation of UI elements like CAPTCHA
 */
public class VisualValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualValidator.class);
    private static final String SCREENSHOTS_DIR = "target/screenshots";
    private static final By CAPTCHA_CONTAINER_LOCATOR = By.id("com.securitytests.demoapp:id/captchaContainer");
    
    /**
     * Takes a screenshot of the CAPTCHA element for visual validation
     *
     * @param loginPage the login page containing the CAPTCHA
     * @return true if CAPTCHA screenshot was taken successfully
     */
    public static boolean captureCaptchaScreenshot(LoginPage loginPage) {
        try {
            LOGGER.info("Taking screenshot of CAPTCHA element");
            
            // Create screenshots directory if it doesn't exist
            Files.createDirectories(Paths.get(SCREENSHOTS_DIR));
            
            // Take screenshot of CAPTCHA element using AShot
            WebElement captchaElement = loginPage.driver.findElement(CAPTCHA_CONTAINER_LOCATOR);
            Screenshot screenshot = new AShot()
                    .coordsProvider(new WebDriverCoordsProvider())
                    .takeScreenshot(loginPage.driver, captchaElement);
            
            // Save screenshot to file
            String filename = SCREENSHOTS_DIR + "/captcha_" + System.currentTimeMillis() + ".png";
            ImageIO.write(screenshot.getImage(), "PNG", new File(filename));
            
            // Attach screenshot to Allure report
            byte[] screenshotBytes = imageToBytes(screenshot.getImage());
            attachCaptchaScreenshot(screenshotBytes);
            
            LOGGER.info("CAPTCHA screenshot saved to: {}", filename);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to take CAPTCHA screenshot", e);
            return false;
        }
    }
    
    /**
     * Compare current CAPTCHA with baseline image to detect visual changes
     *
     * @param loginPage the login page containing the CAPTCHA
     * @param baselineImagePath path to the baseline CAPTCHA image
     * @return true if CAPTCHA matches the baseline, false if different
     */
    public static boolean validateCaptchaImage(LoginPage loginPage, String baselineImagePath) {
        try {
            LOGGER.info("Validating CAPTCHA image against baseline");
            
            // Take screenshot of current CAPTCHA
            WebElement captchaElement = loginPage.driver.findElement(CAPTCHA_CONTAINER_LOCATOR);
            Screenshot currentCaptcha = new AShot()
                    .coordsProvider(new WebDriverCoordsProvider())
                    .takeScreenshot(loginPage.driver, captchaElement);
            
            // Load baseline image
            BufferedImage baselineImage = ImageIO.read(new File(baselineImagePath));
            
            // Compare images
            ImageDiffer imageDiffer = new ImageDiffer();
            ImageDiff diff = imageDiffer.makeDiff(currentCaptcha.getImage(), baselineImage);
            
            // Save diff image if there are differences
            if (diff.hasDiff()) {
                LOGGER.info("CAPTCHA visual differences detected");
                String diffFilename = SCREENSHOTS_DIR + "/captcha_diff_" + System.currentTimeMillis() + ".png";
                ImageIO.write(diff.getMarkedImage(), "PNG", new File(diffFilename));
                
                // Attach diff to Allure report
                byte[] diffBytes = imageToBytes(diff.getMarkedImage());
                attachDiffImage(diffBytes);
            }
            
            return !diff.hasDiff();
        } catch (Exception e) {
            LOGGER.error("Failed to validate CAPTCHA image", e);
            return false;
        }
    }
    
    /**
     * Convert BufferedImage to byte array
     *
     * @param image the image to convert
     * @return byte array representation of the image
     */
    private static byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
    
    /**
     * Attach CAPTCHA screenshot to Allure report
     *
     * @param screenshot the screenshot as byte array
     * @return the same byte array for chaining
     */
    @Attachment(value = "CAPTCHA Screenshot", type = "image/png")
    private static byte[] attachCaptchaScreenshot(byte[] screenshot) {
        return screenshot;
    }
    
    /**
     * Attach diff image to Allure report
     *
     * @param diffImage the diff image as byte array
     * @return the same byte array for chaining
     */
    @Attachment(value = "CAPTCHA Visual Diff", type = "image/png")
    private static byte[] attachDiffImage(byte[] diffImage) {
        return diffImage;
    }
}
