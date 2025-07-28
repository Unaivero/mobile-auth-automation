package com.securitytests.utils.notification;

import com.securitytests.utils.logging.StructuredLogger;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending notifications about test results and performance issues
 */
public class NotificationService {
    private static final StructuredLogger logger = new StructuredLogger(NotificationService.class);
    private final Properties emailConfig;
    private final String slackWebhookUrl;
    private final String teamsWebhookUrl;
    private final Set<String> enabledChannels;
    
    /**
     * Create a new notification service with default configuration
     */
    public NotificationService() {
        // Initialize with values from configuration
        this.emailConfig = loadEmailConfig();
        this.slackWebhookUrl = System.getProperty("notification.slack.webhook");
        this.teamsWebhookUrl = System.getProperty("notification.teams.webhook");
        
        // Determine which channels are enabled
        this.enabledChannels = new HashSet<>();
        if (emailConfig.containsKey("mail.smtp.host")) {
            enabledChannels.add("email");
        }
        if (slackWebhookUrl != null && !slackWebhookUrl.isEmpty()) {
            enabledChannels.add("slack");
        }
        if (teamsWebhookUrl != null && !teamsWebhookUrl.isEmpty()) {
            enabledChannels.add("teams");
        }
        
        logger.info("Notification service initialized with channels: {}", enabledChannels);
    }
    
    /**
     * Load email configuration from system properties
     * @return Properties object with email configuration
     */
    private Properties loadEmailConfig() {
        Properties props = new Properties();
        
        // SMTP configuration
        String smtpHost = System.getProperty("notification.email.smtp.host");
        String smtpPort = System.getProperty("notification.email.smtp.port", "587");
        String smtpAuth = System.getProperty("notification.email.smtp.auth", "true");
        String smtpStartTls = System.getProperty("notification.email.smtp.starttls.enable", "true");
        
        if (smtpHost != null && !smtpHost.isEmpty()) {
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", smtpAuth);
            props.put("mail.smtp.starttls.enable", smtpStartTls);
        }
        
        return props;
    }
    
    /**
     * Send a test failure notification
     * @param subject Notification subject
     * @param message Notification message
     * @param recipients Email recipients (only used for email channel)
     * @param priority Notification priority (high, medium, low)
     * @param attachmentPaths Optional paths to files to attach to the notification
     * @return CompletableFuture that completes when notifications have been sent
     */
    public CompletableFuture<Void> sendTestFailureNotification(
            String subject,
            String message,
            List<String> recipients,
            NotificationPriority priority,
            String... attachmentPaths) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Sending test failure notification: {}", subject);
                
                // Send to all enabled channels
                for (String channel : enabledChannels) {
                    switch (channel) {
                        case "email":
                            sendEmailNotification(subject, message, recipients, priority, attachmentPaths);
                            break;
                        case "slack":
                            sendSlackNotification(subject, message, priority);
                            break;
                        case "teams":
                            sendTeamsNotification(subject, message, priority);
                            break;
                    }
                }
                
                logger.info("Test failure notification sent successfully");
            } catch (Exception e) {
                logger.error("Failed to send test failure notification: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Send a performance regression notification
     * @param subject Notification subject
     * @param message Notification message
     * @param metrics Performance metrics to include
     * @param recipients Email recipients (only used for email channel)
     * @param priority Notification priority
     * @param chartPath Optional path to performance chart image
     * @return CompletableFuture that completes when notifications have been sent
     */
    public CompletableFuture<Void> sendPerformanceRegressionNotification(
            String subject,
            String message,
            Map<String, Double> metrics,
            List<String> recipients,
            NotificationPriority priority,
            String chartPath) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Sending performance regression notification: {}", subject);
                
                // Add metrics to the message
                StringBuilder enhancedMessage = new StringBuilder(message);
                enhancedMessage.append("\n\nPerformance Metrics:\n");
                
                for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                    enhancedMessage.append("- ").append(entry.getKey())
                                  .append(": ").append(entry.getValue())
                                  .append("\n");
                }
                
                // Send to all enabled channels
                String[] attachments = chartPath != null ? new String[]{chartPath} : new String[0];
                
                for (String channel : enabledChannels) {
                    switch (channel) {
                        case "email":
                            sendEmailNotification(subject, enhancedMessage.toString(), recipients, priority, attachments);
                            break;
                        case "slack":
                            sendSlackNotification(subject, enhancedMessage.toString(), priority);
                            break;
                        case "teams":
                            sendTeamsNotification(subject, enhancedMessage.toString(), priority);
                            break;
                    }
                }
                
                logger.info("Performance regression notification sent successfully");
            } catch (Exception e) {
                logger.error("Failed to send performance regression notification: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Send daily test summary notification
     * @param subject Notification subject
     * @param summary Summary text
     * @param testResults Map of test names to results
     * @param recipients Email recipients
     * @return CompletableFuture that completes when notifications have been sent
     */
    public CompletableFuture<Void> sendDailySummaryNotification(
            String subject,
            String summary,
            Map<String, TestResult> testResults,
            List<String> recipients) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Sending daily summary notification");
                
                // Build the summary message
                StringBuilder messageBuilder = new StringBuilder(summary);
                messageBuilder.append("\n\nTest Results Summary:\n");
                
                int passed = 0;
                int failed = 0;
                int skipped = 0;
                
                for (Map.Entry<String, TestResult> entry : testResults.entrySet()) {
                    String testName = entry.getKey();
                    TestResult result = entry.getValue();
                    
                    switch (result.getStatus()) {
                        case PASS:
                            passed++;
                            break;
                        case FAIL:
                            failed++;
                            break;
                        case SKIP:
                            skipped++;
                            break;
                    }
                }
                
                messageBuilder.append("\nTotal: ").append(testResults.size())
                             .append(", Passed: ").append(passed)
                             .append(", Failed: ").append(failed)
                             .append(", Skipped: ").append(skipped)
                             .append("\n\n");
                
                // Add failed test details if any
                if (failed > 0) {
                    messageBuilder.append("Failed Tests:\n");
                    
                    for (Map.Entry<String, TestResult> entry : testResults.entrySet()) {
                        if (entry.getValue().getStatus() == TestStatus.FAIL) {
                            messageBuilder.append("- ").append(entry.getKey())
                                         .append(": ").append(entry.getValue().getMessage())
                                         .append("\n");
                        }
                    }
                }
                
                String message = messageBuilder.toString();
                
                // Determine priority based on failed tests
                NotificationPriority priority = failed > 0 ? 
                    NotificationPriority.HIGH : NotificationPriority.LOW;
                
                // Send to all enabled channels
                for (String channel : enabledChannels) {
                    switch (channel) {
                        case "email":
                            sendEmailNotification(subject, message, recipients, priority);
                            break;
                        case "slack":
                            sendSlackNotification(subject, message, priority);
                            break;
                        case "teams":
                            sendTeamsNotification(subject, message, priority);
                            break;
                    }
                }
                
                logger.info("Daily summary notification sent successfully");
            } catch (Exception e) {
                logger.error("Failed to send daily summary notification: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Send a notification via email
     */
    private void sendEmailNotification(
            String subject,
            String message,
            List<String> recipients,
            NotificationPriority priority,
            String... attachmentPaths) throws MessagingException {
        
        if (!enabledChannels.contains("email")) {
            logger.warn("Email channel not enabled, skipping email notification");
            return;
        }
        
        // Get email credentials from system properties
        final String username = System.getProperty("notification.email.username");
        final String password = System.getProperty("notification.email.password");
        final String fromAddress = System.getProperty("notification.email.from", username);
        
        if (username == null || password == null) {
            logger.error("Email credentials not configured");
            return;
        }
        
        // Create session with authentication
        Session session = Session.getInstance(emailConfig, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        
        // Create message
        Message mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(fromAddress));
        
        // Add recipients
        for (String recipient : recipients) {
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
        }
        
        // Set subject and add priority prefix
        String priorityPrefix = "";
        if (priority == NotificationPriority.HIGH) {
            priorityPrefix = "[URGENT] ";
            // Set high priority headers
            mimeMessage.addHeader("X-Priority", "1");
            mimeMessage.addHeader("X-MSMail-Priority", "High");
            mimeMessage.addHeader("Importance", "High");
        }
        mimeMessage.setSubject(priorityPrefix + subject);
        
        // Create message body
        Multipart multipart = new MimeMultipart();
        
        // Add text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(message);
        multipart.addBodyPart(textPart);
        
        // Add attachments if any
        for (String attachmentPath : attachmentPaths) {
            if (attachmentPath != null && !attachmentPath.isEmpty()) {
                try {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(attachmentPath);
                    multipart.addBodyPart(attachmentPart);
                } catch (IOException e) {
                    logger.error("Failed to attach file {}: {}", attachmentPath, e.getMessage());
                }
            }
        }
        
        mimeMessage.setContent(multipart);
        
        // Send message
        Transport.send(mimeMessage);
        logger.info("Email notification sent to {} recipients", recipients.size());
    }
    
    /**
     * Send a notification via Slack webhook
     */
    private void sendSlackNotification(
            String subject,
            String message,
            NotificationPriority priority) {
        
        if (!enabledChannels.contains("slack") || slackWebhookUrl == null) {
            logger.warn("Slack channel not enabled, skipping Slack notification");
            return;
        }
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(slackWebhookUrl);
            
            // Format the message for Slack
            String emoji = priority == NotificationPriority.HIGH ? ":red_circle:" : 
                          (priority == NotificationPriority.MEDIUM ? ":large_orange_diamond:" : ":large_blue_circle:");
            
            String formattedMessage = String.format(
                "payload={\"text\": \"%s %s\\n\\n%s\"}",
                emoji,
                subject.replace("\"", "\\\""),
                message.replace("\"", "\\\"").replace("\n", "\\n")
            );
            
            StringEntity entity = new StringEntity(formattedMessage);
            request.setEntity(entity);
            request.setHeader("Content-type", "application/x-www-form-urlencoded");
            
            httpClient.execute(request);
            logger.info("Slack notification sent successfully");
            
        } catch (IOException e) {
            logger.error("Failed to send Slack notification: {}", e.getMessage());
        }
    }
    
    /**
     * Send a notification via Microsoft Teams webhook
     */
    private void sendTeamsNotification(
            String subject,
            String message,
            NotificationPriority priority) {
        
        if (!enabledChannels.contains("teams") || teamsWebhookUrl == null) {
            logger.warn("Teams channel not enabled, skipping Teams notification");
            return;
        }
        
        // Determine color based on priority
        String color = switch (priority) {
            case HIGH -> "FF0000";
            case MEDIUM -> "FFA500";
            case LOW -> "0000FF";
        };
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(teamsWebhookUrl);
            
            // Format the message for Teams
            String jsonPayload = String.format(
                "{\"@type\": \"MessageCard\", \"@context\": \"http://schema.org/extensions\", \"themeColor\": \"%s\", \"title\": \"%s\", \"text\": \"%s\"}",
                color,
                subject.replace("\"", "\\\""),
                message.replace("\"", "\\\"").replace("\n", "\\n")
            );
            
            StringEntity entity = new StringEntity(jsonPayload);
            request.setEntity(entity);
            request.setHeader("Content-type", "application/json");
            
            httpClient.execute(request);
            logger.info("Teams notification sent successfully");
            
        } catch (IOException e) {
            logger.error("Failed to send Teams notification: {}", e.getMessage());
        }
    }
    
    /**
     * Check if a specific notification channel is enabled
     * @param channel Channel name ("email", "slack", or "teams")
     * @return True if the channel is enabled, false otherwise
     */
    public boolean isChannelEnabled(String channel) {
        return enabledChannels.contains(channel.toLowerCase());
    }
    
    /**
     * Get a list of enabled notification channels
     * @return Set of enabled channel names
     */
    public Set<String> getEnabledChannels() {
        return Collections.unmodifiableSet(enabledChannels);
    }
}
