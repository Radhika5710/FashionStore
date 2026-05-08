package com.fashionstore.controller;

import com.fashionstore.dao.OrderDAO;
import com.fashionstore.daoimpl.OrderDAOImpl;
import com.fashionstore.daoimpl.OrderItemDAOImpl;
import com.fashionstore.model.Order;
import com.fashionstore.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

@WebServlet("/orders")
public class OrderController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private OrderDAO orderDAO;
    private OrderItemDAOImpl orderItemDAO;

    @Override
    public void init() {
        orderDAO = new OrderDAOImpl();
        orderItemDAO = new OrderItemDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        int userId = user.getUserId();

        // ✅ Get all orders
        List<Order> orders = orderDAO.getOrdersByUserId(userId);

        // 🔥 Batch load order items to avoid N+1 queries
        orderItemDAO.batchLoadOrderItems(orders);

        request.setAttribute("orders", orders);

        request.getRequestDispatcher("/WEB-INF/views/orders.jsp")
                .forward(request, response);
    }
}