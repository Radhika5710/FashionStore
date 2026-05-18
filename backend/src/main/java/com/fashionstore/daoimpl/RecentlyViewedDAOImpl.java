package com.fashionstore.daoimpl;

import com.fashionstore.dao.RecentlyViewedDAO;
import com.fashionstore.model.RecentlyViewed;
import com.fashionstore.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecentlyViewedDAOImpl implements RecentlyViewedDAO {
    private static final Logger logger = LoggerFactory.getLogger(RecentlyViewedDAOImpl.class);

    @Override
    public boolean addRecentlyViewed(int userId, int productId) {
        String sql = "INSERT INTO recently_viewed (user_id, product_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error adding recently viewed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<RecentlyViewed> getRecentlyViewedByUserId(int userId) {
        List<RecentlyViewed> items = new ArrayList<>();
        String sql = "SELECT id, user_id, product_id, viewed_at FROM recently_viewed WHERE user_id = ? ORDER BY viewed_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting recently viewed: {}", e.getMessage(), e);
        }
        return items;
    }

    @Override
    public List<RecentlyViewed> getRecentlyViewedByUserId(int userId, int limit) {
        List<RecentlyViewed> items = new ArrayList<>();
        String sql = "SELECT id, user_id, product_id, viewed_at FROM recently_viewed WHERE user_id = ? ORDER BY viewed_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) items.add(mapItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting recently viewed: {}", e.getMessage(), e);
        }
        return items;
    }

    @Override
    public boolean clearRecentlyViewed(int userId) {
        String sql = "DELETE FROM recently_viewed WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error clearing recently viewed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeRecentlyViewed(int userId, int productId) {
        return deleteRecentlyViewed(userId, productId);
    }

    @Override
    public int getRecentlyViewedCount(int userId) {
        String sql = "SELECT COUNT(*) FROM recently_viewed WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting count: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public boolean updateTimestamp(int userId, int productId) {
        String sql = "UPDATE recently_viewed SET viewed_at = NOW() WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating timestamp: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteRecentlyViewed(int userId, int productId) {
        String sql = "DELETE FROM recently_viewed WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting recently viewed: {}", e.getMessage(), e);
            return false;
        }
    }

    private RecentlyViewed mapItem(ResultSet rs) throws SQLException {
        RecentlyViewed item = new RecentlyViewed();
        item.setId(rs.getInt("id"));
        item.setUserId(rs.getInt("user_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setViewedAt(rs.getTimestamp("viewed_at") != null ? rs.getTimestamp("viewed_at").toLocalDateTime() : null);
        return item;
    }
}
