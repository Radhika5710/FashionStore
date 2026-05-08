package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.WishlistDAO;
import com.fashionstore.model.WishlistItem;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class WishlistDAOImpl implements WishlistDAO {

    private static final Logger logger = LoggerFactory.getLogger(WishlistDAOImpl.class);

    @Override
    public boolean addWishlistItem(int userId, int productId) {
        if (isProductInWishlist(userId, productId)) {
            return false;
        }
        
        String sql = "INSERT INTO wishlist_items (user_id, product_id) VALUES (?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
            
        } catch (Exception e) {
            logger.error("WishlistDAOImpl.addWishlistItem Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public boolean removeWishlistItem(int userId, int productId) {
        String sql = "DELETE FROM wishlist_items WHERE user_id = ? AND product_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
            
        } catch (Exception e) {
            logger.error("WishlistDAOImpl.removeWishlistItem Error: {}", e.getMessage());
        }
        return false;
    }

    @Override
    public List<WishlistItem> getWishlistByUserId(int userId) {
        List<WishlistItem> list = new ArrayList<>();
        String sql = "SELECT w.wishlist_item_id, w.user_id, w.product_id, w.created_at, " +
                     "p.product_name, p.image_url, p.price " +
                     "FROM wishlist_items w " +
                     "JOIN products p ON w.product_id = p.product_id " +
                     "WHERE w.user_id = ? " +
                     "ORDER BY w.created_at DESC";
                     
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                WishlistItem item = new WishlistItem();
                item.setId(rs.getInt("wishlist_item_id"));
                item.setUserId(rs.getInt("user_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setCreatedAt(rs.getTimestamp("created_at"));
                item.setProductName(rs.getString("product_name"));
                item.setImageUrl(rs.getString("image_url"));
                item.setPrice(rs.getDouble("price"));
                list.add(item);
            }
            
        } catch (Exception e) {
            logger.error("WishlistDAOImpl.getWishlistByUserId Error: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public boolean isProductInWishlist(int userId, int productId) {
        String sql = "SELECT 1 FROM wishlist_items WHERE user_id = ? AND product_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
            
        } catch (Exception e) {
            logger.error("WishlistDAOImpl.isProductInWishlist Error: {}", e.getMessage());
        }
        return false;
    }
}
