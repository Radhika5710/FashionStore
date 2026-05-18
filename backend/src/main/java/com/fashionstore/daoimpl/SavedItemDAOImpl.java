package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.SavedItemDAO;
import com.fashionstore.model.SavedItem;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SavedItemDAOImpl implements SavedItemDAO {

    private static final Logger logger = LoggerFactory.getLogger(SavedItemDAOImpl.class);

    @Override
    public boolean saveItem(SavedItem savedItem) {
        String sql = "INSERT INTO saved_items (user_id, product_id, size_label) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE saved_at = CURRENT_TIMESTAMP";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, savedItem.getUserId());
            ps.setInt(2, savedItem.getProductId());
            ps.setString(3, savedItem.getSizeLabel());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("SavedItemDAOImpl.saveItem Error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeItem(int savedItemId) {
        String sql = "DELETE FROM saved_items WHERE saved_item_id = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, savedItemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("SavedItemDAOImpl.removeItem Error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<SavedItem> getSavedItemsByUserId(int userId) {
        String sql = "SELECT si.*, p.product_name, p.image_url, p.price " +
                     "FROM saved_items si " +
                     "JOIN products p ON si.product_id = p.product_id " +
                     "WHERE si.user_id = ? ORDER BY si.saved_at DESC";
        List<SavedItem> savedItems = new ArrayList<>();
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SavedItem item = new SavedItem();
                    item.setSavedItemId(rs.getInt("saved_item_id"));
                    item.setUserId(rs.getInt("user_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setSizeLabel(rs.getString("size_label"));
                    item.setSavedAt(rs.getTimestamp("saved_at"));
                    item.setProductName(rs.getString("product_name"));
                    item.setImageUrl(rs.getString("image_url"));
                    item.setPrice(rs.getDouble("price"));
                    savedItems.add(item);
                }
            }
        } catch (SQLException e) {
            logger.error("SavedItemDAOImpl.getSavedItemsByUserId Error: {}", e.getMessage(), e);
        }
        return savedItems;
    }

    @Override
    public SavedItem getSavedItem(int userId, int productId, String sizeLabel) {
        String sql = "SELECT saved_item_id, user_id, product_id, size_label, saved_at FROM saved_items WHERE user_id = ? AND product_id = ? AND size_label = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setString(3, sizeLabel);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SavedItem item = new SavedItem();
                    item.setSavedItemId(rs.getInt("saved_item_id"));
                    item.setUserId(rs.getInt("user_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setSizeLabel(rs.getString("size_label"));
                    item.setSavedAt(rs.getTimestamp("saved_at"));
                    return item;
                }
            }
        } catch (SQLException e) {
            logger.error("SavedItemDAOImpl.getSavedItem Error: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean moveToCart(int savedItemId) {
        String selectSql = "SELECT user_id, product_id, size_label FROM saved_items WHERE saved_item_id = ?";
        String findCartSql = "SELECT cart_item_id, quantity FROM cart_items WHERE user_id = ? AND product_id = ? AND size_label = ?";
        String updateCartSql = "UPDATE cart_items SET quantity = quantity + 1 WHERE cart_item_id = ?";
        String insertCartSql = "INSERT INTO cart_items (user_id, product_id, size_label, quantity) VALUES (?, ?, ?, 1)";
        String deleteSavedSql = "DELETE FROM saved_items WHERE saved_item_id = ?";

        try {
            return com.fashionstore.util.TransactionManager.executeInTransaction(con -> {
                int userId = 0, productId = 0;
                String sizeLabel = null;

                try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                    ps.setInt(1, savedItemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("user_id");
                            productId = rs.getInt("product_id");
                            sizeLabel = rs.getString("size_label");
                        }
                    }
                }

                if (userId == 0) {
                    return false;
                }

                String resolvedSize = sizeLabel != null ? sizeLabel : "M";

                Integer existingCartItemId = null;
                try (PreparedStatement ps = con.prepareStatement(findCartSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.setString(3, resolvedSize);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingCartItemId = rs.getInt("cart_item_id");
                        }
                    }
                }

                if (existingCartItemId != null) {
                    try (PreparedStatement ps = con.prepareStatement(updateCartSql)) {
                        ps.setInt(1, existingCartItemId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = con.prepareStatement(insertCartSql)) {
                        ps.setInt(1, userId);
                        ps.setInt(2, productId);
                        ps.setString(3, resolvedSize);
                        ps.executeUpdate();
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(deleteSavedSql)) {
                    ps.setInt(1, savedItemId);
                    ps.executeUpdate();
                }

                return true;
            });
        } catch (Exception e) {
            logger.error("SavedItemDAOImpl.moveToCart Error: {}", e.getMessage(), e);
            return false;
        }
    }
}
