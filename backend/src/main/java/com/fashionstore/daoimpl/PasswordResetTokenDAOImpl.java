package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.PasswordResetTokenDAO;
import com.fashionstore.model.PasswordResetToken;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class PasswordResetTokenDAOImpl implements PasswordResetTokenDAO {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenDAOImpl.class);

    @Override
    public int createToken(PasswordResetToken token) {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at, used) VALUES (?, ?, ?, ?)";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, token.getUserId());
            ps.setString(2, token.getToken());
            ps.setTimestamp(3, Timestamp.valueOf(token.getExpiresAt()));
            ps.setBoolean(4, token.isUsed());
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error creating password reset token: {}", e.getMessage(), e);
        }
        return -1;
    }

    @Override
    public PasswordResetToken getTokenByToken(String token) {
        String sql = "SELECT token_id, user_id, token, expires_at, used, created_at FROM password_reset_tokens WHERE token = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, token);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractTokenFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting token by token string: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public PasswordResetToken getTokenByUserId(int userId) {
        String sql = "SELECT token_id, user_id, token, expires_at, used, created_at FROM password_reset_tokens WHERE user_id = ? AND used = false AND expires_at > NOW() ORDER BY created_at DESC LIMIT 1";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractTokenFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting token by user ID: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean markTokenAsUsed(int tokenId) {
        String sql = "UPDATE password_reset_tokens SET used = true WHERE token_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, tokenId);
            
            int result = ps.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("Error marking token as used: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean invalidateToken(String token) {
        String sql = "UPDATE password_reset_tokens SET used = true WHERE token = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, token);
            
            int result = ps.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("Error invalidating token: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteExpiredTokens() {
        String sql = "DELETE FROM password_reset_tokens WHERE expires_at < NOW()";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            int result = ps.executeUpdate();
            logger.info("Deleted {} expired password reset tokens", result);
            return true;
        } catch (SQLException e) {
            logger.error("Error deleting expired tokens: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean deleteToken(int tokenId) {
        String sql = "DELETE FROM password_reset_tokens WHERE token_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, tokenId);
            
            int result = ps.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("Error deleting token: {}", e.getMessage(), e);
        }
        return false;
    }

    private PasswordResetToken extractTokenFromResultSet(ResultSet rs) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenId(rs.getInt("token_id"));
        token.setUserId(rs.getInt("user_id"));
        token.setToken(rs.getString("token"));
        
        Timestamp expiresTs = rs.getTimestamp("expires_at");
        if (expiresTs != null) {
            token.setExpiresAt(expiresTs.toLocalDateTime());
        }
        
        token.setUsed(rs.getBoolean("used"));
        
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            token.setCreatedAt(createdTs.toLocalDateTime());
        }
        
        return token;
    }
}
