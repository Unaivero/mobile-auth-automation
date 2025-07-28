package com.securitytests.security;

import com.securitytests.utils.logging.StructuredLogger;
import com.securitytests.utils.security.*;
import io.qameta.allure.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.util.*;

/**
 * Comprehensive security tests for authentication endpoints
 */
@Epic("Authentication Security")
@Feature("Security Testing")
public class AuthSecurityTests {
    private static final StructuredLogger logger = new StructuredLogger(AuthSecurityTests.class);
    private String baseUrl;
    private String apiBaseUrl;
    private ZAPSecurityScanner zapScanner;
    private SecurityHeadersAnalyzer headersAnalyzer;
    private JWTSecurityAnalyzer jwtAnalyzer;
    private SessionSecurityTester sessionTester;
    private String testUsername;
    private String testPassword;
    private String authToken;
    
    @BeforeClass
    public void setup() {
        // Get configuration from properties
        baseUrl = System.getProperty("test.baseUrl", "https://example.com");
        apiBaseUrl = System.getProperty("test.apiBaseUrl", "https://api.example.com");
        String zapHost = System.getProperty("zap.host", "localhost");
        int zapPort = Integer.parseInt(System.getProperty("zap.port", "8080"));
        String zapApiKey = System.getProperty("zap.apiKey", "");
        testUsername = System.getProperty("test.username", "test@example.com");
        testPassword = System.getProperty("test.password", "Password123!");
        
        // Initialize security testing components
        zapScanner = new ZAPSecurityScanner(zapHost, zapPort, zapApiKey, 
                baseUrl, "auth_context", 300);
        headersAnalyzer = new SecurityHeadersAnalyzer();
        jwtAnalyzer = new JWTSecurityAnalyzer();
        sessionTester = new SessionSecurityTester(baseUrl);
        
        logger.info("Security tests initialized with base URL: {}", baseUrl);
    }
    
    @BeforeMethod
    public void setupTest() {
        String testId = logger.startTest("Security Test");
        logger.info("Starting security test with ID: {}", testId);
    }
    
    @AfterMethod
    public void tearDown() {
        logger.endTest("complete");
    }
    
    /**
     * Test security headers in API responses
     */
    @Test(description = "Verify security headers in API responses")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Security Headers Analysis")
    public void testSecurityHeaders() throws Exception {
        logger.info("Testing security headers in API responses");
        
        // Create HTTP client
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // Test login endpoint
            HttpGet request = new HttpGet(apiBaseUrl + "/auth/status");
            HttpResponse response = client.execute(request);
            
            // Analyze security headers
            SecurityHeadersAnalyzer.SecurityHeadersReport report = headersAnalyzer.analyzeHeaders(response);
            
            // Attach report to Allure
            Allure.addAttachment("Security Headers Report", report.generateHtmlReport());
            
            // Log findings
            logger.info("Security headers analysis completed with score: {}/100 (Grade {})",
                    report.getSecurityScore(), report.getSecurityGrade());
            
            if (report.hasIssues()) {
                for (Map.Entry<String, List<String>> entry : report.getIssues().entrySet()) {
                    logger.warn("Security header issue: {} - {}", entry.getKey(), entry.getValue());
                }
            }
            
            // Validate no critical security header issues
            Assert.assertFalse(report.hasCriticalIssues(),
                    "Critical security header issues found");
            
            // Security score should be at least 70 (Grade C)
            Assert.assertTrue(report.getSecurityScore() >= 70,
                    "Security headers score too low: " + report.getSecurityScore());
        }
    }
    
    /**
     * Test JWT token security
     */
    @Test(description = "Verify JWT token security")
    @Severity(SeverityLevel.CRITICAL)
    @Story("JWT Security Analysis")
    public void testJWTTokenSecurity() throws Exception {
        logger.info("Testing JWT token security");
        
        // First obtain a JWT token via login
        String token = loginAndGetToken();
        Assert.assertNotNull(token, "Failed to obtain JWT token");
        
        // Analyze token security
        JWTSecurityAnalyzer.JWTAnalysisResult analysis = jwtAnalyzer.analyzeToken(token);
        
        // Attach analysis to Allure
        Allure.addAttachment("JWT Security Analysis", analysis.toString());
        
        // Log findings
        logger.info("JWT security analysis completed: {}", analysis.getSummary());
        if (!analysis.getIssues().isEmpty()) {
            for (String issue : analysis.getIssues()) {
                logger.warn("JWT security issue: {}", issue);
            }
        }
        
        // Validate JWT token security
        Assert.assertTrue(analysis.isSecure(),
                "JWT token has security issues: " + analysis.getIssues());
    }
    
    /**
     * Test session security and management
     */
    @Test(description = "Verify session security management")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Session Security Testing")
    public void testSessionSecurity() throws Exception {
        logger.info("Testing session security management");
        
        // Prepare login parameters
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("username", testUsername);
        loginParams.put("password", testPassword);
        
        // Test session invalidation after logout
        SessionSecurityTester.SessionTestResult invalidationResult = 
                sessionTester.testSessionInvalidation(
                        "/login",
                        "/logout",
                        "/profile",
                        loginParams);
        
        // Attach result to Allure
        Allure.addAttachment("Session Invalidation Test", invalidationResult.toString());
        
        // Test cookie security attributes
        SessionSecurityTester.SessionTestResult cookieResult = 
                sessionTester.testCookieSecurity("/login", loginParams);
        
        // Attach result to Allure
        Allure.addAttachment("Cookie Security Test", cookieResult.toString());
        
        // Test session fixation
        SessionSecurityTester.SessionTestResult fixationResult = 
                sessionTester.testSessionFixation("/login", loginParams);
        
        // Attach result to Allure
        Allure.addAttachment("Session Fixation Test", fixationResult.toString());
        
        // Validate session security
        Assert.assertTrue(invalidationResult.isPassed(),
                "Session invalidation test failed: " + invalidationResult.getSummary());
        Assert.assertTrue(cookieResult.isPassed(),
                "Cookie security test failed: " + cookieResult.getSummary());
        Assert.assertTrue(fixationResult.isPassed(),
                "Session fixation test failed: " + fixationResult.getSummary());
    }
    
    /**
     * Run OWASP ZAP security scan against auth endpoints
     */
    @Test(description = "Run OWASP ZAP security scan", 
          timeOut = 600000) // 10-minute timeout for this long-running test
    @Severity(SeverityLevel.BLOCKER)
    @Story("OWASP ZAP Security Scan")
    public void testOWASPSecurityScan() throws Exception {
        logger.info("Running OWASP ZAP security scan");
        
        try {
            // Initialize ZAP session
            zapScanner.initializeSession();
            
            // Define authentication URLs to scan
            List<String> authUrls = Arrays.asList(
                    baseUrl + "/login",
                    baseUrl + "/register",
                    baseUrl + "/password-reset",
                    baseUrl + "/verify-email");
            
            // Run passive scan
            zapScanner.runPassiveScan(authUrls);
            
            // Run spider scan
            int spiderScanId = zapScanner.runSpiderScan();
            logger.info("Spider scan completed with ID: {}", spiderScanId);
            
            // Run active scan
            int activeScanId = zapScanner.runActiveScan();
            logger.info("Active scan completed with ID: {}", activeScanId);
            
            // Get security alerts
            List<SecurityAlert> highAlerts = zapScanner.getAlerts(AlertRiskLevel.HIGH);
            List<SecurityAlert> mediumAlerts = zapScanner.getAlerts(AlertRiskLevel.MEDIUM);
            
            // Generate and save report
            String reportPath = "target/zap-security-report.html";
            zapScanner.saveHtmlReportToFile(reportPath);
            
            // Attach report to Allure
            byte[] reportBytes = zapScanner.generateHtmlReport();
            Allure.addAttachment("ZAP Security Scan Report", "text/html", new String(reportBytes), "html");
            
            // Log findings
            logger.info("ZAP security scan completed. Found {} high and {} medium risk alerts", 
                    highAlerts.size(), mediumAlerts.size());
            
            for (SecurityAlert alert : highAlerts) {
                logger.error("High risk security issue: {}", alert.getSummary());
            }
            
            for (SecurityAlert alert : mediumAlerts) {
                logger.warn("Medium risk security issue: {}", alert.getSummary());
            }
            
            // Validate no high-risk security issues
            Assert.assertTrue(highAlerts.isEmpty(),
                    "High risk security issues found: " + highAlerts.size());
            
            // Log report location
            logger.info("Security scan report saved to: {}", new File(reportPath).getAbsolutePath());
        } finally {
            // Close ZAP session
            zapScanner.closeSession();
        }
    }
    
    /**
     * Helper method to login and get JWT token
     */
    private String loginAndGetToken() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(apiBaseUrl + "/auth/login");
            request.setHeader("Content-Type", "application/json");
            
            JSONObject credentials = new JSONObject();
            credentials.put("username", testUsername);
            credentials.put("password", testPassword);
            
            StringEntity entity = new StringEntity(credentials.toString());
            request.setEntity(entity);
            
            HttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            
            if (response.getStatusLine().getStatusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                return jsonResponse.getString("token");
            } else {
                logger.error("Failed to login: {} - {}", 
                        response.getStatusLine().getStatusCode(), responseBody);
                return null;
            }
        }
    }
}
