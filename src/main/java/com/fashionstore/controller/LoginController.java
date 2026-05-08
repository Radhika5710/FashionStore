package com.fashionstore.controller;

import com.fashionstore.dao.UserDAO;
import com.fashionstore.daoimpl.UserDAOImpl;
import com.fashionstore.model.User;
import com.fashionstore.util.AuditLogger;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/login")
public class LoginController extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() {
        userDAO = new UserDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.getRequestDispatcher("/WEB-INF/views/login.jsp")
           .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        // Input validation
        if (email == null || email.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            req.setAttribute("error", "Email and password are required.");
            AuditLogger.logSecurityEvent("LOGIN_FAILURE", "Missing email or password", req);
            req.getRequestDispatcher("/WEB-INF/views/login.jsp")
               .forward(req, resp);
            return;
        }

        // Validate email format
        if (!isValidEmail(email)) {
            req.setAttribute("error", "Invalid email format.");
            AuditLogger.logSecurityEvent("LOGIN_FAILURE", "Invalid email format: " + email, req);
            req.getRequestDispatcher("/WEB-INF/views/login.jsp")
               .forward(req, resp);
            return;
        }

        User user = userDAO.loginUser(email, password);

        if (user != null) {
            // Session fixation prevention: invalidate existing session
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            // Create new session
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            // Generate new CSRF token
            String csrfToken = generateCSRFToken();
            session.setAttribute("csrf_token", csrfToken);

            AuditLogger.log(String.valueOf(user.getUserId()), "LOGIN_SUCCESS", "User logged in successfully", req);

            // Role-based redirect: admin → dashboard, customer → home
            if (user.isAdmin()) {
                resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
            } else {
                resp.sendRedirect(req.getContextPath() + "/home");
            }

        } else {
            req.setAttribute("error", "Invalid email or password.");
            AuditLogger.logSecurityEvent("LOGIN_FAILURE", "Invalid credentials for email: " + email, req);
            req.getRequestDispatcher("/WEB-INF/views/login.jsp")
               .forward(req, resp);
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) return false;
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private String generateCSRFToken() {
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
}