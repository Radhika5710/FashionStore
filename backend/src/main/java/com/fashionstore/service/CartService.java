package com.fashionstore.service;

import com.fashionstore.model.CartItem;
import com.fashionstore.model.Coupon;
import com.fashionstore.model.User;

import java.util.List;

/**
 * CartService - MVC Service Layer Interface
 * 
 * REFACTORED FOR PROPER MVC:
 * - ALL cart calculations in backend
 * - ALL cart validation in backend
 * - ALL coupon processing in backend
 * - Backend is single source of truth
 * - Frontend cannot manipulate cart totals
 * 
 * Cart Calculation Rules:
 * - Subtotal = sum of (item price × quantity)
 * - Discount = coupon discount (validated on backend)
 * - Tax = (subtotal - discount) × TAX_RATE
 * - Shipping = free above threshold, otherwise fixed cost
 * - Total = subtotal - discount + tax + shipping
 * 
 * All calculations are performed on backend.
 * Frontend only displays backend-calculated values.
 * Frontend cannot modify prices, quantities, or totals.
 */
public interface CartService {
    
    /**
     * Get cart items for a user
     * Backend provides authoritative cart data
     */
    List<CartItem> getCartItems(int userId);
    
    /**
     * Add item to cart with business logic validation
     * Backend validates:
     * - Product exists and is active
     * - Size is available
     * - Quantity is valid
     * - Stock is available
     */
    boolean addToCart(int userId, int productId, String size, int quantity);
    
    /**
     * Update cart item quantity with validation
     * Backend validates:
     * - Quantity is positive
     * - Stock is available
     * - Maximum quantity per item
     */
    boolean updateCartItemQuantity(int cartItemId, int userId, int quantity);
    
    /**
     * Remove item from cart
     */
    boolean removeCartItem(int cartItemId, int userId);
    
    /**
     * Calculate cart total with business rules
     * Backend calculates:
     * - Subtotal from product prices
     * - No frontend manipulation possible
     */
    double calculateCartTotal(int userId);
    
    /**
     * Calculate cart total with coupon discount
     * Backend validates coupon and calculates discount
     * Frontend cannot apply invalid coupons
     */
    double calculateCartTotalWithCoupon(int userId, String couponCode);
    
    /**
     * Validate cart for checkout
     * Backend validates:
     * - Cart is not empty
     * - All items are available
     * - Stock is sufficient
     */
    boolean validateCartForCheckout(int userId);
    
    /**
     * Clear cart after successful order
     */
    boolean clearCart(int userId);
    
    /**
     * Get cart item count
     */
    int getCartItemCount(int userId);
    
    /**
     * Check if product is already in cart
     */
    boolean isProductInCart(int userId, int productId, String size);
}
