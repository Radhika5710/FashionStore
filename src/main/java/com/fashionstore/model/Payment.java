package com.fashionstore.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment model for handling payment transactions
 */
public class Payment {
    private int paymentId;
    private int orderId;
    private String paymentMethod; // RAZORPAY, STRIPE, COD
    private String transactionId; // Gateway transaction ID
    private BigDecimal amount;
    private String currency;
    private String status; // PENDING, SUCCESS, FAILED, REFUNDED
    private String gatewayResponse; // JSON response from gateway
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String paymentSignature; // For Razorpay signature verification
    private String webhookId; // Webhook event ID
    private boolean verified; // Whether payment was verified via webhook

    public Payment() {}

    public Payment(int orderId, String paymentMethod, String transactionId, BigDecimal amount, String currency) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.verified = false;
    }

    // Getters and Setters
    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPaymentSignature() {
        return paymentSignature;
    }

    public void setPaymentSignature(String paymentSignature) {
        this.paymentSignature = paymentSignature;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public void setWebhookId(String webhookId) {
        this.webhookId = webhookId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
