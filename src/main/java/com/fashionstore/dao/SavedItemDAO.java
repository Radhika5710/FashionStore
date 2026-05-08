package com.fashionstore.dao;

import com.fashionstore.model.SavedItem;
import java.util.List;

public interface SavedItemDAO {
    boolean saveItem(SavedItem savedItem);
    boolean removeItem(int savedItemId);
    List<SavedItem> getSavedItemsByUserId(int userId);
    SavedItem getSavedItem(int userId, int productId, String sizeLabel);
    boolean moveToCart(int savedItemId);
}
