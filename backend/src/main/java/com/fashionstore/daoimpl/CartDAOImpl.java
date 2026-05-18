package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.CartDAO;
import com.fashionstore.model.CartItem;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CartDAOImpl implements CartDAO {

    private static final Logger logger = LoggerFactory.getLogger(CartDAOImpl.class);

    @Override
    public int addToCart(CartItem item) {
        if (item == null || item.getUserId() <= 0 || item.getProductId() <= 0 || item.getQuantity() <= 0) {
            logger.error("Invalid cart item: {}", item);
            return 0;
        }

        String checkSql = "SELECT cart_item_id, quantity FROM cart_items WHERE user_id = ? AND product_id = ? AND size_label = ?";
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE cart_item_id = ?";
        String insertSql = "INSERT INTO cart_items (user_id, product_id, size_label, quantity) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                logger.error("Failed to get database connection for cart operation");
                return 0;
            }

            boolean originalAutoCommit = con.getAutoCommit();

            try {
                // Disable auto-commit for transaction consistency
                con.setAutoCommit(false);

                try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                    checkPs.setInt(1, item.getUserId());
                    checkPs.setInt(2, item.getProductId());
                    checkPs.setString(3, item.getSizeLabel() != null ? item.getSizeLabel() : "M");

                    try (ResultSet rs = checkPs.executeQuery()) {
                        if (rs.next()) {
                            int existingId = rs.getInt("cart_item_id");
                            int currentQuantity = rs.getInt("quantity");

                            // Prevent excessive quantities
                            int newQuantity = Math.min(currentQuantity + item.getQuantity(), 100);

                            try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                                updatePs.setInt(1, item.getQuantity());
                                updatePs.setInt(2, existingId);
                                int result = updatePs.executeUpdate();
                                con.commit();
                                return result;
                            }
                        } else {
                            try (PreparedStatement insertPs = con.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                                insertPs.setInt(1, item.getUserId());
                                insertPs.setInt(2, item.getProductId());
                                insertPs.setString(3, item.getSizeLabel() != null ? item.getSizeLabel() : "M");
                                insertPs.setInt(4, item.getQuantity());

                                int result = insertPs.executeUpdate();
                                con.commit();

                                if (result > 0) {
                                    try (ResultSet generatedKeys = insertPs.getGeneratedKeys()) {
                                        if (generatedKeys.next()) {
                                            return generatedKeys.getInt(1);
                                        }
                                    }
                                }
                                return result;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(originalAutoCommit);
            }
        } catch (Exception e) {
            logger.error("Error adding to cart for user {}: {}", item.getUserId(), e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<CartItem> getCartItemsByUserId(int userId) {
        List<CartItem> list = new ArrayList<>();
        // PREVIOUS N+1 ISSUE: Product details fetched separately for each cart item
        // NEW OPTIMIZED STRATEGY: JOIN with products table to fetch all details in single query
        String sql = "SELECT ci.cart_item_id, ci.user_id, ci.product_id, ci.size_label, ci.quantity, " +
                     "p.product_name, p.image_url, p.price " +
                     "FROM cart_items ci " +
                     "JOIN products p ON ci.product_id = p.product_id " +
                     "WHERE ci.user_id = ?";

        long startTime = System.currentTimeMillis();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem item = new CartItem();
                    item.setCartItemId(rs.getInt("cart_item_id"));
                    item.setUserId(rs.getInt("user_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setSizeLabel(rs.getString("size_label"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setProductName(rs.getString("product_name"));
                    item.setImageUrl(rs.getString("image_url"));
                    item.setPrice(rs.getDouble("price"));
                    list.add(item);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 200) {
                logger.warn("Slow query detected: getCartItemsByUserId took {}ms for user {}", duration, userId);
            } else {
                logger.debug("getCartItemsByUserId completed in {}ms, fetched {} items for user {}", duration, list.size(), userId);
            }

        } catch (Exception e) {
            logger.error("Error fetching cart for user {}: {}", userId, e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean removeCartItem(int cartItemId, int userId) {
        String sql = "DELETE FROM cart_items WHERE cart_item_id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cartItemId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error removing cart item {} for user {}: {}", cartItemId, userId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean updateQuantity(int cartItemId, int userId, int quantity) {
        if (quantity <= 0) {
            return removeCartItem(cartItemId, userId);
        }
        String sql = "UPDATE cart_items SET quantity = ? WHERE cart_item_id = ? AND user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, cartItemId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error("Error updating quantity for cart item {} for user {}: {}", cartItemId, userId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean clearCartByUserId(int userId) {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            logger.error("Error clearing cart for user {}: {}", userId, e.getMessage(), e);
        }
        return false;
    }
}