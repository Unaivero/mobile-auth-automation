package com.securitytests.utils.security;

import com.securitytests.utils.logging.StructuredLogger;
import org.zaproxy.clientapi.core.*;
import org.zaproxy.clientapi.gen.Alert;
import org.zaproxy.clientapi.gen.Spider;
import org.zaproxy.clientapi.gen.Ascan;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;

/**
 * OWASP ZAP security scanner integration for automated security testing
 * Requires ZAP to be running either locally or on a designated server
 */
public class ZAPSecurityScanner {
    private static final StructuredLogger logger = new StructuredLogger(ZAPSecurityScanner.class);
    private final ClientApi zapClient;
    private final String targetUrl;
    private final String contextName;
    private final String apiKey;
    private final int timeoutInSeconds;
    private final Map<String, String> scannerConfig;
    
    /**
     * Create a new ZAP security scanner
     * @param zapHost ZAP proxy host (e.g., "localhost")
     * @param zapPort ZAP proxy port (default: 8080)
     * @param apiKey ZAP API key for authentication (if configured in ZAP)
     * @param targetUrl The base URL of the target application to scan
     * @param contextName The ZAP context name to use
     * @param timeoutInSeconds Maximum timeout for scanning operations
     */
    public ZAPSecurityScanner(String zapHost, int zapPort, String apiKey, 
                             String targetUrl, String contextName, int timeoutInSeconds) {
        this.zapClient = new ClientApi(zapHost, zapPort, apiKey);
        this.targetUrl = targetUrl;
        this.contextName = contextName;
        this.apiKey = apiKey;
        this.timeoutInSeconds = timeoutInSeconds;
        this.scannerConfig = new HashMap<>();
        
        // Default scanner configuration
        scannerConfig.put("maxChildren", "10");
        scannerConfig.put("recurse", "true");
        scannerConfig.put("contextName", contextName);
        scannerConfig.put("subtreeOnly", "false");
        
        logger.info("ZAP security scanner initialized for target: {}", targetUrl);
    }
    
    /**
     * Set ZAP scanner configuration parameters
     * @param config Map of configuration parameters
     */
    public void setConfiguration(Map<String, String> config) {
        scannerConfig.putAll(config);
        logger.info("Updated ZAP scanner configuration: {}", scannerConfig);
    }
    
    /**
     * Initialize a new ZAP session
     * @return true if session was successfully initialized
     * @throws ClientApiException if there's an error communicating with ZAP
     */
    @Step("Initialize ZAP session")
    public boolean initializeSession() throws ClientApiException {
        try {
            zapClient.core.newSession(apiKey, "", "true");
            zapClient.context.newContext(apiKey, contextName);
            zapClient.context.includeInContext(apiKey, contextName, ".*" + targetUrl + ".*");
            logger.info("ZAP session initialized for context: {}", contextName);
            return true;
        } catch (ClientApiException e) {
            logger.error("Failed to initialize ZAP session", e);
            throw e;
        }
    }
    
    /**
     * Run a passive scan on the target URL
     * @param includeUrls List of URLs to include in the scan (supports regex patterns)
     * @throws ClientApiException if there's an error communicating with ZAP
     * @throws InterruptedException if the scan is interrupted
     */
    @Step("Run passive security scan")
    public void runPassiveScan(List<String> includeUrls) throws ClientApiException, InterruptedException {
        logger.info("Starting passive scan for target: {}", targetUrl);
        
        // Access the target URL through ZAP proxy to trigger passive scanning
        zapClient.core.accessUrl(apiKey, targetUrl, "true");
        
        // Add additional URLs if provided
        if (includeUrls != null) {
            for (String url : includeUrls) {
                zapClient.core.accessUrl(apiKey, url, "true");
                logger.info("Added URL to passive scan: {}", url);
            }
        }
        
        // Wait for passive scanner to complete
        waitForPassiveScanCompletion();
        logger.info("Passive scan completed");
    }
    
    /**
     * Run a spider scan on the target URL
     * @return ID of the spider scan
     * @throws ClientApiException if there's an error communicating with ZAP
     * @throws InterruptedException if the scan is interrupted
     */
    @Step("Run spider scan")
    public int runSpiderScan() throws ClientApiException, InterruptedException {
        logger.info("Starting spider scan for target: {}", targetUrl);
        
        Spider spider = zapClient.spider;
        
        // Start the spider scan
        ApiResponse response = spider.scan(apiKey, targetUrl, null, 
                scannerConfig.get("maxChildren"), scannerConfig.get("recurse"), 
                scannerConfig.get("contextName"), scannerConfig.get("subtreeOnly"));
        
        int scanId = extractScanId(response);
        logger.info("Spider scan started with ID: {}", scanId);
        
        // Wait for spider scan to complete
        int progress;
        do {
            TimeUnit.SECONDS.sleep(2);
            progress = Integer.parseInt(((ApiResponseElement)spider.status(apiKey, Integer.toString(scanId))).getValue());
            logger.info("Spider scan progress: {}%", progress);
        } while (progress < 100);
        
        logger.info("Spider scan completed");
        return scanId;
    }
    
    /**
     * Run an active scan on the target URL
     * @return ID of the active scan
     * @throws ClientApiException if there's an error communicating with ZAP
     * @throws InterruptedException if the scan is interrupted
     */
    @Step("Run active security scan")
    public int runActiveScan() throws ClientApiException, InterruptedException {
        logger.info("Starting active scan for target: {}", targetUrl);
        
        Ascan ascan = zapClient.ascan;
        
        // Start the active scan
        ApiResponse response = ascan.scan(apiKey, targetUrl, "true", "false", 
                null, null, null);
        
        int scanId = extractScanId(response);
        logger.info("Active scan started with ID: {}", scanId);
        
        // Wait for active scan to complete (with timeout)
        int progress;
        int timeWaited = 0;
        do {
            TimeUnit.SECONDS.sleep(5);
            timeWaited += 5;
            progress = Integer.parseInt(((ApiResponseElement)ascan.status(apiKey, Integer.toString(scanId))).getValue());
            logger.info("Active scan progress: {}%", progress);
            
            if (timeWaited > timeoutInSeconds) {
                logger.warn("Active scan timeout reached after {} seconds", timeoutInSeconds);
                break;
            }
        } while (progress < 100);
        
        logger.info("Active scan completed or timeout reached");
        return scanId;
    }
    
    /**
     * Wait for passive scanner to complete
     * @throws ClientApiException if there's an error communicating with ZAP
     * @throws InterruptedException if the wait is interrupted
     */
    private void waitForPassiveScanCompletion() throws ClientApiException, InterruptedException {
        logger.info("Waiting for passive scan completion...");
        int recordsToScan;
        int timeWaited = 0;
        
        do {
            TimeUnit.SECONDS.sleep(2);
            timeWaited += 2;
            ApiResponse response = zapClient.pscan.recordsToScan(apiKey);
            recordsToScan = Integer.parseInt(((ApiResponseElement)response).getValue());
            logger.info("Records left to scan: {}", recordsToScan);
            
            if (timeWaited > timeoutInSeconds) {
                logger.warn("Passive scan timeout reached after {} seconds", timeoutInSeconds);
                break;
            }
        } while (recordsToScan > 0);
    }
    
    /**
     * Get all security alerts found by ZAP
     * @param riskLevel Minimum risk level to include (null for all alerts)
     * @return List of security alerts
     * @throws ClientApiException if there's an error communicating with ZAP
     */
    @Step("Get security alerts")
    public List<SecurityAlert> getAlerts(AlertRiskLevel riskLevel) throws ClientApiException {
        Alert alert = zapClient.getAlert();
        
        // Get all alerts
        ApiResponse response = alert.alerts(apiKey, targetUrl, 0, -1, null);
        
        // Convert ApiResponse to SecurityAlert objects
        List<SecurityAlert> alerts = new ArrayList<>();
        if (response instanceof ApiResponseList) {
            ApiResponseList alertList = (ApiResponseList) response;
            for (ApiResponse apiResp : alertList.getItems()) {
                if (apiResp instanceof ApiResponseSet) {
                    SecurityAlert secAlert = new SecurityAlert((ApiResponseSet<?>) apiResp);
                    
                    // Filter by risk level if specified
                    if (riskLevel == null || 
                            secAlert.getRiskLevel().getLevel() >= riskLevel.getLevel()) {
                        alerts.add(secAlert);
                    }
                }
            }
        }
        
        logger.info("Retrieved {} security alerts", alerts.size());
        return alerts;
    }
    
    /**
     * Generate an HTML report of security issues
     * @return The HTML report content
     * @throws ClientApiException if there's an error communicating with ZAP
     */
    @Attachment(value = "ZAP Security Report", type = "text/html")
    @Step("Generate security report")
    public byte[] generateHtmlReport() throws ClientApiException {
        logger.info("Generating HTML security report");
        byte[] report = zapClient.core.htmlreport(apiKey);
        logger.info("HTML report generated, size: {} bytes", report.length);
        return report;
    }
    
    /**
     * Save the HTML report to a file
     * @param filePath Path to save the report to
     * @throws Exception if there's an error saving the report
     */
    @Step("Save security report to file")
    public void saveHtmlReportToFile(String filePath) throws Exception {
        logger.info("Saving HTML report to file: {}", filePath);
        byte[] report = generateHtmlReport();
        Files.write(Paths.get(filePath), report);
        logger.info("Report saved successfully");
    }
    
    /**
     * Extract scan ID from ZAP API response
     * @param response ZAP API response
     * @return The scan ID as an integer
     */
    private int extractScanId(ApiResponse response) {
        return Integer.parseInt(((ApiResponseElement)response).getValue());
    }
    
    /**
     * Close the ZAP session
     * @throws ClientApiException if there's an error communicating with ZAP
     */
    public void closeSession() throws ClientApiException {
        logger.info("Closing ZAP session");
        zapClient.core.shutdown(apiKey);
    }
}
