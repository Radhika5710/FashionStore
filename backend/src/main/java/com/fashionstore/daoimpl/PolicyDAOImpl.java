package com.fashionstore.daoimpl;

import com.fashionstore.dao.PolicyDAO;
import com.fashionstore.model.Policy;
import com.fashionstore.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PolicyDAOImpl implements PolicyDAO {
    private static final Logger logger = LoggerFactory.getLogger(PolicyDAOImpl.class);

    @Override
    public Policy getPolicyByType(String policyType) {
        String sql = "SELECT policy_id, policy_type, title, content, is_active, last_updated FROM policies WHERE policy_type = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, policyType);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPolicy(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting policy: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Policy> getAllPolicies() {
        List<Policy> policies = new ArrayList<>();
        String sql = "SELECT policy_id, policy_type, title, content, is_active, last_updated FROM policies ORDER BY policy_type";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) policies.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Error getting all policies: {}", e.getMessage(), e);
        }
        return policies;
    }

    @Override
    public List<Policy> getActivePolicies() {
        List<Policy> policies = new ArrayList<>();
        String sql = "SELECT policy_id, policy_type, title, content, is_active, last_updated FROM policies WHERE is_active = true ORDER BY policy_type";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) policies.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Error getting active policies: {}", e.getMessage(), e);
        }
        return policies;
    }

    @Override
    public boolean updatePolicy(Policy policy) {
        String sql = "UPDATE policies SET title = ?, content = ?, is_active = ?, last_updated = NOW() WHERE policy_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, policy.getTitle());
            ps.setString(2, policy.getContent());
            ps.setBoolean(3, policy.isActive());
            ps.setInt(4, policy.getPolicyId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating policy: {}", e.getMessage(), e);
            return false;
        }
    }

    private Policy mapPolicy(ResultSet rs) throws SQLException {
        Policy policy = new Policy();
        policy.setPolicyId(rs.getInt("policy_id"));
        policy.setPolicyType(rs.getString("policy_type"));
        policy.setTitle(rs.getString("title"));
        policy.setContent(rs.getString("content"));
        policy.setActive(rs.getBoolean("is_active"));
        policy.setLastUpdated(rs.getTimestamp("last_updated") != null ? rs.getTimestamp("last_updated").toLocalDateTime() : null);
        return policy;
    }
}
