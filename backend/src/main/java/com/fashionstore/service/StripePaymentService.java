package com.fashionstore.service;

import com.fashionstore.dao.PaymentDAO;
import com.fashionstore.model.Payment;
import com.fashionstore.util.AuditLogger;
import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.*;
import com.stripe.param.*;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stripe Payment Service - Production-grade payment integration
 * Handles payment intents, webhooks, refunds, and idempotency
 */
public class StripePaymentService {
    
    private static final Logger LOGGER = Logger.getLogger(StripePaymentService.class.getName());
    private static final Gson GSON = new Gson();
    
    private PaymentDAO paymentDAO;
    private String stripeSecretKey;
    private String stripeWebhookSecret;
    private String stripePublishableKey;
    
    public StripePaymentService() {
        loadConfiguration();
    }
    
    public void setPaymentDAO(PaymentDAO paymentDAO) {
        this.paymentDAO = paymentDAO;
    }
    
    private void loadConfiguration() {
        this.stripeSecretKey = System.getenv("STRIPE_SECRET_KEY");
        this.stripeWebhookSecret = System.getenv("STRIPE_WEBHOOK_SECRET");
        this.stripePublishableKey = System.getenv("STRIPE_PUBLISHABLE_KEY");
        
        if (stripeSecretKey != null && !stripeSecretKey.isEmpty()) {
            Stripe.apiKey = stripeSecretKey;
            LOGGER.info("Stripe API key configured");
        } else {
            LOGGER.warning("Stripe secret key not configured");
        }
    }
    
    /**
     * Generate idempotency key for safe retries
     */
    public String generateIdempotencyKey(String prefix) {
        String randomPart = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + "_" + timestamp + "_" + randomPart;
    }
    
    /**
     * Create or retrieve Stripe customer for user
     */
    public Customer createOrGetStripeCustomer(int userId, String email, String name, String phone) throws StripeException {
        // Check if customer already exists in our database
        // For now, create new customer each time (in production, cache this)
        CustomerCreateParams params = CustomerCreateParams.builder()
            .setEmail(email)
            .setName(name)
            .setPhone(phone)
            .putMetadata("user_id", String.valueOf(userId))
            .build();
        
        return Customer.create(params);
    }
    
    /**
     * Create Payment Intent for checkout
     */
    public PaymentIntent createPaymentIntent(
        BigDecimal amount,
        String currency,
        String customerEmail,
        String customerName,
        String description,
        Map<String, String> metadata,
        HttpServletRequest request
    ) throws StripeException {
        
        // Convert amount to cents (Stripe uses smallest currency unit)
        long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
        
        // Generate idempotency key
        String idempotencyKey = generateIdempotencyKey("payment_intent");
        
        // Create payment intent params
        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
            .setAmount(amountInCents)
            .setCurrency(currency.toLowerCase())
            .setDescription(description)
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .putMetadata("idempotency_key", idempotencyKey);
        
        // Add metadata if provided
        if (metadata != null && !metadata.isEmpty()) {
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                paramsBuilder.putMetadata(entry.getKey(), entry.getValue());
            }
        }
        
        // Add customer info if provided
        if (customerEmail != null) {
            paramsBuilder.setReceiptEmail(customerEmail);
        }
        
        // Use RequestOptions for idempotency
        RequestOptions requestOptions = RequestOptions.builder()
            .setIdempotencyKey(idempotencyKey)
            .build();
        
        PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build(), requestOptions);
        
        AuditLogger.log("PAYMENT_INTENT_CREATED", 
            "Payment intent created: " + paymentIntent.getId() + " for amount: " + amount, 
            paymentIntent.getId(), request);
        
        return paymentIntent;
    }
    
    /**
     * Confirm Payment Intent (for client-side confirmation)
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        
        PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
            .setPaymentMethod(paymentMethodId)
            .build();
        
        return paymentIntent.confirm(params);
    }
    
    /**
     * Retrieve Payment Intent
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }
    
    /**
     * Create Setup Intent for saving payment methods
     */
    public SetupIntent createSetupIntent(String customerId) throws StripeException {
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
            .setCustomer(customerId)
            .build();
        
        return SetupIntent.create(params);
    }
    
    /**
     * Create Payment Method from token
     */
    public PaymentMethod createPaymentMethod(String paymentMethodToken) throws StripeException {
        return PaymentMethod.retrieve(paymentMethodToken);
    }
    
    /**
     * Attach Payment Method to Customer
     */
    public PaymentMethod attachPaymentMethod(String paymentMethodId, String customerId) throws StripeException {
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
        
        PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
            .setCustomer(customerId)
            .build();
        
        return paymentMethod.attach(params);
    }
    
    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signatureHeader, HttpServletRequest request) {
        try {
            if (stripeWebhookSecret == null || stripeWebhookSecret.isEmpty()) {
                LOGGER.warning("Stripe webhook secret not configured");
                return false;
            }
            
            Event event = Webhook.constructEvent(
                payload,
                signatureHeader,
                stripeWebhookSecret
            );
            
            AuditLogger.log("WEBHOOK_SIGNATURE_VERIFIED", 
                "Webhook signature verified for event: " + event.getId(), 
                event.getId(), request);
            
            return true;
        } catch (SignatureVerificationException e) {
            LOGGER.log(Level.SEVERE, "Webhook signature verification failed", e);
            AuditLogger.log("WEBHOOK_SIGNATURE_FAILED", 
                "Webhook signature verification failed", 
                "unknown", request);
            return false;
        }
    }
    
    /**
     * Parse webhook event
     */
    public Event parseWebhookEvent(String payload) {
        return GSON.fromJson(payload, Event.class);
    }
    
    /**
     * Handle payment succeeded webhook event
     */
    public void handlePaymentSucceeded(Event event, HttpServletRequest request) {
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElseThrow();
            
            if (stripeObject instanceof PaymentIntent paymentIntent) {
                String paymentIntentId = paymentIntent.getId();
                String status = paymentIntent.getStatus();
                
                LOGGER.info("Processing payment succeeded event: " + paymentIntentId + ", status: " + status);
                
                // Find payment by Stripe payment intent ID
                Payment payment = paymentDAO.getPaymentByStripePaymentIntentId(paymentIntentId);
                
                if (payment != null) {
                    // Webhook Idempotency: Check if the payment was already processed
                    if (payment.isVerified() || "succeeded".equals(payment.getStatus())) {
                        LOGGER.info("Payment " + payment.getPaymentId() + " already completed. Skipping duplicate webhook event ID: " + event.getId());
                        return;
                    }
                    
                    // Update payment status
                    if ("succeeded".equals(status)) {
                        paymentDAO.updatePaymentStatus(payment.getPaymentId(), "succeeded");
                        paymentDAO.updatePaymentVerification(payment.getPaymentId(), true, event.getId());
                        
                        // Update order status
                        paymentDAO.updateOrderPaymentStatus(payment.getOrderId(), "completed", paymentIntentId);
                        
                        AuditLogger.log("PAYMENT_WEBHOOK_SUCCEEDED", 
                            "Payment succeeded via webhook: " + paymentIntentId, 
                            paymentIntentId, request);
                    }
                } else {
                    LOGGER.warning("Payment not found for Stripe payment intent: " + paymentIntentId);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling payment succeeded event", e);
        }
    }
    
    /**
     * Handle payment failed webhook event
     */
    public void handlePaymentFailed(Event event, HttpServletRequest request) {
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = dataObjectDeserializer.getObject().orElseThrow();
            
            if (stripeObject instanceof PaymentIntent paymentIntent) {
                String paymentIntentId = paymentIntent.getId();
                String status = paymentIntent.getStatus();
                
                LOGGER.info("Processing payment failed event: " + paymentIntentId + ", status: " + status);
                
                // Find payment by Stripe payment intent ID
                Payment payment = paymentDAO.getPaymentByStripePaymentIntentId(paymentIntentId);
                
                if (payment != null) {
                    // Webhook Idempotency Check for failed events
                    if ("failed".equals(payment.getStatus())) {
                        LOGGER.info("Payment " + payment.getPaymentId() + " already marked failed. Skipping duplicate webhook event ID: " + event.getId());
                        return;
                    }
                    if ("succeeded".equals(payment.getStatus())) {
                        LOGGER.info("Payment " + payment.getPaymentId() + " is already successful. Ignoring late-arriving failure event ID: " + event.getId());
                        return;
                    }
                    
                    // Update payment status
                    paymentDAO.updatePaymentStatus(payment.getPaymentId(), "failed");
                    
                    // Get failure reason
                    String failureReason = paymentIntent.getLastPaymentError() != null 
                        ? paymentIntent.getLastPaymentError().getMessage() 
                        : "Unknown error";
                    
                    paymentDAO.updatePaymentFailureReason(payment.getPaymentId(), failureReason);
                    
                    AuditLogger.log("PAYMENT_WEBHOOK_FAILED", 
                        "Payment failed via webhook: " + paymentIntentId + ", reason: " + failureReason, 
                        paymentIntentId, request);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling payment failed event", e);
        }
    }
    
    /**
     * Create refund
     */
    public Refund createRefund(
        String chargeId,
        BigDecimal amount,
        String reason,
        HttpServletRequest request
    ) throws StripeException {
        
        long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
        String idempotencyKey = generateIdempotencyKey("refund");
        
        RefundCreateParams params = RefundCreateParams.builder()
            .setCharge(chargeId)
            .setAmount(amountInCents)
            .setReason(RefundCreateParams.Reason.valueOf(reason.toUpperCase()))
            .build();
        
        // Use RequestOptions for idempotency
        RequestOptions requestOptions = RequestOptions.builder()
            .setIdempotencyKey(idempotencyKey)
            .build();
        
        Refund refund = Refund.create(params, requestOptions);
        
        AuditLogger.log("REFUND_CREATED", 
            "Refund created: " + refund.getId() + " for charge: " + chargeId + ", amount: " + amount, 
            refund.getId(), request);
        
        return refund;
    }
    
    /**
     * Create full refund
     */
    public Refund createFullRefund(String chargeId, HttpServletRequest request) throws StripeException {
        String idempotencyKey = generateIdempotencyKey("refund");
        
        RefundCreateParams params = RefundCreateParams.builder()
            .setCharge(chargeId)
            .build();
        
        // Use RequestOptions for idempotency
        RequestOptions requestOptions = RequestOptions.builder()
            .setIdempotencyKey(idempotencyKey)
            .build();
        
        Refund refund = Refund.create(params, requestOptions);
        
        AuditLogger.log("FULL_REFUND_CREATED", 
            "Full refund created: " + refund.getId() + " for charge: " + chargeId, 
            refund.getId(), request);
        
        return refund;
    }

    /**
     * Safely refund a payment by either its Charge ID or PaymentIntent ID
     */
    public Refund refundPaymentIntent(String paymentIntentId, BigDecimal amount, String reason, HttpServletRequest request) throws StripeException {
        long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
        String idempotencyKey = generateIdempotencyKey("refund");
        
        RefundCreateParams.Builder builder = RefundCreateParams.builder()
            .setAmount(amountInCents);
            
        if (paymentIntentId.startsWith("ch_") || paymentIntentId.startsWith("py_")) {
            builder.setCharge(paymentIntentId);
        } else {
            builder.setPaymentIntent(paymentIntentId);
        }
        
        if (reason != null && !reason.isEmpty()) {
            try {
                builder.setReason(RefundCreateParams.Reason.valueOf(reason.toUpperCase()));
            } catch (Exception e) {
                // Ignore invalid enum values
            }
        }
        
        RequestOptions requestOptions = RequestOptions.builder()
            .setIdempotencyKey(idempotencyKey)
            .build();
            
        Refund refund = Refund.create(builder.build(), requestOptions);
        
        AuditLogger.log("REFUND_CREATED", 
            "Refund created: " + refund.getId() + " for payment intent: " + paymentIntentId + ", amount: " + amount, 
            refund.getId(), request);
            
        return refund;
    }
    
    /**
     * Get Stripe publishable key for frontend
     */
    public String getStripePublishableKey() {
        return stripePublishableKey;
    }
    
    /**
     * Check if Stripe is configured
     */
    public boolean isConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isEmpty() 
            && stripePublishableKey != null && !stripePublishableKey.isEmpty();
    }
}
