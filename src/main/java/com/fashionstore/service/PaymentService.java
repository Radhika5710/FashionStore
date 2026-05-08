package com.fashionstore.service;

import com.fashionstore.dao.PaymentDAO;
import com.fashionstore.daoimpl.PaymentDAOImpl;
import com.fashionstore.model.Payment;
import com.fashionstore.util.AuditLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Payment service for handling payment operations
 * Supports Razorpay, Stripe, and COD
 */
public class PaymentService {
    
    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());
    private PaymentDAO paymentDAO;
    
    // Razorpay configuration (should be loaded from environment or config)
    private String razorpayKeyId;
    private String razorpayKeySecret;
    
    // Stripe configuration (should be loaded from environment or config)
    private String stripePublicKey;
    public PaymentService() {
        this.paymentDAO = new PaymentDAOImpl();
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        // In production, load from environment variables or config file
        this.razorpayKeyId = System.getenv("RAZORPAY_KEY_ID");
        this.razorpayKeySecret = System.getenv("RAZORPAY_KEY_SECRET");
        this.stripePublicKey = System.getenv("STRIPE_PUBLIC_KEY");
        
        LOGGER.info("Payment service configuration loaded");
    }
    
    /**
     * Create a new payment record
     */
    public Payment createPayment(int orderId, String paymentMethod, String transactionId, 
                                 BigDecimal amount, String currency, HttpServletRequest request) {
        Payment payment = new Payment(orderId, paymentMethod, transactionId, amount, currency);
        int paymentId = paymentDAO.createPayment(payment);
        
        if (paymentId > 0) {
            payment.setPaymentId(paymentId);
            AuditLogger.log("PAYMENT_CREATED", "Payment created: " + paymentId + " for order: " + orderId, 
                           String.valueOf(orderId), request);
            return payment;
        }
        
        LOGGER.log(Level.SEVERE, "Failed to create payment for order: " + orderId);
        return null;
    }
    
    /**
     * Verify Razorpay payment signature
     * Prevents fake payment confirmations
     */
    public boolean verifyRazorpaySignature(String orderId, String paymentId, String signature, HttpServletRequest request) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);
            
            boolean isValid = expectedSignature.equals(signature);
            
            if (isValid) {
                AuditLogger.log("PAYMENT_VERIFIED", "Razorpay payment verified: " + paymentId, 
                               paymentId, request);
            } else {
                AuditLogger.log("PAYMENT_VERIFICATION_FAILED", "Invalid Razorpay signature for payment: " + paymentId, 
                               paymentId, request);
            }
            
            return isValid;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.log(Level.SEVERE, "Error verifying Razorpay signature", e);
            return false;
        }
    }
    
    /**
     * Update payment status after verification
     */
    public boolean updatePaymentStatus(int paymentId, String status, HttpServletRequest request) {
        boolean updated = paymentDAO.updatePaymentStatus(paymentId, status);
        
        if (updated) {
            AuditLogger.log("PAYMENT_STATUS_UPDATED", "Payment status updated to: " + status + " for payment: " + paymentId, 
                           String.valueOf(paymentId), request);
        }
        
        return updated;
    }
    
    /**
     * Mark payment as verified via webhook
     */
    public boolean markPaymentVerified(int paymentId, String webhookId, HttpServletRequest request) {
        boolean updated = paymentDAO.updatePaymentVerification(paymentId, true, webhookId);
        
        if (updated) {
            AuditLogger.log("PAYMENT_WEBHOOK_VERIFIED", "Payment verified via webhook: " + paymentId + ", webhook: " + webhookId, 
                           String.valueOf(paymentId), request);
        }
        
        return updated;
    }
    
    /**
     * Get payment by ID
     */
    public Payment getPaymentById(int paymentId) {
        return paymentDAO.getPaymentById(paymentId);
    }
    
    /**
     * Get payment by transaction ID
     */
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentDAO.getPaymentByTransactionId(transactionId);
    }
    
    /**
     * Get payment by order ID
     */
    public Payment getPaymentByOrderId(int orderId) {
        return paymentDAO.getPaymentByOrderId(orderId);
    }
    
    /**
     * Process COD payment (no external gateway)
     */
    public Payment processCODPayment(int orderId, BigDecimal amount, HttpServletRequest request) {
        String transactionId = "COD_" + System.currentTimeMillis() + "_" + orderId;
        Payment payment = createPayment(orderId, "COD", transactionId, amount, "INR", request);
        
        if (payment != null) {
            // COD is confirmed immediately
            payment.setStatus("SUCCESS");
            payment.setVerified(true);
            paymentDAO.updatePaymentStatus(payment.getPaymentId(), "SUCCESS");
            paymentDAO.updatePaymentVerification(payment.getPaymentId(), true, null);
        }
        
        return payment;
    }
    
    /**
     * Process Razorpay payment
     */
    public Payment processRazorpayPayment(int orderId, BigDecimal amount, String razorpayOrderId, HttpServletRequest request) {
        String transactionId = razorpayOrderId;
        Payment payment = createPayment(orderId, "RAZORPAY", transactionId, amount, "INR", request);
        return payment;
    }
    
    /**
     * Process Stripe payment
     */
    public Payment processStripePayment(int orderId, BigDecimal amount, String stripePaymentIntentId, HttpServletRequest request) {
        String transactionId = stripePaymentIntentId;
        Payment payment = createPayment(orderId, "STRIPE", transactionId, amount, "INR", request);
        return payment;
    }
    
    /**
     * Handle payment failure
     */
    public boolean handlePaymentFailure(int paymentId, String failureReason, HttpServletRequest request) {
        Payment payment = paymentDAO.getPaymentById(paymentId);
        
        if (payment != null) {
            payment.setStatus("FAILED");
            payment.setGatewayResponse(failureReason);
            paymentDAO.updatePaymentStatus(paymentId, "FAILED");
            AuditLogger.log("PAYMENT_FAILED", "Payment failed: " + paymentId + ", reason: " + failureReason, 
                           String.valueOf(paymentId), request);
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle payment success
     */
    public boolean handlePaymentSuccess(int paymentId, String gatewayResponse, HttpServletRequest request) {
        Payment payment = paymentDAO.getPaymentById(paymentId);
        
        if (payment != null) {
            payment.setStatus("SUCCESS");
            payment.setGatewayResponse(gatewayResponse);
            paymentDAO.updatePaymentStatus(paymentId, "SUCCESS");
            AuditLogger.log("PAYMENT_SUCCESS", "Payment succeeded: " + paymentId, 
                           String.valueOf(paymentId), request);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if payment is verified (via webhook or signature)
     */
    public boolean isPaymentVerified(int paymentId) {
        Payment payment = paymentDAO.getPaymentById(paymentId);
        return payment != null && payment.isVerified();
    }
    
    /**
     * Get Razorpay key ID for frontend
     */
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
    
    /**
     * Get Stripe public key for frontend
     */
    public String getStripePublicKey() {
        return stripePublicKey;
    }
}
