package com.fashionstore.controller;

import com.fashionstore.model.User;
import com.fashionstore.security.RateLimiter;
import com.fashionstore.security.CSRFProtection;
import com.fashionstore.service.UserService;
import com.fashionstore.util.AuditLogger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Rate limiting check
        if (!RateLimiter.checkRateLimit(request, "/login")) {
            request.setAttribute("error", "Too many login attempts. Please try again later.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("error", "Email and password are required");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        try {
            User user = userService.loginUser(email, password);

            if (user != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("user", user);
                session.setAttribute("csrfToken", CSRFProtection.generateToken(request));

                AuditLogger.log("LOGIN_SUCCESS", "User logged in: " + email, String.valueOf(user.getUserId()), request);
                
                // Reset rate limit on successful login
                RateLimiter.resetRateLimit(request, "/login");

                response.sendRedirect(request.getContextPath() + "/home");
            } else {
                request.setAttribute("error", "Invalid email or password");
                AuditLogger.log("LOGIN_FAILED", "Failed login attempt: " + email, null, request);
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            }

        } catch (Exception e) {
            request.setAttribute("error", "An error occurred during login");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }
}