package com.fashionstore.serviceimpl;

import com.fashionstore.dao.CartDAO;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.model.CartItem;
import com.fashionstore.model.Product;
import com.fashionstore.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for cart operations with business logic
 * Handles cart calculations, validations, and coupon processing
 */
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);
    private static final int MAX_QUANTITY_PER_ITEM = 100;
    private static final int MAX_CART_ITEMS = 50;

    private final CartDAO cartDAO;
    private final ProductDAO productDAO;

    public CartServiceImpl() {
        // Default constructor - DAOs will be set via setter injection
        this.cartDAO = null;
        this.productDAO = null;
    }

    public CartServiceImpl(CartDAO cartDAO, ProductDAO productDAO) {
        this.cartDAO = cartDAO;
        this.productDAO = productDAO;
    }

    public void setCartDAO(CartDAO cartDAO) {
        // For backward compatibility - use constructor injection when possible
        if (this.cartDAO == null) {
            // Hack to set final field via reflection for backward compatibility
            try {
                java.lang.reflect.Field field = CartServiceImpl.class.getDeclaredField("cartDAO");
                field.setAccessible(true);
                field.set(this, cartDAO);
            } catch (Exception e) {
                logger.error("Failed to set cartDAO", e);
            }
        }
    }

    public void setProductDAO(ProductDAO productDAO) {
        // For backward compatibility - use constructor injection when possible
        if (this.productDAO == null) {
            try {
                java.lang.reflect.Field field = CartServiceImpl.class.getDeclaredField("productDAO");
                field.setAccessible(true);
                field.set(this, productDAO);
            } catch (Exception e) {
                logger.error("Failed to set productDAO", e);
            }
        }
    }

    @Override
    public List<CartItem> getCartItems(int userId) {
        if (userId <= 0) {
            logger.warn("Invalid user ID: {}", userId);
            return new ArrayList<>();
        }
        
        try {
            List<CartItem> items = cartDAO.getCartItemsByUserId(userId);
            // Validate and filter invalid items
            List<CartItem> validItems = new ArrayList<>();
            for (CartItem item : items) {
                if (isValidCartItem(item)) {
                    validItems.add(item);
                } else {
                    logger.warn("Removing invalid cart item: {}", item);
                    cartDAO.removeCartItem(item.getCartItemId(), userId);
                }
            }
            
            return validItems;
        } catch (Exception e) {
            logger.error("Error getting cart items for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean addToCart(int userId, int productId, String size, int quantity) {
        if (userId <= 0 || productId <= 0 || quantity <= 0) {
            logger.warn("Invalid parameters for add to cart: userId={}, productId={}, quantity={}", 
                       userId, productId, quantity);
            return false;
        }

        // Validate quantity limits
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            logger.warn("Quantity exceeds maximum: {}", quantity);
            return false;
        }

        // Check if product exists and is available
        Product product = productDAO.getProductById(productId);
        if (product == null || !product.isActive()) {
            logger.warn("Product not available: {}", productId);
            return false;
        }

        // Check current cart size
        List<CartItem> currentItems = getCartItems(userId);
        if (currentItems.size() >= MAX_CART_ITEMS) {
            logger.warn("Cart size limit exceeded for user: {}", userId);
            return false;
        }

        try {
            // Check if item already exists
            CartItem existingItem = findCartItem(currentItems, productId, size);
            boolean result;
            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + quantity;
                if (newQuantity > MAX_QUANTITY_PER_ITEM) {
                    logger.warn("Combined quantity exceeds maximum: {}", newQuantity);
                    return false;
                }
                result = cartDAO.updateQuantity(existingItem.getCartItemId(), userId, newQuantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setUserId(userId);
                newItem.setProductId(productId);
                newItem.setSizeLabel(size != null ? size : "M");
                newItem.setQuantity(quantity);
                newItem.setPrice(product.getPrice());
                newItem.setProductName(product.getProductName());
                newItem.setImageUrl(product.getImageUrl());
                
                result = cartDAO.addToCart(newItem) > 0;
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error adding item to cart: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateCartItemQuantity(int cartItemId, int userId, int quantity) {
        if (cartItemId <= 0 || userId <= 0 || quantity <= 0) {
            logger.warn("Invalid parameters for update quantity: cartItemId={}, userId={}, quantity={}", 
                       cartItemId, userId, quantity);
            return false;
        }

        if (quantity > MAX_QUANTITY_PER_ITEM) {
            logger.warn("Quantity exceeds maximum: {}", quantity);
            return false;
        }

        try {
            boolean result = cartDAO.updateQuantity(cartItemId, userId, quantity);
            return result;
        } catch (Exception e) {
            logger.error("Error updating cart item quantity: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean removeCartItem(int cartItemId, int userId) {
        if (cartItemId <= 0 || userId <= 0) {
            logger.warn("Invalid parameters for remove item: cartItemId={}, userId={}", cartItemId, userId);
            return false;
        }

        try {
            boolean result = cartDAO.removeCartItem(cartItemId, userId);
            return result;
        } catch (Exception e) {
            logger.error("Error removing cart item: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public double calculateCartTotal(int userId) {
        List<CartItem> items = getCartItems(userId);
        double total = 0.0;
        
        for (CartItem item : items) {
            if (isValidCartItem(item)) {
                total += item.getPrice() * item.getQuantity();
            }
        }
        
        return Math.round(total * 100.0) / 100.0; // Round to 2 decimal places
    }

    @Override
    public double calculateCartTotalWithCoupon(int userId, String couponCode) {
        // Coupon functionality not available - CouponDAO doesn't exist
        return calculateCartTotal(userId);
    }

    @Override
    public boolean validateCartForCheckout(int userId) {
        List<CartItem> items = getCartItems(userId);
        
        if (items.isEmpty()) {
            logger.warn("Empty cart for user: {}", userId);
            return false;
        }

        // Validate each item
        for (CartItem item : items) {
            if (!isValidCartItem(item)) {
                logger.warn("Invalid cart item found: {}", item);
                return false;
            }
            
            // Check product availability
            Product product = productDAO.getProductById(item.getProductId());
            if (product == null || !product.isActive()) {
                logger.warn("Product not available: {}", item.getProductId());
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean clearCart(int userId) {
        if (userId <= 0) {
            logger.warn("Invalid user ID for clear cart: {}", userId);
            return false;
        }

        try {
            boolean result = cartDAO.clearCartByUserId(userId);
            return result;
        } catch (Exception e) {
            logger.error("Error clearing cart: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int getCartItemCount(int userId) {
        List<CartItem> items = getCartItems(userId);
        int count = 0;
        for (CartItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    @Override
    public boolean isProductInCart(int userId, int productId, String size) {
        List<CartItem> items = getCartItems(userId);
        return findCartItem(items, productId, size) != null;
    }

    // Private helper methods
    private boolean isValidCartItem(CartItem item) {
        return item != null && 
               item.getCartItemId() > 0 && 
               item.getUserId() > 0 && 
               item.getProductId() > 0 && 
               item.getQuantity() > 0 && 
               item.getPrice() > 0;
    }

    private CartItem findCartItem(List<CartItem> items, int productId, String size) {
        String normalizedSize = size != null ? size.trim() : "M";
        for (CartItem item : items) {
            if (item.getProductId() == productId) {
                String itemSize = item.getSizeLabel() != null ? item.getSizeLabel().trim() : "M";
                if (itemSize.equals(normalizedSize)) {
                    return item;
                }
            }
        }
        return null;
    }
}
