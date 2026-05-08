package com.fashionstore.dao;

import com.fashionstore.model.Coupon;
import java.util.List;

public interface CouponDAO {
    // CRUD operations
    boolean addCoupon(Coupon coupon);
    boolean updateCoupon(Coupon coupon);
    boolean deleteCoupon(int couponId);
    Coupon getCouponById(int couponId);
    Coupon getCouponByCode(String code);
    List<Coupon> getAllCoupons();
    List<Coupon> getActiveCoupons();
    
    // Usage tracking
    boolean recordCouponUsage(int couponId, int userId, int orderId);
    int getCouponUsageCount(int couponId);
    int getUserCouponUsageCount(int couponId, int userId);
    boolean isCouponUsedByUser(int couponId, int userId);
    
    // Validation methods
    boolean isCouponValid(String code, double orderAmount, int userId, int productId, int categoryId);
    List<Coupon> getApplicableCoupons(double orderAmount, int userId, int productId, int categoryId);
    
    // Utility methods
    boolean couponExists(String code);
    boolean incrementCouponUsage(int couponId);
}
