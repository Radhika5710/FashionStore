package com.fashionstore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple email notification service
 * Delegates to EmailService for sending emails
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    public NotificationService() {
        // Initialize service
    }
    
    /**
     * Send order confirmation email
     */
    public void sendOrderConfirmation(String email, String username, int orderId, double total) {
        try {
            EmailService.getInstance().sendOrderConfirmationEmail(email, username, orderId, total);
            logger.info("Order confirmation email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send order confirmation email to {}: {}", email, e.getMessage());
        }
    }
    
    /**
     * Send newsletter email
     */
    public void sendNewsletter(String email, String subject, String content) {
        try {
            EmailService.getInstance().sendNewsletterEmail(email, subject, content);
            logger.info("Newsletter email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send newsletter email to {}: {}", email, e.getMessage());
        }
    }
    
    /**
     * Send password reset email
     */
    public void sendPasswordReset(String email, String resetLink, String username) {
        try {
            EmailService.getInstance().sendPasswordResetEmail(email, resetLink, username);
            logger.info("Password reset email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }
    
    /**
     * Send welcome email
     */
    public void sendWelcome(String email, String username) {
        try {
            EmailService.getInstance().sendWelcomeEmail(email, username);
            logger.info("Welcome email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", email, e.getMessage());
        }
    }
}
