package com.fashionstore.controller.api;

import com.fashionstore.controller.ApiResponse;
import com.fashionstore.model.*;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.OrderService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

@WebServlet("/api/admin/orders/*")
public class AdminOrderApiController extends AdminApiBaseController {

    private static final long serialVersionUID = 1L;

    private OrderService orderService;

    @Override
    public void init() {
        super.init();
        orderService = ServiceRegistry.getInstance().getOrderService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/admin/orders - List all orders
                int limit = parseInt(request.getParameter("limit"), 50);
                List<Order> orders = orderService.getRecentOrders(limit);
                writeApiResponse(response, 200, ApiResponse.success("Orders retrieved successfully", Map.of(
                    "orders", orders.stream().map(this::publicOrder).toList(),
                    "count", orders.size()
                )));
                return;
            }
            
            // GET /api/admin/orders/recent - Get recent orders
            if (pathInfo.equals("/recent")) {
                int limit = parseInt(request.getParameter("limit"), 10);
                List<Order> orders = orderService.getRecentOrders(limit);
                writeApiResponse(response, 200, ApiResponse.success("Recent orders retrieved successfully", orders.stream().map(this::publicOrder).toList()));
                return;
            }
            
            // GET /api/admin/orders/{id} - Get single order
            String[] segments = pathInfo.split("/");
            if (segments.length == 2) {
                try {
                    int orderId = Integer.parseInt(segments[1]);
                    Order order = orderService.getOrderById(orderId, 0);
                    if (order == null) {
                        writeApiResponse(response, 404, ApiResponse.error("Order not found"));
                        return;
                    }
                    writeApiResponse(response, 200, ApiResponse.success("Order retrieved successfully", publicOrder(order)));
                } catch (NumberFormatException e) {
                    writeApiResponse(response, 400, ApiResponse.error("Invalid order ID"));
                }
                return;
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!isTrustedStateChangingRequest(request)) {
            writeApiResponse(response, 403, ApiResponse.error("Blocked by origin policy"));
            return;
        }
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && !pathInfo.equals("/")) {
                String[] segments = pathInfo.split("/");
                if (segments.length >= 3) {
                    try {
                        int orderId = Integer.parseInt(segments[1]);
                        String action = segments[2];
                        
                        if (orderService.getOrderById(orderId, 0) == null) {
                            writeApiResponse(response, 404, ApiResponse.error("Order not found"));
                            return;
                        }
                        
                        boolean success = false;
                        switch (action) {
                            case "status" -> {
                                Map<String, Object> body = readJsonBody(request);
                                String status = strParam(body, "status");
                                if (status.isBlank()) {
                                    writeApiResponse(response, 400, ApiResponse.error("Status required"));
                                    return;
                                }
                                success = orderService.updateOrderStatus(orderId, status, 0);
                                if (success) {
                                    writeApiResponse(response, 200, ApiResponse.success("Order status updated to " + status, null));
                                } else {
                                    writeApiResponse(response, 400, ApiResponse.error("Failed to update status"));
                                }
                            }
                            case "confirm" -> {
                                success = orderService.updateOrderStatus(orderId, "Confirmed", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order confirmed successfully", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Confirmed"));
                            }
                            case "pack" -> {
                                success = orderService.updateOrderStatus(orderId, "Packing", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order packing started", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Packing"));
                            }
                            case "approve" -> {
                                success = orderService.updateOrderStatus(orderId, "Processing", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order approved successfully", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Processing"));
                            }
                            case "cancel" -> {
                                success = orderService.updateOrderStatus(orderId, "Cancelled", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order cancelled successfully", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Cancelled"));
                            }
                            case "refund" -> {
                                success = orderService.updateOrderStatus(orderId, "Refunded", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order refunded successfully", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Refunded"));
                            }
                            case "ship" -> {
                                success = orderService.updateOrderStatus(orderId, "Shipped", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order shipped successfully", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Shipped"));
                            }
                            case "outfordelivery" -> {
                                success = orderService.updateOrderStatus(orderId, "Out for Delivery", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order is out for delivery", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Out for Delivery"));
                            }
                            case "deliver" -> {
                                success = orderService.updateOrderStatus(orderId, "Delivered", 0);
                                if (success) writeApiResponse(response, 200, ApiResponse.success("Order marked as delivered", null));
                                else writeApiResponse(response, 400, ApiResponse.error("Failed to transition to Delivered"));
                            }
                            default -> {
                                writeApiResponse(response, 404, ApiResponse.error("Action not found"));
                            }
                        }
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid order ID"));
                    }
                    return;
                }
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        applyCors(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    // Helper methods
    private Map<String, Object> publicOrder(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getOrderId());
        m.put("userId", o.getUserId());
        m.put("customerName", o.getFullName());
        m.put("total", o.getTotalAmount());
        m.put("status", o.getStatus());
        m.put("paymentStatus", "pending");
        m.put("paymentMethod", o.getPaymentMethod());
        m.put("createdAt", o.getOrderDate() != null ? o.getOrderDate().getTime() : null);
        m.put("address", o.getAddress());
        m.put("city", o.getCity());
        m.put("state", o.getState());
        m.put("zip", o.getZip());
        m.put("phone", o.getPhone());
        List<Map<String, Object>> items = new ArrayList<>();
        if (o.getItems() != null) {
            for (OrderItem item : o.getItems()) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("productId", item.getProductId());
                im.put("quantity", item.getQuantity());
                im.put("price", item.getPrice());
                im.put("sizeLabel", item.getSizeLabel());
                items.add(im);
            }
        }
        m.put("items", items);
        return m;
    }
}
