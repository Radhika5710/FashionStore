package com.fashionstore.daoimpl;

import com.fashionstore.dao.NewsletterDAO;
import com.fashionstore.model.Newsletter;
import com.fashionstore.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NewsletterDAOImpl implements NewsletterDAO {
    private static final Logger logger = LoggerFactory.getLogger(NewsletterDAOImpl.class);

    @Override
    public boolean subscribe(String email) {
        String sql = "INSERT INTO newsletters (email, subscribed) VALUES (?, true) ON DUPLICATE KEY UPDATE subscribed = true";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error subscribing: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean unsubscribe(String email) {
        String sql = "UPDATE newsletters SET subscribed = false WHERE email = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error unsubscribing: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isSubscribed(String email) {
        String sql = "SELECT subscribed FROM newsletters WHERE email = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBoolean("subscribed");
            }
        } catch (SQLException e) {
            logger.error("Error checking subscription: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<Newsletter> getAllSubscribers() {
        List<Newsletter> subscribers = new ArrayList<>();
        String sql = "SELECT id, email, subscribed, subscribed_at FROM newsletters ORDER BY subscribed_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) subscribers.add(mapNewsletter(rs));
        } catch (SQLException e) {
            logger.error("Error getting subscribers: {}", e.getMessage(), e);
        }
        return subscribers;
    }

    @Override
    public List<Newsletter> getActiveSubscribers() {
        List<Newsletter> subscribers = new ArrayList<>();
        String sql = "SELECT id, email, subscribed, subscribed_at FROM newsletters WHERE subscribed = true ORDER BY subscribed_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) subscribers.add(mapNewsletter(rs));
        } catch (SQLException e) {
            logger.error("Error getting active subscribers: {}", e.getMessage(), e);
        }
        return subscribers;
    }

    private Newsletter mapNewsletter(ResultSet rs) throws SQLException {
        Newsletter newsletter = new Newsletter();
        newsletter.setId(rs.getInt("id"));
        newsletter.setEmail(rs.getString("email"));
        newsletter.setSubscribed(rs.getBoolean("subscribed"));
        newsletter.setSubscribedAt(rs.getTimestamp("subscribed_at") != null ? rs.getTimestamp("subscribed_at").toLocalDateTime() : null);
        return newsletter;
    }
}
