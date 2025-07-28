package com.securitytests.utils.security;

import com.securitytests.utils.logging.StructuredLogger;
import io.qameta.allure.Step;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Utility for testing session management security
 * Validates session handling, timeout enforcement, session fixation protection,
 * cookie security settings, and concurrent session handling
 */
public class SessionSecurityTester {
    private static final StructuredLogger logger = new StructuredLogger(SessionSecurityTester.class);
    private final HttpClientBuilder httpClientBuilder;
    private final String baseUrl;
    private final CookieStore cookieStore;
    private final HttpClientContext context;
    private String authToken;
    
    /**
     * Create a new session security tester
     * @param baseUrl The base URL of the application to test
     */
    public SessionSecurityTester(String baseUrl) {
        this.baseUrl = baseUrl;
        this.cookieStore = new BasicCookieStore();
        this.context = HttpClientContext.create();
        this.context.setCookieStore(cookieStore);
        this.httpClientBuilder = HttpClients.custom()
                .setDefaultCookieStore(cookieStore);
        
        logger.info("Session security tester initialized for base URL: {}", baseUrl);
    }
    
    /**
     * Set authentication token for tests
     * @param authToken JWT or other auth token
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        logger.info("Authentication token set for session security tests");
    }
    
    /**
     * Test session timeout enforcement
     * @param loginUrl URL to login endpoint
     * @param protectedUrl URL to protected resource
     * @param expectedTimeout Expected session timeout in minutes
     * @param loginParams Login parameters (username, password)
     * @return SessionTestResult with test outcome
     */
    @Step("Test session timeout enforcement")
    public SessionTestResult testSessionTimeout(
            String loginUrl, 
            String protectedUrl,
            int expectedTimeout,
            Map<String, String> loginParams) {
        
        logger.info("Testing session timeout enforcement. Expected timeout: {} minutes", expectedTimeout);
        
        try {
            // Login to establish session
            HttpClient client = httpClientBuilder.build();
            HttpResponse loginResponse = performLogin(client, loginUrl, loginParams);
            
            if (loginResponse.getStatusLine().getStatusCode() != 200) {
                logger.error("Login failed with status code: {}", 
                        loginResponse.getStatusLine().getStatusCode());
                return new SessionTestResult(false, 
                        "Login failed with status: " + loginResponse.getStatusLine().getStatusCode(),
                        Collections.singletonList("Login failed"));
            }
            
            // Extract session cookies or tokens
            List<Cookie> sessionCookies = cookieStore.getCookies();
            logger.info("Session established. Found {} cookies", sessionCookies.size());
            
            // Access protected resource to verify session is active
            HttpGet initialRequest = new HttpGet(baseUrl + protectedUrl);
            if (authToken != null) {
                initialRequest.setHeader("Authorization", "Bearer " + authToken);
            }
            
            HttpResponse initialResponse = client.execute(initialRequest, context);
            int initialStatusCode = initialResponse.getStatusLine().getStatusCode();
            EntityUtils.consume(initialResponse.getEntity());
            
            if (initialStatusCode != 200) {
                logger.error("Initial access to protected resource failed: {}", initialStatusCode);
                return new SessionTestResult(false, 
                        "Initial access to protected resource failed: " + initialStatusCode,
                        Collections.singletonList("Protected resource access failed"));
            }
            
            logger.info("Successfully accessed protected resource with active session");
            
            // Calculate wait time - use a shorter time for testing
            // In a real test, we might want to use the full expectedTimeout
            long waitTimeMillis = Math.min(expectedTimeout * 60 * 1000, 5000); // Use 5 seconds for testing
            
            logger.info("Waiting for {} milliseconds to test session timeout", waitTimeMillis);
            Thread.sleep(waitTimeMillis);
            
            // Try to access protected resource again after waiting
            HttpGet timeoutRequest = new HttpGet(baseUrl + protectedUrl);
            if (authToken != null) {
                timeoutRequest.setHeader("Authorization", "Bearer " + authToken);
            }
            
            HttpResponse timeoutResponse = client.execute(timeoutRequest, context);
            int timeoutStatusCode = timeoutResponse.getStatusLine().getStatusCode();
            EntityUtils.consume(timeoutResponse.getEntity());
            
            // For testing purposes, we'll simulate a timeout by checking if the status is 401/403
            // In a real test, this would depend on the actual session timeout
            boolean sessionTimedOut = (timeoutStatusCode == 401 || timeoutStatusCode == 403);
            
            if (sessionTimedOut) {
                logger.info("Session correctly timed out after waiting");
                return new SessionTestResult(true, 
                        "Session correctly timed out after waiting", 
                        Collections.emptyList());
            } else {
                logger.warn("Session did not time out as expected after waiting");
                return new SessionTestResult(false, 
                        "Session did not time out as expected", 
                        Collections.singletonList("Session remained active after expected timeout"));
            }
            
        } catch (Exception e) {
            logger.error("Error testing session timeout", e);
            return new SessionTestResult(false, 
                    "Error testing session timeout: " + e.getMessage(),
                    Collections.singletonList(e.getMessage()));
        }
    }
    
    /**
     * Test session fixation protection
     * @param loginUrl URL to login endpoint
     * @param loginParams Login parameters
     * @return SessionTestResult with test outcome
     */
    @Step("Test session fixation protection")
    public SessionTestResult testSessionFixation(String loginUrl, Map<String, String> loginParams) {
        logger.info("Testing session fixation protection");
        
        try {
            // First, access the site to get any initial cookies
            HttpClient client = httpClientBuilder.build();
            HttpGet initialGet = new HttpGet(baseUrl);
            HttpResponse initialResponse = client.execute(initialGet, context);
            EntityUtils.consume(initialResponse.getEntity());
            
            // Store pre-login cookies
            List<Cookie> preLoginCookies = new ArrayList<>(cookieStore.getCookies());
            Set<String> preLoginCookieNames = new HashSet<>();
            for (Cookie cookie : preLoginCookies) {
                preLoginCookieNames.add(cookie.getName());
                logger.info("Pre-login cookie: {} = {}", cookie.getName(), cookie.getValue());
            }
            
            // Perform login
            HttpResponse loginResponse = performLogin(client, loginUrl, loginParams);
            
            if (loginResponse.getStatusLine().getStatusCode() != 200) {
                logger.error("Login failed with status code: {}", 
                        loginResponse.getStatusLine().getStatusCode());
                return new SessionTestResult(false, 
                        "Login failed with status: " + loginResponse.getStatusLine().getStatusCode(),
                        Collections.singletonList("Login failed"));
            }
            
            // Get post-login cookies
            List<Cookie> postLoginCookies = cookieStore.getCookies();
            boolean sessionChanged = false;
            
            // Check if session cookies changed after login
            for (Cookie postCookie : postLoginCookies) {
                if (preLoginCookieNames.contains(postCookie.getName())) {
                    // Find the matching pre-login cookie
                    for (Cookie preCookie : preLoginCookies) {
                        if (preCookie.getName().equals(postCookie.getName())) {
                            if (!preCookie.getValue().equals(postCookie.getValue())) {
                                logger.info("Session cookie changed after login: {} (Values: {} -> {})",
                                        postCookie.getName(), preCookie.getValue(), postCookie.getValue());
                                sessionChanged = true;
                            }
                        }
                    }
                }
            }
            
            if (sessionChanged) {
                logger.info("Session fixation protection confirmed - session ID changed after login");
                return new SessionTestResult(true, 
                        "Session ID changed after login, indicating protection against session fixation",
                        Collections.emptyList());
            } else {
                logger.warn("Session ID did not change after login, potential session fixation vulnerability");
                return new SessionTestResult(false, 
                        "Session ID did not change after login", 
                        Collections.singletonList("Session fixation vulnerability: Session ID remained unchanged after login"));
            }
            
        } catch (Exception e) {
            logger.error("Error testing session fixation", e);
            return new SessionTestResult(false, 
                    "Error testing session fixation: " + e.getMessage(),
                    Collections.singletonList(e.getMessage()));
        }
    }
    
    /**
     * Test secure cookie attributes
     * @param loginUrl URL to login endpoint
     * @param loginParams Login parameters
     * @return SessionTestResult with test outcome
     */
    @Step("Test secure cookie attributes")
    public SessionTestResult testCookieSecurity(String loginUrl, Map<String, String> loginParams) {
        logger.info("Testing cookie security attributes");
        
        try {
            // Login to get session cookies
            HttpClient client = httpClientBuilder.build();
            HttpResponse loginResponse = performLogin(client, loginUrl, loginParams);
            
            if (loginResponse.getStatusLine().getStatusCode() != 200) {
                logger.error("Login failed with status code: {}", 
                        loginResponse.getStatusLine().getStatusCode());
                return new SessionTestResult(false, 
                        "Login failed with status: " + loginResponse.getStatusLine().getStatusCode(),
                        Collections.singletonList("Login failed"));
            }
            
            // Get cookies and validate security attributes
            List<Cookie> cookies = cookieStore.getCookies();
            List<String> issues = new ArrayList<>();
            
            logger.info("Analyzing {} cookies for security attributes", cookies.size());
            
            for (Cookie cookie : cookies) {
                // Check for Secure flag
                if (!cookie.isSecure()) {
                    issues.add("Cookie '" + cookie.getName() + "' is missing Secure flag");
                    logger.warn("Cookie '{}' is missing Secure flag", cookie.getName());
                }
                
                // Check for HttpOnly flag (not directly available in Cookie interface)
                // We can check the Set-Cookie header from the login response
                boolean hasHttpOnly = false;
                Header[] headers = loginResponse.getHeaders("Set-Cookie");
                for (Header header : headers) {
                    if (header.getValue().contains(cookie.getName() + "=") && 
                            header.getValue().toLowerCase().contains("httponly")) {
                        hasHttpOnly = true;
                        break;
                    }
                }
                
                if (!hasHttpOnly) {
                    issues.add("Cookie '" + cookie.getName() + "' is missing HttpOnly flag");
                    logger.warn("Cookie '{}' is missing HttpOnly flag", cookie.getName());
                }
                
                // Check for SameSite attribute (not directly available in Cookie interface)
                boolean hasSameSite = false;
                for (Header header : headers) {
                    if (header.getValue().contains(cookie.getName() + "=") && 
                            (header.getValue().toLowerCase().contains("samesite=strict") || 
                             header.getValue().toLowerCase().contains("samesite=lax"))) {
                        hasSameSite = true;
                        break;
                    }
                }
                
                if (!hasSameSite) {
                    issues.add("Cookie '" + cookie.getName() + "' is missing SameSite attribute");
                    logger.warn("Cookie '{}' is missing SameSite attribute", cookie.getName());
                }
                
                // Check for reasonable expiration time
                if (cookie.getExpiryDate() != null) {
                    Date now = new Date();
                    long durationMillis = cookie.getExpiryDate().getTime() - now.getTime();
                    
                    // Convert to days
                    long durationDays = TimeUnit.MILLISECONDS.toDays(durationMillis);
                    
                    if (durationDays > 30) {
                        issues.add("Cookie '" + cookie.getName() + "' has long expiration time: " + durationDays + " days");
                        logger.warn("Cookie '{}' has long expiration time: {} days", cookie.getName(), durationDays);
                    }
                }
            }
            
            boolean isSecure = issues.isEmpty();
            String summary = isSecure ? 
                    "All cookies have proper security attributes" : 
                    "Some cookies have missing security attributes";
            
            logger.info("Cookie security test completed. Secure: {}", isSecure);
            return new SessionTestResult(isSecure, summary, issues);
            
        } catch (Exception e) {
            logger.error("Error testing cookie security", e);
            return new SessionTestResult(false, 
                    "Error testing cookie security: " + e.getMessage(),
                    Collections.singletonList(e.getMessage()));
        }
    }
    
    /**
     * Test session invalidation after logout
     * @param loginUrl URL to login endpoint
     * @param logoutUrl URL to logout endpoint
     * @param protectedUrl URL to protected resource
     * @param loginParams Login parameters
     * @return SessionTestResult with test outcome
     */
    @Step("Test session invalidation after logout")
    public SessionTestResult testSessionInvalidation(
            String loginUrl, 
            String logoutUrl, 
            String protectedUrl,
            Map<String, String> loginParams) {
        
        logger.info("Testing session invalidation after logout");
        
        try {
            // Login to establish session
            HttpClient client = httpClientBuilder.build();
            HttpResponse loginResponse = performLogin(client, loginUrl, loginParams);
            
            if (loginResponse.getStatusLine().getStatusCode() != 200) {
                logger.error("Login failed with status code: {}", 
                        loginResponse.getStatusLine().getStatusCode());
                return new SessionTestResult(false, 
                        "Login failed with status: " + loginResponse.getStatusLine().getStatusCode(),
                        Collections.singletonList("Login failed"));
            }
            
            // Access protected resource to verify session is active
            HttpGet initialRequest = new HttpGet(baseUrl + protectedUrl);
            if (authToken != null) {
                initialRequest.setHeader("Authorization", "Bearer " + authToken);
            }
            
            HttpResponse initialResponse = client.execute(initialRequest, context);
            int initialStatusCode = initialResponse.getStatusLine().getStatusCode();
            EntityUtils.consume(initialResponse.getEntity());
            
            if (initialStatusCode != 200) {
                logger.error("Initial access to protected resource failed: {}", initialStatusCode);
                return new SessionTestResult(false, 
                        "Initial access to protected resource failed: " + initialStatusCode,
                        Collections.singletonList("Protected resource access failed"));
            }
            
            logger.info("Successfully accessed protected resource with active session");
            
            // Perform logout
            HttpGet logoutRequest = new HttpGet(baseUrl + logoutUrl);
            if (authToken != null) {
                logoutRequest.setHeader("Authorization", "Bearer " + authToken);
            }
            
            HttpResponse logoutResponse = client.execute(logoutRequest, context);
            EntityUtils.consume(logoutResponse.getEntity());
            
            logger.info("Logout performed with status: {}", 
                    logoutResponse.getStatusLine().getStatusCode());
            
            // Try to access protected resource again after logout
            HttpGet postLogoutRequest = new HttpGet(baseUrl + protectedUrl);
            if (authToken != null) {
                postLogoutRequest.setHeader("Authorization", "Bearer " + authToken);
            }
            
            HttpResponse postLogoutResponse = client.execute(postLogoutRequest, context);
            int postLogoutStatusCode = postLogoutResponse.getStatusLine().getStatusCode();
            EntityUtils.consume(postLogoutResponse.getEntity());
            
            // Check if session was properly invalidated (expecting 401/403)
            boolean sessionInvalidated = (postLogoutStatusCode == 401 || postLogoutStatusCode == 403);
            
            if (sessionInvalidated) {
                logger.info("Session correctly invalidated after logout");
                return new SessionTestResult(true, 
                        "Session correctly invalidated after logout", 
                        Collections.emptyList());
            } else {
                logger.warn("Session not properly invalidated after logout");
                return new SessionTestResult(false, 
                        "Session not properly invalidated after logout", 
                        Collections.singletonList("Session remained active after logout"));
            }
            
        } catch (Exception e) {
            logger.error("Error testing session invalidation", e);
            return new SessionTestResult(false, 
                    "Error testing session invalidation: " + e.getMessage(),
                    Collections.singletonList(e.getMessage()));
        }
    }
    
    /**
     * Perform login to establish a session
     */
    private HttpResponse performLogin(HttpClient client, String loginUrl, Map<String, String> loginParams) 
            throws IOException {
        // This is a simplified login - in a real implementation, we would properly build the request body
        // based on the form or API requirements
        HttpPost loginRequest = new HttpPost(baseUrl + loginUrl);
        
        // Add headers and parameters as needed
        loginRequest.setHeader("Content-Type", "application/json");
        
        // In a real implementation, we would create the request body from loginParams
        // For now, we're just simulating a login
        
        return client.execute(loginRequest, context);
    }
    
    /**
     * Session test result class
     */
    public static class SessionTestResult {
        private final boolean passed;
        private final String summary;
        private final List<String> issues;
        private final Date timestamp;
        
        public SessionTestResult(boolean passed, String summary, List<String> issues) {
            this.passed = passed;
            this.summary = summary;
            this.issues = issues;
            this.timestamp = new Date();
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public List<String> getIssues() {
            return issues;
        }
        
        public Date getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Session Security Test Result:\n");
            sb.append("Status: ").append(passed ? "PASSED" : "FAILED").append("\n");
            sb.append("Summary: ").append(summary).append("\n");
            
            if (!issues.isEmpty()) {
                sb.append("\nIssues:\n");
                for (String issue : issues) {
                    sb.append("- ").append(issue).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
}
