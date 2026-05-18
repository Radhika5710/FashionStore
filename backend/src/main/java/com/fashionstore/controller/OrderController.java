package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.model.Order;
import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.OrderService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet("/orders")
public class OrderController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private OrderService orderService;

    @Override
    public void init() {
        ServiceRegistry registry = ServiceRegistry.getInstance();
        orderService = registry.getOrderService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("customerAuth") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        int userId = user.getUserId();

        List<Order> orders;
        try {
            if (userId <= 0) {
                logger.warn("Invalid user ID in OrderController: {}", userId);
                orders = Collections.emptyList();
                request.setAttribute("error", "Invalid user session. Please login again.");
            } else {
                orders = orderService.getOrdersForUser(userId);
                if (orders != null) {
                    // Batch load order items to avoid N+1 queries
                    orderService.batchLoadOrderItems(orders);
                } else {
                    orders = Collections.emptyList();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load orders for user #{}: {}", userId, e.getMessage(), e);
            orders = Collections.emptyList();
            request.setAttribute("error", "We could not load your orders right now. Please try again later.");
        }

        request.setAttribute("orders", orders);

        request.getRequestDispatcher("/WEB-INF/views/orders.jsp")
                .forward(request, response);
    }
}