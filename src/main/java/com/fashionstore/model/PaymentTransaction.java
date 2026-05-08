package com.fashionstore.model;

import java.sql.Timestamp;

public class PaymentTransaction {
    private int transactionId;
    private int orderId;
    private Integer paymentMethodId;
    private String gatewayTransactionId;
    private String gateway; // stripe, razorpay, paypal
    private double amount;
    private String currency;
    private String status; // pending, processing, completed, failed, cancelled, refunded
    private String paymentMethodType;
    private String gatewayResponse; // Full response from payment gateway
    private String failureReason;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public PaymentTransaction() {}

    public PaymentTransaction(int orderId, String gatewayTransactionId, String gateway, 
                           double amount, String status, String paymentMethodType, 
                           String gatewayResponse, String failureReason) {
        this.orderId = orderId;
        this.gatewayTransactionId = gatewayTransactionId;
        this.gateway = gateway;
        this.amount = amount;
        this.currency = "INR";
        this.status = status;
        this.paymentMethodType = paymentMethodType;
        this.gatewayResponse = gatewayResponse;
        this.failureReason = failureReason;
    }

    // Getters and Setters
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethodType() { return paymentMethodType; }
    public void setPaymentMethodType(String paymentMethodType) { this.paymentMethodType = paymentMethodType; }

    public String getGatewayResponse() { return gatewayResponse; }
    public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    public boolean isSuccessful() {
        return "completed".equals(status);
    }

    public boolean isPending() {
        return "pending".equals(status) || "processing".equals(status);
    }

    public boolean isFailed() {
        return "failed".equals(status) || "cancelled".equals(status);
    }

    public boolean isRefunded() {
        return "refunded".equals(status);
    }

    public String getDisplayStatus() {
        switch (status) {
            case "pending": return "Pending";
            case "processing": return "Processing";
            case "completed": return "Completed";
            case "failed": return "Failed";
            case "cancelled": return "Cancelled";
            case "refunded": return "Refunded";
            default: return status.toUpperCase();
        }
    }

    public String getFormattedAmount() {
        return String.format("₹%.2f", amount);
    }

    @Override
    public String toString() {
        return "PaymentTransaction{" +
               "transactionId=" + transactionId +
               ", orderId=" + orderId +
               ", gatewayTransactionId='" + gatewayTransactionId + '\'' +
               ", gateway='" + gateway + '\'' +
               ", amount=" + amount +
               ", currency='" + currency + '\'' +
               ", status='" + status + '\'' +
               ", paymentMethodType='" + paymentMethodType + '\'' +
               ", failureReason='" + failureReason + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}
