package com.fashionstore.dao;

import com.fashionstore.model.RecentlyViewed;
import java.util.List;

public interface RecentlyViewedDAO {
    boolean addRecentlyViewed(int userId, int productId);
    List<RecentlyViewed> getRecentlyViewedByUserId(int userId);
    List<RecentlyViewed> getRecentlyViewedByUserId(int userId, int limit);
    boolean clearRecentlyViewed(int userId);
    boolean removeRecentlyViewed(int userId, int productId);
    int getRecentlyViewedCount(int userId);
    boolean updateTimestamp(int userId, int productId);
    boolean deleteRecentlyViewed(int userId, int productId);
}
