package com.fashionstore.dao;

import com.fashionstore.model.WishlistItem;
import java.util.List;

public interface WishlistDAO {
    boolean addWishlistItem(int userId, int productId);
    boolean removeWishlistItem(int userId, int productId);
    List<WishlistItem> getWishlistByUserId(int userId);
    boolean isProductInWishlist(int userId, int productId);
}
