package com.fashionstore.model;

import java.sql.Timestamp;

public class PaymentMethod {
    private int paymentMethodId;
    private int userId;
    private String methodType; // credit_card, debit_card, upi, net_banking, wallet
    private String provider; // stripe, razorpay, paypal
    private String methodAlias; // User-friendly name like "My Visa Card"
    private String lastFour;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardBrand;
    private boolean isDefault;
    private boolean isActive;
    private String gatewayToken; // Token from payment gateway
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public PaymentMethod() {}

    public PaymentMethod(int userId, String methodType, String provider, 
                      String methodAlias, String lastFour, Integer expiryMonth, 
                      Integer expiryYear, String cardBrand, boolean isDefault, 
                      boolean isActive, String gatewayToken) {
        this.userId = userId;
        this.methodType = methodType;
        this.provider = provider;
        this.methodAlias = methodAlias;
        this.lastFour = lastFour;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.cardBrand = cardBrand;
        this.isDefault = isDefault;
        this.isActive = isActive;
        this.gatewayToken = gatewayToken;
    }

    // Getters and Setters
    public int getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMethodType() { return methodType; }
    public void setMethodType(String methodType) { this.methodType = methodType; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getMethodAlias() { return methodAlias; }
    public void setMethodAlias(String methodAlias) { this.methodAlias = methodAlias; }

    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }

    public Integer getExpiryMonth() { return expiryMonth; }
    public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }

    public Integer getExpiryYear() { return expiryYear; }
    public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }

    public String getCardBrand() { return cardBrand; }
    public void setCardBrand(String cardBrand) { this.cardBrand = cardBrand; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public String getGatewayToken() { return gatewayToken; }
    public void setGatewayToken(String gatewayToken) { this.gatewayToken = gatewayToken; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Utility method to get masked card number
    public String getMaskedCardNumber() {
        if (lastFour != null && !lastFour.isEmpty()) {
            return "**** **** **** " + lastFour;
        }
        return "";
    }

    // Utility method to get formatted expiry
    public String getFormattedExpiry() {
        if (expiryMonth != null && expiryYear != null) {
            return String.format("%02d/%d", expiryMonth, expiryYear);
        }
        return "";
    }

    // Utility method to get display name
    public String getDisplayName() {
        if (methodAlias != null && !methodAlias.trim().isEmpty()) {
            return methodAlias;
        }
        
        if ("credit_card".equals(methodType) || "debit_card".equals(methodType)) {
            String brand = cardBrand != null ? cardBrand.toUpperCase() : "CARD";
            String masked = getMaskedCardNumber();
            return brand + " " + masked;
        }
        
        return methodType != null ? methodType.replace("_", " ").toUpperCase() : "PAYMENT METHOD";
    }

    @Override
    public String toString() {
        return "PaymentMethod{" +
               "paymentMethodId=" + paymentMethodId +
               ", userId=" + userId +
               ", methodType='" + methodType + '\'' +
               ", provider='" + provider + '\'' +
               ", methodAlias='" + methodAlias + '\'' +
               ", lastFour='" + lastFour + '\'' +
               ", expiryMonth=" + expiryMonth +
               ", expiryYear=" + expiryYear +
               ", cardBrand='" + cardBrand + '\'' +
               ", isDefault=" + isDefault +
               ", isActive=" + isActive +
               '}';
    }
}
