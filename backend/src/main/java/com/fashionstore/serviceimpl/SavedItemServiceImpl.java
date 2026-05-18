package com.fashionstore.serviceimpl;

import com.fashionstore.dao.SavedItemDAO;
import com.fashionstore.model.SavedItem;
import com.fashionstore.service.SavedItemService;

import java.util.List;

/**
 * SavedItemServiceImpl - MVC Service Layer Implementation
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL saved item business logic in service layer
 * - ALL validation in service layer
 * - Controllers only handle request/response
 * - DAO layer only handles database access
 */
public class SavedItemServiceImpl implements SavedItemService {
    
    private final SavedItemDAO savedItemDAO;

    public SavedItemServiceImpl() {
        // Default constructor - DAO will be set via setter injection
        this.savedItemDAO = null;
    }

    public SavedItemServiceImpl(SavedItemDAO savedItemDAO) {
        this.savedItemDAO = savedItemDAO;
    }

    public void setSavedItemDAO(SavedItemDAO savedItemDAO) {
        if (this.savedItemDAO == null) {
            try {
                java.lang.reflect.Field field = SavedItemServiceImpl.class.getDeclaredField("savedItemDAO");
                field.setAccessible(true);
                field.set(this, savedItemDAO);
            } catch (Exception e) {
                System.err.println("Failed to set savedItemDAO: " + e.getMessage());
            }
        }
    }
    
    @Override
    public List<SavedItem> getSavedItems(int userId) {
        return savedItemDAO.getSavedItemsByUserId(userId);
    }
    
    @Override
    public boolean saveItem(SavedItem savedItem) {
        // Business logic: Validate saved item before saving
        if (savedItem == null) {
            return false;
        }
        if (savedItem.getUserId() <= 0) {
            return false;
        }
        if (savedItem.getProductId() <= 0) {
            return false;
        }
        if (savedItem.getSizeLabel() == null || savedItem.getSizeLabel().trim().isEmpty()) {
            return false;
        }
        
        return savedItemDAO.saveItem(savedItem);
    }
    
    @Override
    public boolean removeSavedItem(int savedItemId, int userId) {
        return savedItemDAO.removeItem(savedItemId);
    }
    
    @Override
    public int getSavedItemCount(int userId) {
        List<SavedItem> items = savedItemDAO.getSavedItemsByUserId(userId);
        return items != null ? items.size() : 0;
    }
    
    @Override
    public boolean isProductSaved(int userId, int productId, String size) {
        SavedItem item = savedItemDAO.getSavedItem(userId, productId, size);
        return item != null;
    }
}
