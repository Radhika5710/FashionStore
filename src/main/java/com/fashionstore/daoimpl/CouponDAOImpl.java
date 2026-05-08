package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.CouponDAO;
import com.fashionstore.model.Coupon;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CouponDAOImpl implements CouponDAO {

    private static final Logger logger = LoggerFactory.getLogger(CouponDAOImpl.class);

    @Override
    public boolean addCoupon(Coupon coupon) {
        String sql = "INSERT INTO coupons (code, description, discount_type, discount_value, minimum_order_amount, " +
                     "maximum_discount_amount, usage_limit, usage_count, user_usage_limit, valid_from, valid_until, " +
                     "is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, coupon.getCode());
            ps.setString(2, coupon.getDescription());
            ps.setString(3, coupon.getDiscountType());
            ps.setDouble(4, coupon.getDiscountValue());
            ps.setDouble(5, coupon.getMinimumOrderAmount());
            if (coupon.getMaximumDiscountAmount() != null) {
                ps.setDouble(6, coupon.getMaximumDiscountAmount());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }
            if (coupon.getUsageLimit() != null) {
                ps.setInt(7, coupon.getUsageLimit());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setInt(8, coupon.getUsageCount());
            ps.setInt(9, coupon.getUserUsageLimit());
            ps.setTimestamp(10, new Timestamp(coupon.getValidFrom().getTime()));
            ps.setTimestamp(11, new Timestamp(coupon.getValidUntil().getTime()));
            ps.setBoolean(12, coupon.isActive());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.addCoupon Error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateCoupon(Coupon coupon) {
        String sql = "UPDATE coupons SET code = ?, description = ?, discount_type = ?, discount_value = ?, " +
                     "minimum_order_amount = ?, maximum_discount_amount = ?, usage_limit = ?, valid_from = ?, " +
                     "valid_until = ?, is_active = ?, updated_at = NOW() WHERE coupon_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, coupon.getCode());
            ps.setString(2, coupon.getDescription());
            ps.setString(3, coupon.getDiscountType());
            ps.setDouble(4, coupon.getDiscountValue());
            ps.setDouble(5, coupon.getMinimumOrderAmount());
            if (coupon.getMaximumDiscountAmount() != null) {
                ps.setDouble(6, coupon.getMaximumDiscountAmount());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }
            if (coupon.getUsageLimit() != null) {
                ps.setInt(7, coupon.getUsageLimit());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setTimestamp(8, new Timestamp(coupon.getValidFrom().getTime()));
            ps.setTimestamp(9, new Timestamp(coupon.getValidUntil().getTime()));
            ps.setBoolean(10, coupon.isActive());
            ps.setInt(11, coupon.getCouponId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.updateCoupon Error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteCoupon(int couponId) {
        String sql = "DELETE FROM coupons WHERE coupon_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, couponId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.deleteCoupon Error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Coupon getCouponById(int couponId) {
        String sql = "SELECT * FROM coupons WHERE coupon_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, couponId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return extractCouponFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.getCouponById Error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Coupon getCouponByCode(String code) {
        String sql = "SELECT * FROM coupons WHERE code = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return extractCouponFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.getCouponByCode Error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public List<Coupon> getAllCoupons() {
        String sql = "SELECT * FROM coupons ORDER BY created_at DESC";
        List<Coupon> coupons = new ArrayList<>();
        
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                coupons.add(extractCouponFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.getAllCoupons Error: {}", e.getMessage());
        }
        return coupons;
    }

    @Override
    public List<Coupon> getActiveCoupons() {
        String sql = "SELECT * FROM coupons WHERE is_active = true AND valid_from <= NOW() AND valid_until >= NOW() " +
                     "ORDER BY created_at DESC";
        List<Coupon> coupons = new ArrayList<>();
        
        try (Connection con = DBConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                coupons.add(extractCouponFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.getActiveCoupons Error: {}", e.getMessage());
        }
        return coupons;
    }

    @Override
    public boolean recordCouponUsage(int couponId, int userId, int orderId) {
        String sql = "INSERT INTO coupon_usage (coupon_id, user_id, order_id, used_at) VALUES (?, ?, ?, NOW())";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            ps.setInt(3, orderId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.recordCouponUsage Error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int getCouponUsageCount(int couponId) {
        String sql = "SELECT COUNT(*) FROM coupon_usage WHERE coupon_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, couponId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.getCouponUsageCount Error: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public int getUserCouponUsageCount(int couponId, int userId) {
        String sql = "SELECT COUNT(*) FROM coupon_usage WHERE coupon_id = ? AND user_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.getUserCouponUsageCount Error: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean isCouponUsedByUser(int couponId, int userId) {
        return getUserCouponUsageCount(couponId, userId) > 0;
    }

    @Override
    public boolean isCouponValid(String code, double orderAmount, int userId, int productId, int categoryId) {
        Coupon coupon = getCouponByCode(code);
        if (coupon == null) {
            return false;
        }
        
        // Check if coupon is active
        if (!coupon.isActive()) {
            return false;
        }
        
        // Check validity period
        java.util.Date now = new java.util.Date();
        if (now.before(coupon.getValidFrom()) || now.after(coupon.getValidUntil())) {
            return false;
        }
        
        // Check minimum order amount
        if (orderAmount < coupon.getMinimumOrderAmount()) {
            return false;
        }
        
        // Check usage limit
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            return false;
        }
        
        // Check user usage limit (if applicable)
        if (coupon.getUserUsageLimit() > 0 && getUserCouponUsageCount(coupon.getCouponId(), userId) >= coupon.getUserUsageLimit()) {
            return false;
        }
        
        // Check product/category applicability using model's isApplicableToProduct method
        if (!coupon.isApplicableToProduct(productId, categoryId)) {
            return false;
        }
        
        return true;
    }

    @Override
    public List<Coupon> getApplicableCoupons(double orderAmount, int userId, int productId, int categoryId) {
        List<Coupon> allCoupons = getActiveCoupons();
        List<Coupon> applicable = new ArrayList<>();
        
        for (Coupon coupon : allCoupons) {
            if (isCouponValid(coupon.getCode(), orderAmount, userId, productId, categoryId)) {
                applicable.add(coupon);
            }
        }
        
        return applicable;
    }

    @Override
    public boolean couponExists(String code) {
        String sql = "SELECT COUNT(*) FROM coupons WHERE code = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.couponExists Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean incrementCouponUsage(int couponId) {
        String sql = "UPDATE coupons SET usage_count = usage_count + 1 WHERE coupon_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, couponId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("CouponDAOImpl.incrementCouponUsage Error: {}", e.getMessage());
            return false;
        }
    }

    private Coupon extractCouponFromResultSet(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();
        coupon.setCouponId(rs.getInt("coupon_id"));
        coupon.setCode(rs.getString("code"));
        coupon.setDescription(rs.getString("description"));
        coupon.setDiscountType(rs.getString("discount_type"));
        coupon.setDiscountValue(rs.getDouble("discount_value"));
        coupon.setMinimumOrderAmount(rs.getDouble("minimum_order_amount"));
        double maxDiscount = rs.getDouble("maximum_discount_amount");
        coupon.setMaximumDiscountAmount(rs.wasNull() ? null : maxDiscount);
        int usageLimit = rs.getInt("usage_limit");
        coupon.setUsageLimit(rs.wasNull() ? null : usageLimit);
        coupon.setUsageCount(rs.getInt("usage_count"));
        coupon.setUserUsageLimit(rs.getInt("user_usage_limit"));
        coupon.setValidFrom(rs.getTimestamp("valid_from"));
        coupon.setValidUntil(rs.getTimestamp("valid_until"));
        coupon.setActive(rs.getBoolean("is_active"));
        coupon.setCreatedAt(rs.getTimestamp("created_at"));
        coupon.setUpdatedAt(rs.getTimestamp("updated_at"));
        return coupon;
    }
}
