package com.fashionstore.controller;

import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.security.RateLimiter;
import com.fashionstore.service.UserService;
import com.fashionstore.util.AuditLogger;
import com.fashionstore.validation.Validator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/register")
public class RegisterController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    private UserService userService;

    @Override
    public void init() {
        userService = ServiceRegistry.getInstance().getUserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Generate CSRF token for registration form
        com.fashionstore.security.CSRFProtection.addTokenToRequest(request);
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Rate limiting check
        if (!RateLimiter.checkRateLimit(request, "/register")) {
            sendError(request, response, "Too many registration attempts. Please try again later.", "/WEB-INF/views/register.jsp", 429);
            return;
        }

        String fullName = request.getParameter("fullName");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String gender = request.getParameter("gender");
        String address = request.getParameter("address");

        // Centralized validation
        Validator validator = Validator.create()
            .validateName(fullName, "Full name")
            .validateEmail(email, "Email")
            .validatePhone(phone, "Phone")
            .validatePassword(password, "Password")
            .validateMatch(password, confirmPassword, "Passwords")
            .validateOptionalAddress(address, "Address", 500);

        if (validator.hasErrors()) {
            sendError(request, response, validator.getFirstError(), "/WEB-INF/views/register.jsp", HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // Check if email already exists before attempting registration
        if (userService.isEmailExists(email)) {
            sendError(request, response, "Email already registered. Please login or use a different email.", "/WEB-INF/views/register.jsp", HttpServletResponse.SC_CONFLICT);
            return;
        }

        try {
            User user = new User();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(password);
            user.setGender(gender);
            user.setAddress(address);
            user.setRole("customer");

            int userId = userService.registerUser(user);

            if (userId > 0) {
                AuditLogger.log("USER_REGISTERED", "New user registered: " + email, String.valueOf(userId), request);
                
                // Reset rate limit on successful registration
                RateLimiter.resetRateLimit(request, "/register");
                
                String redirectUrl = request.getContextPath() + "/login?registered=true";
                boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) || 
                                 (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
                if (isAjax) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("success", true);
                    map.put("message", "Registration successful! Please login.");
                    map.put("redirect", redirectUrl);
                    response.getWriter().write(com.fashionstore.util.JsonUtil.toJson(map));
                } else {
                    response.sendRedirect(redirectUrl);
                }
            } else {
                sendError(request, response, "Registration failed due to a database error. Please try again.", "/WEB-INF/views/register.jsp", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            logger.error("Unexpected error during user registration: {}", e.getMessage(), e);
            // Check if it's a duplicate key error (email already exists)
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                sendError(request, response, "Email already registered. Please login or use a different email.", "/WEB-INF/views/register.jsp", HttpServletResponse.SC_CONFLICT);
            } else {
                sendError(request, response, "An error occurred during registration. Please try again.", "/WEB-INF/views/register.jsp", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private void sendError(HttpServletRequest request, HttpServletResponse response, String message, String viewPath, int status)
            throws ServletException, IOException {
        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) || 
                         (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));
        if (isAjax) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(status);
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("success", false);
            map.put("message", message);
            response.getWriter().write(com.fashionstore.util.JsonUtil.toJson(map));
        } else {
            request.setAttribute("error", message);
            request.getRequestDispatcher(viewPath).forward(request, response);
        }
    }
}
