package com.fashionstore.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fashionstore.dao.UserDAO;
import com.fashionstore.daoimpl.UserDAOImpl;
import com.fashionstore.model.User;
import com.fashionstore.util.SecurityUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/users")
public class AdminUsersController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AdminUsersController.class);

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SecurityUtil.requireAdmin(request, response)) {
            return;
        }

        try {
            List<User> users = userDAO.getAllUsers();
            request.setAttribute("users", users);
            request.getRequestDispatcher("/WEB-INF/views/admin-users.jsp")
                   .forward(request, response);
        } catch (Exception e) {
            logger.error("Error in AdminUsersController.doGet: {}", e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading users");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!SecurityUtil.requireAdmin(request, response)) {
            return;
        }

        HttpSession session = request.getSession(true);
        String action = request.getParameter("action");

        if ("disableUser".equals(action)) {
            int userId = Integer.parseInt(request.getParameter("userId"));
            boolean success = userDAO.updateUserRole(userId, "disabled");
            
            if (success) {
                session.setAttribute("message", "User account disabled successfully");
            } else {
                session.setAttribute("error", "Failed to disable user account");
            }
            response.sendRedirect(request.getContextPath() + "/admin/users");
            return;
        }

        if ("enableUser".equals(action)) {
            int userId = Integer.parseInt(request.getParameter("userId"));
            boolean success = userDAO.updateUserRole(userId, "user");
            
            if (success) {
                session.setAttribute("message", "User account enabled successfully");
            } else {
                session.setAttribute("error", "Failed to enable user account");
            }
            response.sendRedirect(request.getContextPath() + "/admin/users");
            return;
        }

        if ("setAdmin".equals(action)) {
            int userId = Integer.parseInt(request.getParameter("userId"));
            boolean success = userDAO.updateUserRole(userId, "admin");
            
            if (success) {
                session.setAttribute("message", "User promoted to admin successfully");
            } else {
                session.setAttribute("error", "Failed to promote user to admin");
            }
            response.sendRedirect(request.getContextPath() + "/admin/users");
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/users");
    }
}
