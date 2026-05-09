package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.OrderDAO;
import com.fashionstore.model.Order;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAOImpl implements OrderDAO {

    private static final Logger logger = LoggerFactory.getLogger(OrderDAOImpl.class);

    // Convert ResultSet → Order
    private Order mapOrder(ResultSet rs) throws Exception {
        Order order = new Order();

        order.setOrderId(rs.getInt("order_id"));
        order.setUserId(rs.getInt("user_id"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setFullName(rs.getString("full_name"));
        order.setAddress(rs.getString("address"));
        order.setCity(rs.getString("city"));
        order.setState(rs.getString("state"));
        order.setZip(rs.getString("zip"));
        order.setPhone(rs.getString("phone"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setStatus(rs.getString("status"));
        order.setOrderDate(rs.getTimestamp("created_at"));

        return order;
    }

    // Create Order
    // NOTE: schema requires subtotal NOT NULL (default 0.00), total_amount NOT NULL.
    // We populate subtotal = total_amount because the cart-to-order flow does not yet
    // separate tax/shipping/discount lines. payment_status defaults to 'pending' in DB.
    @Override
    public int createOrder(Order order) {

        String sql = "INSERT INTO orders " +
                     "(user_id, subtotal, total_amount, full_name, address, city, state, zip, phone, payment_method, status, payment_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        java.math.BigDecimal totalDec = java.math.BigDecimal.valueOf(order.getTotalAmount())
                .setScale(2, java.math.RoundingMode.HALF_UP);

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, order.getUserId());
            ps.setBigDecimal(2, totalDec);
            ps.setBigDecimal(3, totalDec);
            ps.setString(4, order.getFullName());
            ps.setString(5, order.getAddress());
            ps.setString(6, order.getCity());
            ps.setString(7, order.getState());
            ps.setString(8, order.getZip());
            ps.setString(9, order.getPhone());
            ps.setString(10, order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD");
            ps.setString(11, order.getStatus() != null ? order.getStatus() : "Pending");
            ps.setString(12, "pending");

            int rows = ps.executeUpdate();

            if (rows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in createOrder for user {}: {}", order.getUserId(), e.getMessage(), e);
        }

        return 0;
    }

    // Get Order By ID
    @Override
    public Order getOrderById(int orderId) {

        String sql = "SELECT * FROM orders WHERE order_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapOrder(rs);
            }

        } catch (Exception e) {
            logger.error("Error in getOrderById for ID {}: {}", orderId, e.getMessage(), e);
        }

        return null;
    }

    // Get Orders by User
    @Override
    public List<Order> getOrdersByUserId(int userId) {

        List<Order> list = new ArrayList<>();

        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC, order_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapOrder(rs));
            }

        } catch (Exception e) {
            logger.error("Error in getOrdersByUserId for user {}: {}", userId, e.getMessage(), e);
        }

        return list;
    }

    @Override
    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY created_at DESC, order_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapOrder(rs));
            }

        } catch (Exception e) {
            logger.error("Error in getAllOrders: {}", e.getMessage(), e);
        }

        return list;
    }

    @Override
    public boolean updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("Error in updateOrderStatus for ID {}: {}", orderId, e.getMessage(), e);
        }

        return false;
    }

    // Analytics methods
    @Override
    public double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) as total FROM orders WHERE status != 'Cancelled'";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (Exception e) {
            logger.error("Error in getTotalRevenue: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public int getTotalOrderCount() {
        String sql = "SELECT COUNT(*) as count FROM orders";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("Error in getTotalOrderCount: {}", e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY created_at DESC LIMIT ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        } catch (Exception e) {
            logger.error("Error in getRecentOrders: {}", e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Order> getOrdersInLastDays(int days) {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) ORDER BY created_at DESC";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                list.add(mapOrder(rs));
            }
        } catch (Exception e) {
            logger.error("Error in getOrdersInLastDays for {} days: {}", days, e.getMessage(), e);
        }
        return list;
    }
}
