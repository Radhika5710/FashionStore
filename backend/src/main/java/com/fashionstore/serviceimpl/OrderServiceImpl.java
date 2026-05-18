package com.fashionstore.serviceimpl;

import com.fashionstore.dao.OrderDAO;
import com.fashionstore.dao.OrderItemDAO;
import com.fashionstore.model.Order;
import com.fashionstore.model.OrderItem;
import com.fashionstore.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for order operations
 */
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;

    public OrderServiceImpl() {
        // Default constructor - DAOs will be set via setter injection
        this.orderDAO = null;
        this.orderItemDAO = null;
    }

    public OrderServiceImpl(OrderDAO orderDAO, OrderItemDAO orderItemDAO) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
    }

    public void setOrderDAO(OrderDAO orderDAO) {
        if (this.orderDAO == null) {
            try {
                java.lang.reflect.Field field = OrderServiceImpl.class.getDeclaredField("orderDAO");
                field.setAccessible(true);
                field.set(this, orderDAO);
            } catch (Exception e) {
                logger.error("Failed to set orderDAO", e);
            }
        }
    }

    public void setOrderItemDAO(OrderItemDAO orderItemDAO) {
        if (this.orderItemDAO == null) {
            try {
                java.lang.reflect.Field field = OrderServiceImpl.class.getDeclaredField("orderItemDAO");
                field.setAccessible(true);
                field.set(this, orderItemDAO);
            } catch (Exception e) {
                logger.error("Failed to set orderItemDAO", e);
            }
        }
    }

    @Override
    public Order createOrder(int userId, Map<String, Object> orderData) {
        try {
            Order order = new Order();
            order.setUserId(userId);
            
            if (orderData.containsKey("totalAmount")) {
                order.setTotalAmount(((Number) orderData.get("totalAmount")).doubleValue());
            }
            if (orderData.containsKey("fullName")) {
                order.setFullName((String) orderData.get("fullName"));
            }
            if (orderData.containsKey("address")) {
                order.setAddress((String) orderData.get("address"));
            }
            if (orderData.containsKey("city")) {
                order.setCity((String) orderData.get("city"));
            }
            if (orderData.containsKey("state")) {
                order.setState((String) orderData.get("state"));
            }
            if (orderData.containsKey("zip")) {
                order.setZip((String) orderData.get("zip"));
            }
            if (orderData.containsKey("phone")) {
                order.setPhone((String) orderData.get("phone"));
            }
            if (orderData.containsKey("paymentMethod")) {
                order.setPaymentMethod((String) orderData.get("paymentMethod"));
            }
            if (orderData.containsKey("status")) {
                order.setStatus((String) orderData.get("status"));
            } else {
                order.setStatus("Pending");
            }
            
            int orderId = orderDAO.createOrder(order);
            if (orderId > 0) {
                order.setOrderId(orderId);
                
                // Add order items if provided
                if (orderData.containsKey("items")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> itemsData = (List<Map<String, Object>>) orderData.get("items");
                    for (Map<String, Object> itemData : itemsData) {
                        OrderItem item = new OrderItem();
                        item.setOrderId(orderId);
                        item.setProductId(((Number) itemData.get("productId")).intValue());
                        item.setSizeLabel((String) itemData.get("sizeLabel"));
                        item.setQuantity(((Number) itemData.get("quantity")).intValue());
                        item.setPrice(((Number) itemData.get("price")).doubleValue());
                        orderItemDAO.addOrderItem(item);
                    }
                }
                
                logger.info("Order created successfully: orderId={}, userId={}", orderId, userId);
                return order;
            }
        } catch (Exception e) {
            logger.error("Error creating order for user {}: {}", userId, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Order getOrderById(int orderId, int requestingUserId) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order != null && order.getUserId() == requestingUserId) {
                return order;
            }
        } catch (Exception e) {
            logger.error("Error getting order by ID {}: {}", orderId, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Order> getOrdersForUser(int userId) {
        try {
            return orderDAO.getOrdersByUserId(userId);
        } catch (Exception e) {
            logger.error("Error getting orders for user {}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Order> getAllOrders() {
        try {
            return orderDAO.getAllOrders();
        } catch (Exception e) {
            logger.error("Error getting all orders: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Order> getRecentOrders(int limit) {
        try {
            return orderDAO.getRecentOrders(limit);
        } catch (Exception e) {
            logger.error("Error getting recent orders: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean updateOrderStatus(int orderId, String newStatus, int requestingUserId) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order != null) {
                return orderDAO.updateOrderStatus(orderId, newStatus);
            }
        } catch (Exception e) {
            logger.error("Error updating order status for order {}: {}", orderId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean processOrderPayment(int orderId, String paymentMethod, double amount) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order != null) {
                order.setPaymentMethod(paymentMethod);
                order.setStatus("Processing");
                return orderDAO.updateOrderStatus(orderId, "Processing");
            }
        } catch (Exception e) {
            logger.error("Error processing payment for order {}: {}", orderId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean cancelOrder(int orderId, int requestingUserId) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order != null && order.getUserId() == requestingUserId) {
                if ("Pending".equals(order.getStatus()) || "Processing".equals(order.getStatus())) {
                    return orderDAO.updateOrderStatus(orderId, "Cancelled");
                }
            }
        } catch (Exception e) {
            logger.error("Error cancelling order {}: {}", orderId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean refundOrder(int orderId, int requestingUserId) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order != null) {
                if ("Delivered".equals(order.getStatus()) || "Shipped".equals(order.getStatus())) {
                    return orderDAO.updateOrderStatus(orderId, "Refunded");
                }
            }
        } catch (Exception e) {
            logger.error("Error refunding order {}: {}", orderId, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<OrderItem> getOrderItems(int orderId) {
        try {
            return orderItemDAO.getItemsByOrderId(orderId);
        } catch (Exception e) {
            logger.error("Error getting order items for order {}: {}", orderId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public double calculateOrderTotal(int orderId) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order != null) {
                return order.getTotalAmount();
            }
        } catch (Exception e) {
            logger.error("Error calculating order total for order {}: {}", orderId, e.getMessage(), e);
        }
        return 0.0;
    }

    @Override
    public boolean validateOrderForProcessing(int orderId) {
        try {
            Order order = orderDAO.getOrderById(orderId);
            if (order != null) {
                List<OrderItem> items = orderItemDAO.getItemsByOrderId(orderId);
                return order != null && items != null && !items.isEmpty();
            }
        } catch (Exception e) {
            logger.error("Error validating order for processing: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Map<String, Object> getOrderStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOrders", orderDAO.getTotalOrderCount());
            stats.put("totalRevenue", orderDAO.getTotalRevenue());
            stats.put("pendingOrders", countOrdersByStatus("Pending"));
            stats.put("processingOrders", countOrdersByStatus("Processing"));
            stats.put("shippedOrders", countOrdersByStatus("Shipped"));
            stats.put("deliveredOrders", countOrdersByStatus("Delivered"));
            stats.put("cancelledOrders", countOrdersByStatus("Cancelled"));
            stats.put("refundedOrders", countOrdersByStatus("Refunded"));
            return stats;
        } catch (Exception e) {
            logger.error("Error getting order statistics: {}", e.getMessage(), e);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOrders", 0);
            stats.put("totalRevenue", 0.0);
            stats.put("pendingOrders", 0);
            stats.put("processingOrders", 0);
            stats.put("shippedOrders", 0);
            stats.put("deliveredOrders", 0);
            stats.put("cancelledOrders", 0);
            stats.put("refundedOrders", 0);
            return stats;
        }
    }

    private int countOrdersByStatus(String status) {
        try {
            List<Order> orders = orderDAO.getAllOrders();
            return (int) orders.stream().filter(o -> status.equals(o.getStatus())).count();
        } catch (Exception e) {
            logger.error("Error counting orders by status {}: {}", status, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public void batchLoadOrderItems(List<Order> orders) {
        try {
            orderItemDAO.batchLoadOrderItems(orders);
        } catch (Exception e) {
            logger.error("Error batch loading order items: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<Order> getOrdersByStatus(String status) {
        try {
            List<Order> allOrders = orderDAO.getAllOrders();
            List<Order> filtered = new ArrayList<>();
            for (Order order : allOrders) {
                if (status.equals(order.getStatus())) {
                    filtered.add(order);
                }
            }
            return filtered;
        } catch (Exception e) {
            logger.error("Error getting orders by status {}: {}", status, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public double getTotalRevenue() {
        try {
            return orderDAO.getTotalRevenue();
        } catch (Exception e) {
            logger.error("Error getting total revenue: {}", e.getMessage(), e);
            return 0.0;
        }
    }

    @Override
    public int getTotalOrderCount() {
        try {
            return orderDAO.getTotalOrderCount();
        } catch (Exception e) {
            logger.error("Error getting total order count: {}", e.getMessage(), e);
            return 0;
        }
    }
}
