package com.fashionstore.dao;

import com.fashionstore.model.CartItem;
import java.util.List;

public interface CartDAO {

    int addToCart(CartItem item);

    List<CartItem> getCartItemsByUserId(int userId);

    boolean removeCartItem(int cartItemId, int userId);

    boolean updateQuantity(int cartItemId, int userId, int quantity);

    boolean clearCartByUserId(int userId);

    boolean clearCartByUserIdInTransaction(java.sql.Connection conn, int userId) throws Exception;
}