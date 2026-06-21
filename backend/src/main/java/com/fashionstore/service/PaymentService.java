package com.fashionstore.service;

import com.fashionstore.dao.PaymentDAO;
import com.fashionstore.model.Payment;
import com.fashionstore.util.AuditLogger;

import java.math.BigDecimal;
import java.sql.Connection;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
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
        loadConfiguration();
    }
    
    public void setPaymentDAO(PaymentDAO paymentDAO) {
        this.paymentDAO = paymentDAO;
    }
    
    private void loadConfiguration() {
        // In production, load from environment variables or config file
        this.razorpayKeyId = System.getenv("RAZORPAY_KEY_ID");
        this.razorpayKeySecret = System.getenv("RAZORPAY_KEY_SECRET");
        this.stripePublicKey = System.getenv("STRIPE_PUBLIC_KEY");
        
        LOGGER.info("Payment service configuration loaded");
    }
    
    /**
     * Create a payment record
     */
    public int createPayment(Payment payment) {
        return paymentDAO.createPayment(payment);
    }
    
    /**
     * Create a payment record with connection for transaction support
     * This version is used within a transaction to ensure atomicity
     */
    public int createPaymentInTransaction(Connection conn, Payment payment) {
        return paymentDAO.createPaymentInTransaction(conn, payment);
    }
    
    /**
     * Create a new payment record
     */
    public Payment createPayment(int orderId, String paymentMethod, String transactionId, 
                                 BigDecimal amount, String currency, HttpServletRequest request) {
        Payment payment = new Payment(orderId, paymentMethod, transactionId, amount, currency);
        int paymentId = createPayment(payment);
        
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
            
            // Update orders table payment status for consistency
            Payment payment = paymentDAO.getPaymentById(paymentId);
            if (payment != null) {
                String orderPaymentStatus = mapPaymentStatusToOrderStatus(status);
                paymentDAO.updateOrderPaymentStatus(payment.getOrderId(), orderPaymentStatus, payment.getTransactionId());
            }
        }
        
        return updated;
    }
    
    /**
     * Map payment status to order payment status
     */
    private String mapPaymentStatusToOrderStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return "pending";
        }
        
        switch (paymentStatus.toLowerCase()) {
            case "succeeded":
                return "completed";
            case "failed":
                return "failed";
            case "refunded":
                return "refunded";
            case "processing":
            case "requires_payment_method":
            case "requires_confirmation":
            case "requires_action":
                return "processing";
            case "canceled":
                return "failed";
            case "pending":
            default:
                return "pending";
        }
    }
    
    /**
     * Mark payment as verified via webhook with idempotency
     * Prevents duplicate webhook processing
     */
    public boolean markPaymentVerified(int paymentId, String webhookId, HttpServletRequest request) {
        // Check if already processed to prevent duplicate webhook handling
        Payment existingPayment = paymentDAO.getPaymentById(paymentId);
        if (existingPayment != null && existingPayment.isVerified()) {
            // Already verified, check if same webhook
            if (webhookId != null && webhookId.equals(existingPayment.getGatewayResponse())) {
                LOGGER.info("Payment already verified with same webhook: " + paymentId);
                return true; // Idempotent - return success
            }
            LOGGER.warning("Payment already verified with different webhook: " + paymentId);
            return false; // Different webhook, potential duplicate
        }
        
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
            // COD is confirmed immediately - use correct ENUM value "succeeded"
            payment.setStatus("succeeded");
            payment.setVerified(true);
            paymentDAO.updatePaymentStatus(payment.getPaymentId(), "succeeded");
            paymentDAO.updatePaymentVerification(payment.getPaymentId(), true, null);
            
            // Update orders table payment status for consistency
            paymentDAO.updateOrderPaymentStatus(orderId, "completed", transactionId);
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
     * Handle payment failure with inventory rollback
     * Restores inventory when payment fails after order creation
     */
    public boolean handlePaymentFailure(int paymentId, String failureReason, HttpServletRequest request) {
        Payment payment = paymentDAO.getPaymentById(paymentId);
        
        if (payment != null) {
            // Use correct ENUM value "failed"
            payment.setStatus("failed");
            payment.setGatewayResponse(failureReason);
            paymentDAO.updatePaymentStatus(paymentId, "failed");
            AuditLogger.log("PAYMENT_FAILED", "Payment failed: " + paymentId + ", reason: " + failureReason, 
                           String.valueOf(paymentId), request);
            
            // Update orders table payment status for consistency
            int orderId = payment.getOrderId();
            if (orderId > 0) {
                paymentDAO.updateOrderPaymentStatus(orderId, "failed", payment.getTransactionId());
                
                // Rollback inventory for failed payment by cancelling the order
                try {
                    com.fashionstore.service.OrderService orderService = new com.fashionstore.serviceimpl.OrderServiceImpl();
                    // Cancel order to restore inventory (uses internal restoreInventoryForOrder)
                    orderService.cancelOrder(orderId, 0); // 0 = system request
                    LOGGER.info("Order cancelled and inventory restored for order: " + orderId + " due to payment failure: " + paymentId);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to cancel order and restore inventory for order: " + orderId, e);
                }
            }
            
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
            // Use correct ENUM value "succeeded"
            payment.setStatus("succeeded");
            payment.setGatewayResponse(gatewayResponse);
            paymentDAO.updatePaymentStatus(paymentId, "succeeded");
            
            // Update orders table payment status for consistency
            paymentDAO.updateOrderPaymentStatus(payment.getOrderId(), "completed", payment.getTransactionId());
            
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
