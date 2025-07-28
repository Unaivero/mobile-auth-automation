package com.securitytests.utils.wait;

import com.securitytests.utils.logging.StructuredLogger;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Advanced waiting strategies to handle flaky elements and improve test stability
 */
public class SmartWait {
    private static final StructuredLogger logger = new StructuredLogger(SmartWait.class);
    private final AppiumDriver driver;
    private final long defaultTimeoutSeconds;
    private final long defaultPollingIntervalMillis;
    
    /**
     * Create a new SmartWait
     * @param driver The AppiumDriver
     */
    public SmartWait(AppiumDriver driver) {
        this(driver, 15, 500);
    }
    
    /**
     * Create a new SmartWait with custom timeout and polling interval
     * @param driver The AppiumDriver
     * @param timeoutSeconds Default timeout in seconds
     * @param pollingIntervalMillis Default polling interval in milliseconds
     */
    public SmartWait(AppiumDriver driver, long timeoutSeconds, long pollingIntervalMillis) {
        this.driver = driver;
        this.defaultTimeoutSeconds = timeoutSeconds;
        this.defaultPollingIntervalMillis = pollingIntervalMillis;
    }
    
    /**
     * Wait for an element to be clickable, retrying with different strategies if it fails
     * @param locator The element locator
     * @return The WebElement when it's clickable
     */
    public WebElement waitForClickable(By locator) {
        logger.debug("Waiting for element to be clickable: {}", locator);
        
        try {
            // First attempt - standard wait
            return createWait().until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            logger.debug("Standard wait failed for clickable element: {}, trying recovery strategies", locator);
            
            // Recovery strategies
            return tryAlternativeWaitStrategies(locator, "clickable");
        }
    }
    
    /**
     * Wait for an element to be visible, retrying with different strategies if it fails
     * @param locator The element locator
     * @return The WebElement when it's visible
     */
    public WebElement waitForVisible(By locator) {
        logger.debug("Waiting for element to be visible: {}", locator);
        
        try {
            // First attempt - standard wait
            return createWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            logger.debug("Standard wait failed for visible element: {}, trying recovery strategies", locator);
            
            // Recovery strategies
            return tryAlternativeWaitStrategies(locator, "visible");
        }
    }
    
    /**
     * Wait for an element to contain specific text
     * @param locator The element locator
     * @param text The text to wait for
     * @return The WebElement when it contains the text
     */
    public WebElement waitForTextPresent(By locator, String text) {
        logger.debug("Waiting for text '{}' in element: {}", text, locator);
        
        try {
            // First attempt - standard wait
            return createWait().until(driver -> {
                WebElement element = driver.findElement(locator);
                String elementText = element.getText();
                return elementText.contains(text) ? element : null;
            });
        } catch (TimeoutException e) {
            logger.debug("Standard wait failed for text '{}' in element: {}, trying recovery strategies", text, locator);
            
            // Try refreshing the page state
            refreshPageState();
            
            // Second attempt after refresh
            return createWait().until(driver -> {
                WebElement element = driver.findElement(locator);
                String elementText = element.getText();
                return elementText.contains(text) ? element : null;
            });
        }
    }
    
    /**
     * Wait for an element using a custom condition
     * @param locator The element locator
     * @param condition The custom condition
     * @param <T> The type of the expected result
     * @return The result of the condition
     */
    public <T> T waitFor(By locator, Function<WebElement, T> condition) {
        logger.debug("Waiting for custom condition on element: {}", locator);
        
        return createWait().until(driver -> {
            try {
                WebElement element = driver.findElement(locator);
                return condition.apply(element);
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                return null;
            }
        });
    }
    
    /**
     * Wait for a stable state of the application (no ongoing animations, network requests)
     */
    public void waitForStableState() {
        logger.debug("Waiting for stable application state");
        
        // Wait for no active network requests (using JavaScript executor)
        try {
            createWait().until(driver -> {
                Object activeRequests = ((JavascriptExecutor) driver).executeScript(
                    "return window.performance && window.performance.getEntriesByType && " +
                    "window.performance.getEntriesByType('resource').filter(r => !r.responseEnd).length === 0;"
                );
                return Boolean.TRUE.equals(activeRequests);
            });
        } catch (Exception e) {
            logger.debug("Error checking network requests, continuing: {}", e.getMessage());
        }
        
        // Wait for no ongoing animations
        try {
            createWait(3, 100).until(driver -> {
                Object animations = ((JavascriptExecutor) driver).executeScript(
                    "return document.querySelector(':animated') === null;"
                );
                return Boolean.TRUE.equals(animations);
            });
        } catch (Exception e) {
            logger.debug("Error checking animations, continuing: {}", e.getMessage());
        }
    }
    
    /**
     * Create a new FluentWait with default settings
     * @return A new FluentWait
     */
    private FluentWait<AppiumDriver> createWait() {
        return createWait(defaultTimeoutSeconds, defaultPollingIntervalMillis);
    }
    
    /**
     * Create a new FluentWait with custom settings
     * @param timeoutSeconds Timeout in seconds
     * @param pollingIntervalMillis Polling interval in milliseconds
     * @return A new FluentWait
     */
    private FluentWait<AppiumDriver> createWait(long timeoutSeconds, long pollingIntervalMillis) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollingIntervalMillis))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }
    
    /**
     * Try different waiting strategies when the standard wait fails
     * @param locator The element locator
     * @param waitType The type of wait (clickable, visible)
     * @return The WebElement when found
     */
    private WebElement tryAlternativeWaitStrategies(By locator, String waitType) {
        Map<String, Object> context = new HashMap<>();
        context.put("locator", locator.toString());
        context.put("waitType", waitType);
        
        // Strategy 1: Refresh page state and retry
        try {
            logger.debug("Recovery strategy 1: Refreshing page state for {}", locator);
            refreshPageState();
            
            return "clickable".equals(waitType)
                ? createWait().until(ExpectedConditions.elementToBeClickable(locator))
                : createWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            logger.debug("Recovery strategy 1 failed for {}: {}", locator, e.getMessage());
        }
        
        // Strategy 2: Try with a longer timeout
        try {
            logger.debug("Recovery strategy 2: Increased timeout for {}", locator);
            FluentWait<AppiumDriver> longWait = createWait(defaultTimeoutSeconds * 2, defaultPollingIntervalMillis);
            
            return "clickable".equals(waitType)
                ? longWait.until(ExpectedConditions.elementToBeClickable(locator))
                : longWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            logger.debug("Recovery strategy 2 failed for {}: {}", locator, e.getMessage());
        }
        
        // Strategy 3: Try presence first, then visibility/clickability
        try {
            logger.debug("Recovery strategy 3: Checking presence first for {}", locator);
            WebElement element = createWait().until(ExpectedConditions.presenceOfElementLocated(locator));
            
            // Scroll to element if it's a web context
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            } catch (Exception ignored) {}
            
            // Now try original condition
            return "clickable".equals(waitType)
                ? createWait().until(ExpectedConditions.elementToBeClickable(locator))
                : createWait().until(ExpectedConditions.visibilityOf(element));
        } catch (Exception e) {
            logger.debug("Recovery strategy 3 failed for {}: {}", locator, e.getMessage());
        }
        
        // If all strategies fail, throw an informative exception
        String message = String.format(
            "Element %s failed to become %s after trying multiple recovery strategies",
            locator, waitType
        );
        
        logger.error("Element wait failed after multiple recovery strategies: {}", message);
        throw new TimeoutException(message);
    }
    
    /**
     * Refresh the page state to handle stale elements
     */
    private void refreshPageState() {
        try {
            // Execute platform-specific refresh actions
            if (driver.getPlatformName().toLowerCase().contains("android")) {
                // For Android, sometimes hiding keyboard or tapping away helps
                try {
                    driver.hideKeyboard();
                } catch (Exception ignored) {}
                
                // Small swipe to refresh view
                Dimension size = driver.manage().window().getSize();
                int startY = size.height / 2;
                int endY = startY;
                int startX = size.width / 2;
                int endX = startX - 50; // Small horizontal swipe
                
                new TouchAction<>(driver)
                    .press(PointOption.point(startX, startY))
                    .waitAction(WaitOptions.waitOptions(Duration.ofMillis(100)))
                    .moveTo(PointOption.point(endX, endY))
                    .release()
                    .perform();
                
            } else {
                // For iOS, a small scroll usually refreshes the view
                Map<String, Object> params = new HashMap<>();
                params.put("direction", "down");
                params.put("distance", 0.1);
                driver.executeScript("mobile: scroll", params);
            }
            
            // Small wait after refresh
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
            
        } catch (Exception e) {
            logger.debug("Error refreshing page state: {}", e.getMessage());
        }
    }
}
