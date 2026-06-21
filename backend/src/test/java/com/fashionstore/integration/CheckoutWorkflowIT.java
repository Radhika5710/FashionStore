package com.fashionstore.integration;

import com.fashionstore.dao.OrderDAO;
import com.fashionstore.dao.OrderItemDAO;
import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.ProductSizeDAO;
import com.fashionstore.daoimpl.OrderDAOImpl;
import com.fashionstore.daoimpl.OrderItemDAOImpl;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.daoimpl.ProductSizeDAOImpl;
import com.fashionstore.model.Order;
import com.fashionstore.model.OrderItem;
import com.fashionstore.model.ProductSize;
import com.fashionstore.service.InventoryService;
import com.fashionstore.service.OrderService;
import com.fashionstore.serviceimpl.InventoryServiceImpl;
import com.fashionstore.serviceimpl.OrderServiceImpl;
import com.fashionstore.util.DBConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutWorkflowIT {

    private static OrderService orderService;
    private static InventoryService inventoryService;
    private static ProductSizeDAO productSizeDAO;

    @BeforeAll
    public static void setUp() throws Exception {
        // Force Testcontainers boot
        DatabaseTestContainer.start();

        orderService = new OrderServiceImpl();
        inventoryService = new InventoryServiceImpl();
        productSizeDAO = new ProductSizeDAOImpl();

        // Seed some base user and product data for testing
        try (Connection conn = DBConnection.getConnection()) {
            // Check if test user exists, otherwise insert
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO users (user_id, full_name, email, password, role) VALUES (999, 'Test Buyer', 'buyer@test.com', 'pass', 'customer')")) {
                ps.executeUpdate();
            }
            // Check if test product exists, otherwise insert
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO products (product_id, product_name, price, stock_quantity, category_id) VALUES (999, 'Bespoke Suit', 999.00, 100, 1)")) {
                ps.executeUpdate();
            }
            // Add sizes for product
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO product_sizes (product_id, size_label, stock_quantity) VALUES (999, 'M', 5)")) {
                ps.executeUpdate();
            }
        }
    }

    private ProductSize getProductSize(int productId, String label) {
        List<ProductSize> list = productSizeDAO.getSizesByProductId(productId);
        if (list != null) {
            for (ProductSize ps : list) {
                if (label.equalsIgnoreCase(ps.getSizeLabel())) {
                    return ps;
                }
            }
        }
        return null;
    }

    @Test
    public void testSuccessfulCheckoutDecrementsStock() {
        // Check current stock
        ProductSize ps = getProductSize(999, "M");
        assertNotNull(ps);
        int initialStock = ps.getStockQuantity();

        // Prepare order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("fullName", "Test Buyer");
        orderData.put("address", "123 High Street");
        orderData.put("city", "London");
        orderData.put("state", "London");
        orderData.put("zip", "E1 6AN");
        orderData.put("phone", "07700900077");
        orderData.put("paymentMethod", "STRIPE");
        orderData.put("totalAmount", 999.00);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productId", 999);
        item.put("quantity", 2);
        item.put("price", 999.00);
        item.put("sizeLabel", "M");
        items.add(item);
        orderData.put("items", items);

        // Deduct stock first to replicate controller workflow
        List<ProductSize> sizesToDeduct = new ArrayList<>();
        ProductSize deduction = new ProductSize();
        deduction.setProductId(999);
        deduction.setSizeLabel("M");
        deduction.setStockQuantity(2);
        sizesToDeduct.add(deduction);

        boolean stockDeducted = inventoryService.processInventoryAfterOrder(sizesToDeduct);
        assertTrue(stockDeducted, "Stock should be successfully deducted");

        Order order = orderService.createOrder(999, orderData);
        assertNotNull(order, "Order should be created successfully");
        assertEquals("Pending", order.getStatus());

        // Validate stock decremented in DB
        ProductSize updatedPs = getProductSize(999, "M");
        assertEquals(initialStock - 2, updatedPs.getStockQuantity(), "Stock quantity should be reduced by 2");
    }

    @Test
    public void testHighConcurrencyPurchasesDoesNotNegativeStock() throws Exception {
        // Seed a limited stock item
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO products (product_id, product_name, price, stock_quantity, category_id) VALUES (888, 'Limited Sneakers', 150.00, 10, 1)")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO product_sizes (product_id, size_label, stock_quantity) VALUES (888, '10', 3)")) {
                ps.executeUpdate();
            }
        }

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successfulDeductions = new AtomicInteger(0);
        AtomicInteger failedDeductions = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // Wait for signal to make thread start highly concurrent action
                    
                    List<ProductSize> request = new ArrayList<>();
                    ProductSize size = new ProductSize();
                    size.setProductId(888);
                    size.setSizeLabel("10");
                    size.setStockQuantity(1);
                    request.add(size);

                    // Call the atomic reduction service
                    if (inventoryService.processInventoryAfterOrder(request)) {
                        successfulDeductions.incrementAndGet();
                    } else {
                        failedDeductions.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latch.countDown(); // Trigger high concurrency run!
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(50);
        }

        // Validate stock: only 3 threads should have succeeded since stock was 3
        assertEquals(3, successfulDeductions.get(), "Exactly 3 buyers should succeed");
        assertEquals(7, failedDeductions.get(), "Remaining 7 buyers should be gracefully rejected");

        ProductSize ps = getProductSize(888, "10");
        assertEquals(0, ps.getStockQuantity(), "Inventory stock should be exactly 0, never negative!");
    }

    // NOTE: testAbandonedCheckoutSweeperRestoresStock removed - RuntimeMonitor was deleted as part of dead code cleanup
    // The sweeper functionality was part of the fake monitoring feature that has been removed
}
