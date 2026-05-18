package com.fashionstore.dao;

import com.fashionstore.model.Order;
import java.sql.Connection;
import java.util.List;

public interface OrderDAO {

    int createOrder(Order order);
    
    int createOrder(Connection conn, Order order) throws Exception;

    Order getOrderById(int orderId);

    List<Order> getOrdersByUserId(int userId);

    List<Order> getAllOrders();

    boolean updateOrderStatus(int orderId, String status);

    // Analytics methods
    double getTotalRevenue();
    int getTotalOrderCount();
    List<Order> getRecentOrders(int limit);
    List<Order> getOrdersInLastDays(int days);
}