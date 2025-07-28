package com.securitytests.utils;

import com.securitytests.config.AppiumConfig;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to setup and manage mock server for simulating backend endpoints
 */
public class MockServerUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockServerUtil.class);
    private static ClientAndServer mockServer;
    
    private static final String HOST = AppiumConfig.getProperty("mock.server.host");
    private static final int PORT = Integer.parseInt(AppiumConfig.getProperty("mock.server.port"));
    
    /**
     * Start the mock server
     */
    public static void startServer() {
        if (mockServer == null || !mockServer.isRunning()) {
            LOGGER.info("Starting mock server on port: {}", PORT);
            mockServer = ClientAndServer.startClientAndServer(PORT);
            setupEndpoints();
        }
    }
    
    /**
     * Stop the mock server
     */
    public static void stopServer() {
        if (mockServer != null && mockServer.isRunning()) {
            LOGGER.info("Stopping mock server");
            mockServer.stop();
        }
    }
    
    /**
     * Setup mock endpoints
     */
    private static void setupEndpoints() {
        setupCaptchaVerificationEndpoint();
        setupPasswordResetEndpoint();
        setupTokenVerificationEndpoint();
    }
    
    /**
     * Setup CAPTCHA verification endpoint
     */
    private static void setupCaptchaVerificationEndpoint() {
        new MockServerClient(HOST, PORT)
            .when(
                request()
                    .withMethod("POST")
                    .withPath("/api/auth/verify-captcha")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeaders(
                        new Header("Content-Type", "application/json; charset=utf-8")
                    )
                    .withBody("{\"success\": true, \"message\": \"CAPTCHA verified successfully\"}")
                    .withDelay(org.mockserver.model.Delay.milliseconds(500))
            );
        
        LOGGER.info("Setup CAPTCHA verification endpoint: POST /api/auth/verify-captcha");
    }
    
    /**
     * Setup password reset request endpoint
     */
    private static void setupPasswordResetEndpoint() {
        new MockServerClient(HOST, PORT)
            .when(
                request()
                    .withMethod("POST")
                    .withPath("/api/auth/reset-password-request")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeaders(
                        new Header("Content-Type", "application/json; charset=utf-8")
                    )
                    .withBody("{\"success\": true, \"message\": \"Password reset email sent\"}")
                    .withDelay(org.mockserver.model.Delay.milliseconds(500))
            );
        
        LOGGER.info("Setup password reset request endpoint: POST /api/auth/reset-password-request");
    }
    
    /**
     * Setup token verification endpoint
     */
    private static void setupTokenVerificationEndpoint() {
        new MockServerClient(HOST, PORT)
            .when(
                request()
                    .withMethod("POST")
                    .withPath("/api/auth/verify-reset-token")
            )
            .respond(
                response()
                    .withStatusCode(200)
                    .withHeaders(
                        new Header("Content-Type", "application/json; charset=utf-8")
                    )
                    .withBody("{\"success\": true, \"message\": \"Token verified successfully\", \"tokenExpired\": false}")
                    .withDelay(org.mockserver.model.Delay.milliseconds(500))
            );
        
        LOGGER.info("Setup token verification endpoint: POST /api/auth/verify-reset-token");
    }
}
