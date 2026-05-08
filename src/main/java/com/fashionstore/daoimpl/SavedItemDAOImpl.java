package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.SavedItemDAO;
import com.fashionstore.dao.CartDAO;
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

    private CartDAO cartDAO;

    public SavedItemDAOImpl() {
        this.cartDAO = new com.fashionstore.daoimpl.CartDAOImpl();
    }

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
        String sql = "SELECT si.*, p.name as product_name, p.image_url, p.price " +
                     "FROM saved_items si " +
                     "JOIN products p ON si.product_id = p.product_id " +
                     "WHERE si.user_id = ? ORDER BY si.saved_at DESC";
        List<SavedItem> savedItems = new ArrayList<>();
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
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
        } catch (SQLException e) {
            logger.error("SavedItemDAOImpl.getSavedItemsByUserId Error: {}", e.getMessage());
        }
        return savedItems;
    }

    @Override
    public SavedItem getSavedItem(int userId, int productId, String sizeLabel) {
        String sql = "SELECT * FROM saved_items WHERE user_id = ? AND product_id = ? AND size_label = ?";
        
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setString(3, sizeLabel);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                SavedItem item = new SavedItem();
                item.setSavedItemId(rs.getInt("saved_item_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setSizeLabel(rs.getString("size_label"));
                item.setSavedAt(rs.getTimestamp("saved_at"));
                return item;
            }
        } catch (SQLException e) {
            logger.error("SavedItemDAOImpl.getSavedItem Error: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean moveToCart(int savedItemId) {
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);
            
            // Get saved item details
            String selectSql = "SELECT user_id, product_id, size_label FROM saved_items WHERE saved_item_id = ?";
            int userId = 0, productId = 0;
            String sizeLabel = null;
            
            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setInt(1, savedItemId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    userId = rs.getInt("user_id");
                    productId = rs.getInt("product_id");
                    sizeLabel = rs.getString("size_label");
                }
            }
            
            if (userId == 0) {
                con.rollback();
                return false;
            }
            
            // Add to cart using cartDAO
            com.fashionstore.model.CartItem cartItem = new com.fashionstore.model.CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setSizeLabel(sizeLabel != null ? sizeLabel : "M");
            cartItem.setQuantity(1);
            cartDAO.addToCart(cartItem);
            
            // Remove from saved items
            String deleteSql = "DELETE FROM saved_items WHERE saved_item_id = ?";
            try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
                ps.setInt(1, savedItemId);
                ps.executeUpdate();
            }
            
            con.commit();
            return true;
        } catch (Exception e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {}
            }
            logger.error("SavedItemDAOImpl.moveToCart Error: {}", e.getMessage());
            return false;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {}
            }
        }
    }
}
