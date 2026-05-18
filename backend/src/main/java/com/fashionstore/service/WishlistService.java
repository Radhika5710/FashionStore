package com.fashionstore.service;

import com.fashionstore.dao.WishlistDAO;
import com.fashionstore.model.WishlistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Wishlist service for managing user wishlists
 */
public class WishlistService {
    private static final Logger logger = LoggerFactory.getLogger(WishlistService.class);
    private WishlistDAO wishlistDAO;

    public WishlistService() {
        this.wishlistDAO = null;
    }

    public void setWishlistDAO(WishlistDAO wishlistDAO) {
        if (this.wishlistDAO == null) {
            try {
                java.lang.reflect.Field field = WishlistService.class.getDeclaredField("wishlistDAO");
                field.setAccessible(true);
                field.set(this, wishlistDAO);
            } catch (Exception e) {
                logger.error("Failed to set wishlistDAO", e);
            }
        }
    }

    /**
     * Add item to wishlist
     */
    public Map<String, Object> addToWishlist(int userId, int productId) {
        Map<String, Object> result = new HashMap<>();
        if (wishlistDAO == null) {
            logger.warn("WishlistDAO not initialized");
            result.put("success", false);
            result.put("message", "Service not available");
            return result;
        }
        try {
            boolean added = wishlistDAO.addWishlistItem(userId, productId);
            result.put("success", added);
            result.put("message", added ? "Item added to wishlist successfully" : "Item already in wishlist");
        } catch (Exception e) {
            logger.error("Error adding item to wishlist: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Failed to add item to wishlist");
        }
        return result;
    }

    /**
     * Remove item from wishlist
     */
    public Map<String, Object> removeFromWishlist(int userId, int productId) {
        Map<String, Object> result = new HashMap<>();
        if (wishlistDAO == null) {
            logger.warn("WishlistDAO not initialized");
            result.put("success", false);
            result.put("message", "Service not available");
            return result;
        }
        try {
            boolean removed = wishlistDAO.removeWishlistItem(userId, productId);
            result.put("success", removed);
            result.put("message", removed ? "Item removed from wishlist successfully" : "Item not found in wishlist");
        } catch (Exception e) {
            logger.error("Error removing item from wishlist: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Failed to remove item from wishlist");
        }
        return result;
    }

    /**
     * Get user wishlist
     */
    public Map<String, Object> getWishlist(int userId) {
        Map<String, Object> result = new HashMap<>();
        if (wishlistDAO == null) {
            logger.warn("WishlistDAO not initialized");
            result.put("success", false);
            result.put("message", "Service not available");
            return result;
        }
        try {
            List<WishlistItem> wishlist = wishlistDAO.getWishlistByUserId(userId);
            result.put("success", true);
            result.put("message", "Wishlist retrieved successfully");
            result.put("wishlist", wishlist);
        } catch (Exception e) {
            logger.error("Error getting wishlist: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Failed to get wishlist");
        }
        return result;
    }

    /**
     * Get wishlist items for a user
     */
    public List<WishlistItem> getWishlistItems(int userId) {
        if (wishlistDAO == null) {
            logger.warn("WishlistDAO not initialized");
            return new java.util.ArrayList<>();
        }
        try {
            return wishlistDAO.getWishlistByUserId(userId);
        } catch (Exception e) {
            logger.error("Error getting wishlist items: {}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Check if product is in wishlist
     */
    public boolean isProductInWishlist(int userId, int productId) {
        if (wishlistDAO == null) {
            logger.warn("WishlistDAO not initialized");
            return false;
        }
        try {
            return wishlistDAO.isProductInWishlist(userId, productId);
        } catch (Exception e) {
            logger.error("Error checking if product is in wishlist: {}", e.getMessage(), e);
            return false;
        }
    }
}
