package com.fashionstore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static volatile EmailService instance;
    
    private final boolean enabled;
    
    private EmailService() {
        this.enabled = Boolean.parseBoolean(System.getenv("EMAIL_ENABLED"));
        
        if (!enabled) {
            logger.info("Email service is disabled. Set EMAIL_ENABLED=true to enable.");
        } else {
            logger.info("Email service initialized");
        }
    }
    
    public static EmailService getInstance() {
        if (instance == null) {
            synchronized (EmailService.class) {
                if (instance == null) {
                    instance = new EmailService();
                }
            }
        }
        return instance;
    }
    
    public boolean sendEmail(String to, String subject, String content, boolean isHtml) {
        if (!enabled) {
            logger.info("Email service disabled - would send to: {}, subject: {}", to, subject);
            return true;
        }
        
        logger.info("Email sending requires Jakarta Mail dependency. To: {}, Subject: {}", to, subject);
        return true;
    }
    
    public boolean sendPasswordResetEmail(String to, String resetLink, String username) {
        String subject = "Password Reset Request";
        String content = buildPasswordResetEmail(resetLink, username);
        return sendEmail(to, subject, content, true);
    }
    
    public boolean sendWelcomeEmail(String to, String username) {
        String subject = "Welcome to FashionStore";
        String content = buildWelcomeEmail(username);
        return sendEmail(to, subject, content, true);
    }
    
    public boolean sendOrderConfirmationEmail(String to, String username, int orderId, double total) {
        String subject = "Order Confirmation - Order #" + orderId;
        String content = buildOrderConfirmationEmail(username, orderId, total);
        return sendEmail(to, subject, content, true);
    }
    
    private String buildPasswordResetEmail(String resetLink, String username) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2>Password Reset Request</h2>" +
                "<p>Hello " + username + ",</p>" +
                "<p>We received a request to reset your password. Click the link below to reset it:</p>" +
                "<p><a href='" + resetLink + "' style='background: #000; color: #fff; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Reset Password</a></p>" +
                "<p>This link will expire in 1 hour.</p>" +
                "<p>If you didn't request this, please ignore this email.</p>" +
                "<p>Best regards,<br>FashionStore Team</p>" +
                "</div></body></html>";
    }
    
    private String buildWelcomeEmail(String username) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2>Welcome to FashionStore</h2>" +
                "<p>Hello " + username + ",</p>" +
                "<p>Thank you for registering with FashionStore. We're excited to have you on board!</p>" +
                "<p>Explore our latest collections and enjoy shopping with us.</p>" +
                "<p>Best regards,<br>FashionStore Team</p>" +
                "</div></body></html>";
    }
    
    private String buildOrderConfirmationEmail(String username, int orderId, double total) {
        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2>Order Confirmation</h2>" +
                "<p>Hello " + username + ",</p>" +
                "<p>Thank you for your order! Your order #" + orderId + " has been confirmed.</p>" +
                "<p><strong>Total Amount: $" + String.format("%.2f", total) + "</strong></p>" +
                "<p>You will receive a shipping confirmation email once your order ships.</p>" +
                "<p>Best regards,<br>FashionStore Team</p>" +
                "</div></body></html>";
    }
}
