package com.fashionstore.controller;

import com.fashionstore.model.User;
import com.fashionstore.security.RateLimiter;
import com.fashionstore.service.UserService;
import com.fashionstore.util.AuditLogger;
import com.fashionstore.validation.Validator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/register")
public class RegisterController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UserService userService;

    @Override
    public void init() {
        userService = new UserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Rate limiting check
        if (!RateLimiter.checkRateLimit(request, "/register")) {
            request.setAttribute("error", "Too many registration attempts. Please try again later.");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
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
            .validateMatch(password, confirmPassword, "Passwords");

        if (validator.hasErrors()) {
            request.setAttribute("error", validator.getFirstError());
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
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
            user.setRole("CUSTOMER");

            int userId = userService.registerUser(user);

            if (userId > 0) {
                AuditLogger.log("USER_REGISTERED", "New user registered: " + email, String.valueOf(userId), request);
                
                // Reset rate limit on successful registration
                RateLimiter.resetRateLimit(request, "/register");
                
                response.sendRedirect(request.getContextPath() + "/login?registered=true");
            } else {
                request.setAttribute("error", "Registration failed. Email may already exist.");
                request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            }

        } catch (Exception e) {
            request.setAttribute("error", "An error occurred during registration");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }
}
