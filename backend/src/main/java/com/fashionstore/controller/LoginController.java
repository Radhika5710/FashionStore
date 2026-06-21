package com.fashionstore.controller;

import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/login")
public class LoginController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private UserService userService;
    private ObjectMapper objectMapper;

    @Override
    public void init() {
        userService = ServiceRegistry.getInstance().getUserService();
        objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = null;
        String password = null;

        // Try to get parameters from JSON body (AJAX request)
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = request.getReader().readLine()) != null) {
                    buffer.append(line);
                }
                String payload = buffer.toString();
                
                if (!payload.isEmpty()) {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> data = mapper.readValue(payload, Map.class);
                    email = data.get("email");
                    password = data.get("password");
                }
            } catch (Exception e) {
                // If JSON parsing fails, fall back to form parameters
            }
        }

        // Fall back to form parameters if JSON parsing failed or not JSON request
        if (email == null) {
            email = request.getParameter("email");
        }
        if (password == null) {
            password = request.getParameter("password");
        }

        try {
            User user = userService.validateAndLoginUser(email, password);

            if (user != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("customerAuth", user);

                // Load cart items into session so navbar badge shows correctly immediately
                try {
                    com.fashionstore.service.CartService cartService = ServiceRegistry.getInstance().getCartService();
                    java.util.List<com.fashionstore.model.CartItem> cartItems = cartService.getCartItems(user.getUserId());
                    session.setAttribute("cartItems", cartItems);
                } catch (Exception ex) {
                    // Ignore cart load error during login
                }

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Login successful");
                responseData.put("redirect", request.getContextPath() + "/home");

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(responseData));
            } else {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password");
            }

        } catch (IllegalArgumentException e) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            sendJsonError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred during login");
        }
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("success", false);
        errorData.put("message", message);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.getWriter().write(objectMapper.writeValueAsString(errorData));
    }
}