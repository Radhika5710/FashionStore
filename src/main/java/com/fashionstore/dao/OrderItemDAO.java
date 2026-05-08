package com.fashionstore.dao;

import com.fashionstore.model.Order;
import com.fashionstore.model.OrderItem;
import java.util.List;

public interface OrderItemDAO {

    int addOrderItem(OrderItem item);

    List<OrderItem> getItemsByOrderId(int orderId);

    boolean deleteItemsByOrderId(int orderId);
    
    void batchLoadOrderItems(List<Order> orders);
}