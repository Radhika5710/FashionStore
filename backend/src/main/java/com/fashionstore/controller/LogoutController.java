package com.fashionstore.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Customer MVC Session-Based Logout Controller
 * 
 * CRITICAL: This controller handles CUSTOMER LOGOUT ONLY using SESSIONS.
 * DO NOT add JWT logic here. JWT tokens are for admin APIs only (/api/admin/*).
 * 
 * LOGOUT FLOW:
 * ============
 * 1. Customer requests /logout
 * 2. Invalidate HttpSession (JSESSIONID cookie cleared)
 * 3. Redirect to /login page
 * 4. Session attributes (customer, userId, etc.) are cleared
 * 
 * IMPORTANT SEPARATION:
 * ====================
 * ✓ This controller uses ONLY HttpSession
 * ✓ NO JWT token clearing here
 * ✓ NO Authorization header handling
 * ✓ NO token cookies set here
 * ✓ NO refresh token logic here
 * 
 * Admin JWT logout is handled by:
 * - AdminAuthApiController (/api/admin/logout)
 * - Frontend React clears JWT tokens
 */
@WebServlet("/logout")
public class LogoutController extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Clear session for customer logout
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Redirect to login page
        response.sendRedirect(request.getContextPath() + "/login");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Handle POST logout for API calls
        doGet(request, response);
    }
}