package com.fashionstore.serviceimpl;

import com.fashionstore.dao.CouponDAO;
import com.fashionstore.model.Coupon;
import com.fashionstore.service.CouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service implementation for coupon operations
 */
public class CouponServiceImpl implements CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);
    private final CouponDAO couponDAO;

    public CouponServiceImpl() {
        // Default constructor - DAO will be set via setter injection
        this.couponDAO = null;
    }

    public CouponServiceImpl(CouponDAO couponDAO) {
        this.couponDAO = couponDAO;
    }

    public void setCouponDAO(CouponDAO couponDAO) {
        if (this.couponDAO == null) {
            try {
                java.lang.reflect.Field field = CouponServiceImpl.class.getDeclaredField("couponDAO");
                field.setAccessible(true);
                field.set(this, couponDAO);
            } catch (Exception e) {
                logger.error("Failed to set couponDAO", e);
            }
        }
    }

    @Override
    public Coupon validateCoupon(String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            logger.warn("Coupon code is null or empty");
            return null;
        }
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot validate coupon");
        }
        return couponDAO.getCouponByCode(couponCode.trim());
    }

    @Override
    public boolean applyCoupon(int userId, String couponCode) {
        Coupon coupon = validateCoupon(couponCode);
        if (coupon == null) {
            logger.warn("Invalid coupon code: {}", couponCode);
            return false;
        }
        
        // Validate coupon for user
        if (!isCouponValidForUser(userId, coupon)) {
            logger.warn("Coupon {} is not valid for user {}", couponCode, userId);
            return false;
        }
        
        return true;
    }

    @Override
    public boolean removeCoupon(int userId) {
        // Coupon removal is handled at session/cart level
        // This method is for logging coupon removal if needed
        logger.info("Coupon removed for user {}", userId);
        return true;
    }

    @Override
    public double calculateDiscount(double cartTotal, Coupon coupon) {
        if (coupon == null || cartTotal <= 0) {
            return 0.0;
        }
        
        // Check minimum order amount
        if (cartTotal < coupon.getMinimumOrderAmount()) {
            logger.warn("Cart total {} is less than minimum order amount {}", cartTotal, coupon.getMinimumOrderAmount());
            return 0.0;
        }
        
        return coupon.calculateDiscount(cartTotal);
    }

    @Override
    public boolean isCouponValidForUser(int userId, Coupon coupon) {
        if (coupon == null || userId <= 0) {
            return false;
        }
        
        // Check if coupon is active
        if (!coupon.isActive()) {
            logger.warn("Coupon {} is not active", coupon.getCode());
            return false;
        }
        
        // Check if coupon is expired
        if (isCouponExpired(coupon)) {
            logger.warn("Coupon {} is expired", coupon.getCode());
            return false;
        }
        
        // Check usage limit
        if (hasUsageLimit(coupon)) {
            logger.warn("Coupon {} has reached usage limit", coupon.getCode());
            return false;
        }
        
        // Check user usage limit
        if (coupon.getUserUsageLimit() > 0 && couponDAO.isCouponUsedByUser(coupon.getCouponId(), userId)) {
            logger.warn("User {} has already used coupon {}", userId, coupon.getCode());
            return false;
        }
        
        return true;
    }

    @Override
    public boolean isCouponValidForAmount(double cartAmount, Coupon coupon) {
        if (coupon == null || cartAmount <= 0) {
            return false;
        }
        
        return cartAmount >= coupon.getMinimumOrderAmount();
    }

    @Override
    public boolean hasUsageLimit(Coupon coupon) {
        if (coupon == null) {
            return false;
        }
        
        if (coupon.getUsageLimit() == null) {
            return false;
        }
        
        return coupon.getUsageCount() >= coupon.getUsageLimit();
    }

    @Override
    public boolean isCouponExpired(Coupon coupon) {
        if (coupon == null) {
            return true;
        }
        
        return !coupon.isValid();
    }

    @Override
    public boolean incrementCouponUsage(int couponId) {
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot increment coupon usage");
        }
        return couponDAO.incrementCouponUsage(couponId);
    }

    @Override
    public List<Coupon> getAvailableCoupons(int userId) {
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot get available coupons");
        }
        return couponDAO.getActiveCoupons();
    }

    @Override
    public List<Coupon> getAllActiveCoupons() {
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot get all active coupons");
        }
        return couponDAO.getActiveCoupons();
    }

    @Override
    public List<Coupon> getAllCoupons() {
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot get all coupons");
        }
        return couponDAO.getAllCoupons();
    }

    @Override
    public Coupon getCouponById(int couponId) {
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot get coupon by ID");
        }
        return couponDAO.getCouponById(couponId);
    }

    @Override
    public boolean createCoupon(Coupon coupon) {
        if (coupon == null) {
            logger.warn("Cannot create null coupon");
            return false;
        }
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot create coupon");
        }
        
        // Check if coupon code already exists
        if (couponDAO.couponExists(coupon.getCode())) {
            logger.warn("Coupon code {} already exists", coupon.getCode());
            return false;
        }
        
        return couponDAO.addCoupon(coupon);
    }

    @Override
    public boolean updateCoupon(Coupon coupon) {
        if (coupon == null || coupon.getCouponId() <= 0) {
            logger.warn("Cannot update null or invalid coupon");
            return false;
        }
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot update coupon");
        }
        
        return couponDAO.updateCoupon(coupon);
    }

    @Override
    public boolean deleteCoupon(int couponId) {
        if (couponId <= 0) {
            logger.warn("Invalid coupon ID");
            return false;
        }
        if (couponDAO == null) {
            throw new IllegalStateException("CouponDAO not initialized - cannot delete coupon");
        }
        
        return couponDAO.deleteCoupon(couponId);
    }
}
