package com.fashionstore.serviceimpl;

import com.fashionstore.dao.RecentlyViewedDAO;
import com.fashionstore.model.RecentlyViewed;
import com.fashionstore.service.RecentlyViewedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RecentlyViewedServiceImpl - MVC Service Layer Implementation
 * Lightweight recently viewed tracking without recommendation engines
 */
public class RecentlyViewedServiceImpl implements RecentlyViewedService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecentlyViewedServiceImpl.class);
    private RecentlyViewedDAO recentlyViewedDAO;
    
    public RecentlyViewedServiceImpl() {
        this.recentlyViewedDAO = null;
    }
    
    public void setRecentlyViewedDAO(RecentlyViewedDAO recentlyViewedDAO) {
        if (this.recentlyViewedDAO == null) {
            try {
                java.lang.reflect.Field field = RecentlyViewedServiceImpl.class.getDeclaredField("recentlyViewedDAO");
                field.setAccessible(true);
                field.set(this, recentlyViewedDAO);
            } catch (Exception e) {
                logger.error("Failed to set recentlyViewedDAO", e);
            }
        }
    }
    
    @Override
    public boolean addRecentlyViewed(int userId, int productId) {
        if (recentlyViewedDAO == null) {
            logger.warn("RecentlyViewedDAO not initialized");
            return false;
        }
        try {
            // Check if already exists, update timestamp if so
            if (recentlyViewedDAO.getRecentlyViewedCount(userId) > 0) {
                List<RecentlyViewed> existing = recentlyViewedDAO.getRecentlyViewedByUserId(userId, 50);
                for (RecentlyViewed item : existing) {
                    if (item.getProductId() == productId) {
                        return recentlyViewedDAO.updateTimestamp(userId, productId);
                    }
                }
            }
            // If not exists or limit not reached, add new
            return recentlyViewedDAO.addRecentlyViewed(userId, productId);
        } catch (Exception e) {
            logger.error("Error adding recently viewed for user: {}, product: {}", userId, productId, e);
            return false;
        }
    }
    
    @Override
    public List<RecentlyViewed> getRecentlyViewedByUserId(int userId) {
        if (recentlyViewedDAO == null) {
            logger.warn("RecentlyViewedDAO not initialized");
            return new ArrayList<>();
        }
        try {
            return recentlyViewedDAO.getRecentlyViewedByUserId(userId, 10);
        } catch (Exception e) {
            logger.error("Error getting recently viewed for user: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<RecentlyViewed> getRecentlyViewedByUserId(int userId, int limit) {
        if (recentlyViewedDAO == null) {
            logger.warn("RecentlyViewedDAO not initialized");
            return new ArrayList<>();
        }
        try {
            return recentlyViewedDAO.getRecentlyViewedByUserId(userId, limit);
        } catch (Exception e) {
            logger.error("Error getting recently viewed for user: {} with limit: {}", userId, limit, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean clearRecentlyViewed(int userId) {
        if (recentlyViewedDAO == null) {
            logger.warn("RecentlyViewedDAO not initialized");
            return false;
        }
        try {
            return recentlyViewedDAO.clearRecentlyViewed(userId);
        } catch (Exception e) {
            logger.error("Error clearing recently viewed for user: {}", userId, e);
            return false;
        }
    }
    
    @Override
    public boolean removeRecentlyViewed(int userId, int productId) {
        if (recentlyViewedDAO == null) {
            logger.warn("RecentlyViewedDAO not initialized");
            return false;
        }
        try {
            return recentlyViewedDAO.removeRecentlyViewed(userId, productId);
        } catch (Exception e) {
            logger.error("Error removing recently viewed for user: {}, product: {}", userId, productId, e);
            return false;
        }
    }
    
    @Override
    public int getRecentlyViewedCount(int userId) {
        if (recentlyViewedDAO == null) {
            logger.warn("RecentlyViewedDAO not initialized");
            return 0;
        }
        try {
            return recentlyViewedDAO.getRecentlyViewedCount(userId);
        } catch (Exception e) {
            logger.error("Error getting recently viewed count for user: {}", userId, e);
            return 0;
        }
    }
}
