package com.fashionstore.model;

import java.sql.Timestamp;
import java.util.List;

public class Coupon {
    private int couponId;
    private String code;
    private String description;
    private String discountType; // percentage, fixed_amount
    private double discountValue;
    private double minimumOrderAmount;
    private Double maximumDiscountAmount;
    private Integer usageLimit;
    private int usageCount;
    private int userUsageLimit; // Per user limit
    private boolean isActive;
    private Timestamp validFrom;
    private Timestamp validUntil;
    private List<Integer> applicableProducts; // Product IDs
    private List<Integer> applicableCategories; // Category IDs
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Constructors
    public Coupon() {}

    public Coupon(String code, String description, String discountType, 
                double discountValue, double minimumOrderAmount, 
                Double maximumDiscountAmount, Integer usageLimit, 
                int userUsageLimit, Timestamp validFrom, Timestamp validUntil) {
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.usageLimit = usageLimit;
        this.userUsageLimit = userUsageLimit;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.isActive = true;
    }

    // Getters and Setters
    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getMinimumOrderAmount() { return minimumOrderAmount; }
    public void setMinimumOrderAmount(double minimumOrderAmount) { this.minimumOrderAmount = minimumOrderAmount; }

    public Double getMaximumDiscountAmount() { return maximumDiscountAmount; }
    public void setMaximumDiscountAmount(Double maximumDiscountAmount) { this.maximumDiscountAmount = maximumDiscountAmount; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }

    public int getUserUsageLimit() { return userUsageLimit; }
    public void setUserUsageLimit(int userUsageLimit) { this.userUsageLimit = userUsageLimit; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean isActive) { this.isActive = isActive; }

    public Timestamp getValidFrom() { return validFrom; }
    public void setValidFrom(Timestamp validFrom) { this.validFrom = validFrom; }

    public Timestamp getValidUntil() { return validUntil; }
    public void setValidUntil(Timestamp validUntil) { this.validUntil = validUntil; }

    public List<Integer> getApplicableProducts() { return applicableProducts; }
    public void setApplicableProducts(List<Integer> applicableProducts) { this.applicableProducts = applicableProducts; }

    public List<Integer> getApplicableCategories() { return applicableCategories; }
    public void setApplicableCategories(List<Integer> applicableCategories) { this.applicableCategories = applicableCategories; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    public boolean isValid() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return isActive && 
               now.after(validFrom) && 
               now.before(validUntil) &&
               (usageLimit == null || usageCount < usageLimit);
    }

    public boolean isApplicableToProduct(int productId, int categoryId) {
        // If no restrictions, applicable to all products
        if ((applicableProducts == null || applicableProducts.isEmpty()) &&
            (applicableCategories == null || applicableCategories.isEmpty())) {
            return true;
        }

        // Check product-specific restrictions
        if (applicableProducts != null && !applicableProducts.isEmpty()) {
            return applicableProducts.contains(productId);
        }

        // Check category-specific restrictions
        if (applicableCategories != null && !applicableCategories.isEmpty()) {
            return applicableCategories.contains(categoryId);
        }

        return true;
    }

    public double calculateDiscount(double orderAmount) {
        if (orderAmount < minimumOrderAmount) {
            return 0.0;
        }

        double discount = 0.0;
        if ("percentage".equals(discountType)) {
            discount = orderAmount * (discountValue / 100.0);
        } else if ("fixed_amount".equals(discountType)) {
            discount = discountValue;
        }

        // Apply maximum discount limit if set
        if (maximumDiscountAmount != null && discount > maximumDiscountAmount) {
            discount = maximumDiscountAmount;
        }

        return Math.min(discount, orderAmount); // Don't exceed order amount
    }

    @Override
    public String toString() {
        return "Coupon{" +
               "couponId=" + couponId +
               ", code='" + code + '\'' +
               ", description='" + description + '\'' +
               ", discountType='" + discountType + '\'' +
               ", discountValue=" + discountValue +
               ", minimumOrderAmount=" + minimumOrderAmount +
               ", usageLimit=" + usageLimit +
               ", usageCount=" + usageCount +
               ", userUsageLimit=" + userUsageLimit +
               ", isActive=" + isActive +
               '}';
    }
}
