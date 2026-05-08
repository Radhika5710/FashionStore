package com.fashionstore.daoimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fashionstore.dao.OrderItemDAO;
import com.fashionstore.model.OrderItem;
import com.fashionstore.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrderItemDAOImpl implements OrderItemDAO {

    private static final Logger logger = LoggerFactory.getLogger(OrderItemDAOImpl.class);

    // ✅ Map DB → Object
    private OrderItem mapOrderItem(ResultSet rs) throws Exception {

        OrderItem item = new OrderItem();

        item.setOrderItemId(rs.getInt("order_item_id"));
        item.setOrderId(rs.getInt("order_id"));
        item.setProductId(rs.getInt("product_id"));
        item.setSizeLabel(rs.getString("size_label"));
        item.setQuantity(rs.getInt("quantity"));
        item.setPrice(rs.getDouble("price"));

        return item;
    }

    // 🔥 BATCH LOAD ORDER ITEMS FOR MULTIPLE ORDERS (fixes N+1 query problem)
    public void batchLoadOrderItems(List<com.fashionstore.model.Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        // Collect all order IDs
        List<Integer> orderIds = orders.stream()
                .map(com.fashionstore.model.Order::getOrderId)
                .collect(Collectors.toList());

        // Fetch all order items in a single query using IN clause
        String sql = "SELECT * FROM order_items WHERE order_id IN (" +
                String.join(",", Collections.nCopies(orderIds.size(), "?")) + ") " +
                "ORDER BY order_id, order_item_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            // Set all order ID parameters
            for (int i = 0; i < orderIds.size(); i++) {
                ps.setInt(i + 1, orderIds.get(i));
            }

            ResultSet rs = ps.executeQuery();

            // Group items by order ID using a Map
            Map<Integer, List<OrderItem>> itemsByOrderId = new HashMap<>();
            while (rs.next()) {
                OrderItem item = mapOrderItem(rs);
                itemsByOrderId
                    .computeIfAbsent(rs.getInt("order_id"), k -> new ArrayList<>())
                    .add(item);
            }

            // Assign items to orders
            for (com.fashionstore.model.Order order : orders) {
                order.setItems(itemsByOrderId.getOrDefault(order.getOrderId(), new ArrayList<>()));
            }

        } catch (Exception e) {
            logger.error("OrderItemDAOImpl.batchLoadOrderItems Error: {}", e.getMessage());
            // Fallback to individual queries if batch fails (preserves functionality)
            for (com.fashionstore.model.Order order : orders) {
                order.setItems(getItemsByOrderId(order.getOrderId()));
            }
        }
    }

    // ✅ INSERT
    @Override
    public int addOrderItem(OrderItem item) {

        String sql = "INSERT INTO order_items (order_id, product_id, size_label, quantity, price) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, item.getOrderId());
            ps.setInt(2, item.getProductId());
            ps.setString(3, item.getSizeLabel());
            ps.setInt(4, item.getQuantity());
            ps.setDouble(5, item.getPrice());

            return ps.executeUpdate();

        } catch (Exception e) {
            logger.error("OrderItemDAOImpl.addOrderItem Error: {}", e.getMessage());
        }

        return 0;
    }

    // ✅ GET ITEMS BY ORDER
    @Override
    public List<OrderItem> getItemsByOrderId(int orderId) {

        List<OrderItem> list = new ArrayList<>();

        String sql = "SELECT * FROM order_items WHERE order_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapOrderItem(rs));
            }

        } catch (Exception e) {
            logger.error("OrderItemDAOImpl.getItemsByOrderId Error: {}", e.getMessage());
        }

        return list;
    }

    // ✅ DELETE (optional)
    @Override
    public boolean deleteItemsByOrderId(int orderId) {

        String sql = "DELETE FROM order_items WHERE order_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            logger.error("OrderItemDAOImpl.deleteItemsByOrderId Error: {}", e.getMessage());
        }

        return false;
    }
}