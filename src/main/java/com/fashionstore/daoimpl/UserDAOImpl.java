package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.UserDAO;
import com.fashionstore.model.User;
import com.fashionstore.util.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl implements UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAOImpl.class);

    // Convert ResultSet → User
    private User mapUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPassword(rs.getString("password"));
        user.setGender(rs.getString("gender"));
        user.setAddress(rs.getString("address"));
        user.setRole(rs.getString("role"));
        return user;
    }

    @Override
    public int registerUser(User user) {
        String sql = "INSERT INTO users (full_name, email, phone, password, gender, address) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            
            // ✅ Architecture Upgrade: Hash password before saving
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            ps.setString(4, hashedPassword);
            
            ps.setString(5, user.getGender());
            ps.setString(6, user.getAddress());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }

        } catch (Exception e) {
            logger.error("Error in registerUser for {}: {}", user.getEmail(), e.getMessage(), e);
        }

        return 0;
    }

    @Override
    public User loginUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                User user = mapUser(rs);
                // ✅ Architecture Upgrade: Verify BCrypt hash
                if (BCrypt.checkpw(password, user.getPassword())) {
                    return user;
                }
            }

        } catch (Exception e) {
            logger.error("Error in loginUser for {}: {}", email, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return mapUser(rs);

        } catch (Exception e) {
            logger.error("Error in getUserById for ID {}: {}", userId, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) return mapUser(rs);

        } catch (Exception e) {
            logger.error("Error in getUserByEmail for {}: {}", email, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public boolean isEmailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email);

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (Exception e) {
            logger.error("Error in isEmailExists for {}: {}", email, e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, gender = ?, address = ? WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getGender());
            ps.setString(4, user.getAddress());
            ps.setInt(5, user.getUserId());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("Error in updateUser for ID {}: {}", user.getUserId(), e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // ✅ Architecture Upgrade: Hash new password
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            ps.setString(1, hashedPassword);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("Error in changePassword for ID {}: {}", userId, e.getMessage(), e);
        }

        return false;
    }

    @Override
    public int getTotalUserCount() {
        String sql = "SELECT COUNT(*) as count FROM users";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("Error in getTotalUserCount: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY user_id DESC";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        } catch (Exception e) {
            logger.error("Error in getAllUsers: {}", e.getMessage(), e);
        }
        return users;
    }

    @Override
    public boolean updateUserRole(int userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, role);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error in updateUserRole for ID {}: {}", userId, e.getMessage(), e);
        }
        return false;
    }
}