package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.PaymentDAO;
import com.fashionstore.model.Payment;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO implementation for Payment operations
 */
public class PaymentDAOImpl implements PaymentDAO {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDAOImpl.class);

    @Override
    public int createPayment(Payment payment) {
        String sql = "INSERT INTO payments (order_id, payment_method, transaction_id, amount, currency, status, gateway_response, payment_signature, webhook_id, verified) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, payment.getOrderId());
            ps.setString(2, payment.getPaymentMethod());
            ps.setString(3, payment.getTransactionId());
            ps.setBigDecimal(4, payment.getAmount());
            ps.setString(5, payment.getCurrency());
            ps.setString(6, payment.getStatus());
            ps.setString(7, payment.getGatewayResponse());
            ps.setString(8, payment.getPaymentSignature());
            ps.setString(9, payment.getWebhookId());
            ps.setBoolean(10, payment.isVerified());
            
            int result = ps.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.createPayment Error: {}", e.getMessage());
        }
        return -1;
    }

    @Override
    public Payment getPaymentById(int paymentId) {
        String sql = "SELECT * FROM payments WHERE payment_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, paymentId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractPaymentFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.getPaymentById Error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Payment getPaymentByTransactionId(String transactionId) {
        String sql = "SELECT * FROM payments WHERE transaction_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, transactionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractPaymentFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.getPaymentByTransactionId Error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public Payment getPaymentByOrderId(int orderId) {
        String sql = "SELECT * FROM payments WHERE order_id = ? ORDER BY payment_id DESC LIMIT 1";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, orderId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractPaymentFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.getPaymentByOrderId Error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean updatePaymentStatus(int paymentId, String status) {
        String sql = "UPDATE payments SET status = ?, updated_at = NOW() WHERE payment_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, status);
            ps.setInt(2, paymentId);
            
            int result = ps.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.updatePaymentStatus Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean updatePaymentVerification(int paymentId, boolean verified, String webhookId) {
        String sql = "UPDATE payments SET verified = ?, webhook_id = ?, updated_at = NOW() WHERE payment_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setBoolean(1, verified);
            ps.setString(2, webhookId);
            ps.setInt(3, paymentId);
            
            int result = ps.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.updatePaymentVerification Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public List<Payment> getPaymentsByOrderId(int orderId) {
        String sql = "SELECT * FROM payments WHERE order_id = ? ORDER BY created_at DESC";
        List<Payment> payments = new ArrayList<>();
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, orderId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(extractPaymentFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.getPaymentsByOrderId Error: {}", e.getMessage());
        }
        return payments;
    }

    @Override
    public List<Payment> getPaymentsByUserId(int userId) {
        String sql = "SELECT p.* FROM payments p INNER JOIN orders o ON p.order_id = o.order_id WHERE o.user_id = ? ORDER BY p.created_at DESC";
        List<Payment> payments = new ArrayList<>();
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(extractPaymentFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("PaymentDAOImpl.getPaymentsByUserId Error: {}", e.getMessage());
        }
        return payments;
    }

    @Override
    public boolean logPaymentDetails(Payment payment) {
        return createPayment(payment) > 0;
    }

    private Payment extractPaymentFromResultSet(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(rs.getInt("payment_id"));
        payment.setOrderId(rs.getInt("order_id"));
        payment.setPaymentMethod(rs.getString("payment_method"));
        payment.setTransactionId(rs.getString("transaction_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setCurrency(rs.getString("currency"));
        payment.setStatus(rs.getString("status"));
        payment.setGatewayResponse(rs.getString("gateway_response"));
        payment.setPaymentSignature(rs.getString("payment_signature"));
        payment.setWebhookId(rs.getString("webhook_id"));
        payment.setVerified(rs.getBoolean("verified"));
        
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            payment.setCreatedAt(createdTs.toLocalDateTime());
        }
        
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            payment.setUpdatedAt(updatedTs.toLocalDateTime());
        }
        
        return payment;
    }
}
