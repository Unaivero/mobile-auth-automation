package com.securitytests.utils;

import com.mailslurp.apis.InboxControllerApi;
import com.mailslurp.apis.WaitForControllerApi;
import com.mailslurp.client.ApiClient;
import com.mailslurp.client.ApiException;
import com.mailslurp.client.Configuration;
import com.mailslurp.models.Email;
import com.mailslurp.models.Inbox;
import com.securitytests.config.AppiumConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to handle email verification using MailSlurp API
 */
public class MailSlurpUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailSlurpUtil.class);
    private static final String API_KEY = AppiumConfig.getProperty("mailslurp.api.key");
    private static final long TIMEOUT = Long.parseLong(AppiumConfig.getProperty("mailslurp.timeout.millis"));
    
    private final ApiClient apiClient;
    private final InboxControllerApi inboxApi;
    private final WaitForControllerApi waitForApi;
    
    private UUID inboxId;
    private String emailAddress;
    
    /**
     * Constructor initializing MailSlurp API client
     */
    public MailSlurpUtil() {
        apiClient = Configuration.getDefaultApiClient();
        apiClient.setApiKey(API_KEY);
        
        inboxApi = new InboxControllerApi(apiClient);
        waitForApi = new WaitForControllerApi(apiClient);
    }
    
    /**
     * Creates a new email inbox
     * @return Email address of the created inbox
     */
    public String createInbox() {
        try {
            Inbox inbox = inboxApi.createInbox(null, null, null, null, null, null, null, null, null);
            inboxId = inbox.getId();
            emailAddress = inbox.getEmailAddress();
            LOGGER.info("Created inbox with ID: {} and email: {}", inboxId, emailAddress);
            return emailAddress;
        } catch (ApiException e) {
            LOGGER.error("Failed to create inbox", e);
            throw new RuntimeException("Failed to create MailSlurp inbox", e);
        }
    }
    
    /**
     * Waits for and retrieves the password reset email
     * @return Email object
     */
    public Email waitForPasswordResetEmail() {
        if (inboxId == null) {
            throw new IllegalStateException("Inbox not created. Call createInbox() first");
        }
        
        try {
            LOGGER.info("Waiting for password reset email in inbox: {}", inboxId);
            OffsetDateTime now = OffsetDateTime.now();
            List<Email> emails = waitForApi.waitForLatestEmail(inboxId, TIMEOUT, true, now, null, 1, null, null);
            
            if (emails != null && !emails.isEmpty()) {
                Email email = emails.get(0);
                LOGGER.info("Received email with subject: {}", email.getSubject());
                return email;
            } else {
                LOGGER.error("No emails received within timeout period");
                throw new RuntimeException("No password reset email received");
            }
        } catch (ApiException e) {
            LOGGER.error("Error waiting for email", e);
            throw new RuntimeException("Failed to retrieve password reset email", e);
        }
    }
    
    /**
     * Extracts the password reset token from the email body
     * @param email Email object
     * @return Extracted token
     */
    public String extractResetToken(Email email) {
        String emailBody = email.getBody();
        
        // Define the pattern to search for (adjust based on actual email format)
        Pattern pattern = Pattern.compile("reset code[:\\s]+(\\d{6})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(emailBody);
        
        if (matcher.find()) {
            String token = matcher.group(1);
            LOGGER.info("Found reset token: {}", token);
            return token;
        } else {
            // Try alternative pattern
            pattern = Pattern.compile("verification code[:\\s]+(\\d{6})", Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(emailBody);
            
            if (matcher.find()) {
                String token = matcher.group(1);
                LOGGER.info("Found verification code: {}", token);
                return token;
            }
            
            LOGGER.error("Reset token not found in email body");
            throw new RuntimeException("Reset token not found in email");
        }
    }
    
    /**
     * Complete workflow to get password reset token
     * @return The reset token
     */
    public String getPasswordResetToken() {
        Email email = waitForPasswordResetEmail();
        return extractResetToken(email);
    }
    
    /**
     * Gets the current inbox email address
     * @return The email address
     */
    public String getEmailAddress() {
        return emailAddress;
    }
}
