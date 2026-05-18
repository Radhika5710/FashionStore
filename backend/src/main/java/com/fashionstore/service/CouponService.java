package com.fashionstore.service;

import com.fashionstore.model.Coupon;

import java.util.List;

/**
 * Service interface for coupon operations and business logic
 * Handles coupon validation, application, and management
 */
public interface CouponService {
    
    /**
     * Validate coupon code
     */
    Coupon validateCoupon(String couponCode);
    
    /**
     * Apply coupon to cart
     */
    boolean applyCoupon(int userId, String couponCode);
    
    /**
     * Remove coupon from cart
     */
    boolean removeCoupon(int userId);
    
    /**
     * Calculate discount amount
     */
    double calculateDiscount(double cartTotal, Coupon coupon);
    
    /**
     * Check if coupon is valid for user
     */
    boolean isCouponValidForUser(int userId, Coupon coupon);
    
    /**
     * Check if coupon is valid for cart amount
     */
    boolean isCouponValidForAmount(double cartAmount, Coupon coupon);
    
    /**
     * Check if coupon has usage limit
     */
    boolean hasUsageLimit(Coupon coupon);
    
    /**
     * Check if coupon is expired
     */
    boolean isCouponExpired(Coupon coupon);
    
    /**
     * Increment coupon usage
     */
    boolean incrementCouponUsage(int couponId);
    
    /**
     * Get available coupons for user
     */
    List<Coupon> getAvailableCoupons(int userId);
    
    /**
     * Get all active coupons
     */
    List<Coupon> getAllActiveCoupons();
    
    /**
     * Get all coupons (including inactive)
     */
    List<Coupon> getAllCoupons();
    
    /**
     * Get coupon by ID
     */
    Coupon getCouponById(int couponId);
    
    /**
     * Create new coupon
     */
    boolean createCoupon(Coupon coupon);
    
    /**
     * Update coupon
     */
    boolean updateCoupon(Coupon coupon);
    
    /**
     * Delete coupon
     */
    boolean deleteCoupon(int couponId);
}
