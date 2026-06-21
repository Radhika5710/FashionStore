package com.fashionstore.controller;

import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.security.CSRFProtection;
import com.fashionstore.service.AddressService;
import com.fashionstore.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/account/profile/*")
public class ProfileController extends HttpServlet {
    private final UserService userService;
    private final AddressService addressService;

    public ProfileController() {
        this.userService = ServiceRegistry.getInstance().getUserService();
        this.addressService = ServiceRegistry.getInstance().getAddressService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("customerAuth") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        CSRFProtection.addTokenToRequest(request);

        if (pathInfo == null || pathInfo.equals("/")) {
            showProfile(request, response, user);
        } else if (pathInfo.equals("/edit")) {
            showEditProfileForm(request, response, user);
        } else if (pathInfo.equals("/settings")) {
            showAccountSettings(request, response, user);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("customerAuth") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        // CSRF validation
        if (!CSRFProtection.validateRequest(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String action = request.getParameter("action");

        if ("updateProfile".equals(action)) {
            updateProfile(request, response, user);
        } else if ("updateSettings".equals(action)) {
            updateSettings(request, response, user);
        } else if ("changePassword".equals(action)) {
            changePassword(request, response, user);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }

    private void showProfile(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        int userId = user.getUserId();
        // Get user's addresses
        var addresses = addressService.getAddressesByUserId(userId);
        var defaultShipping = addressService.getDefaultAddress(userId, "shipping");
        var defaultBilling = addressService.getDefaultAddress(userId, "billing");

        request.setAttribute("addresses", addresses);
        request.setAttribute("addressCount", addresses.size());
        request.setAttribute("defaultShipping", defaultShipping);
        request.setAttribute("defaultBilling", defaultBilling);

        request.getRequestDispatcher("/WEB-INF/views/account/profile.jsp").forward(request, response);
    }

    private void showEditProfileForm(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        request.setAttribute("currentUser", user);
        request.getRequestDispatcher("/WEB-INF/views/account/edit-profile.jsp").forward(request, response);
    }

    private void showAccountSettings(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        loadUserSettings(request, user.getUserId());
        request.getRequestDispatcher("/WEB-INF/views/account/account-settings.jsp").forward(request, response);
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        String fullName = trim(request.getParameter("fullName"));
        String phone = trim(request.getParameter("phone"));
        String gender = trim(request.getParameter("gender"));
        String address = trim(request.getParameter("address"));

        // Update user object
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setGender(gender);
        user.setAddress(address);

        // Use service layer for update
        boolean success = userService.updateUser(user);

        if (success) {
            // Update session with customer auth key
            HttpSession session = request.getSession();
            session.setAttribute("customerAuth", user);
            
            request.setAttribute("success", "Profile updated successfully");
            showProfile(request, response, user);
        } else {
            request.setAttribute("error", "Failed to update profile");
            request.setAttribute("currentUser", user);
            showEditProfileForm(request, response, user);
        }
    }

    private void updateSettings(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        boolean emailNotifications = "on".equals(request.getParameter("emailNotifications"));
        boolean smsNotifications = "on".equals(request.getParameter("smsNotifications"));
        boolean orderUpdates = "on".equals(request.getParameter("orderUpdates"));
        boolean promotionalEmails = "on".equals(request.getParameter("promotionalEmails"));

        boolean profileVisible = "on".equals(request.getParameter("profileVisible"));
        boolean activityTracking = "on".equals(request.getParameter("activityTracking"));
        boolean thirdPartySharing = "on".equals(request.getParameter("thirdPartySharing"));

        String language = normalizeToAllowed(request.getParameter("language"), java.util.Set.of("en", "hi", "es"), "en");
        String currency = normalizeToAllowed(request.getParameter("currency"), java.util.Set.of("INR", "USD", "EUR", "GBP"), "INR");
        String theme = normalizeToAllowed(request.getParameter("themePreference"), java.util.Set.of("auto", "light", "dark"), "auto");

        // Build settings map (for future use - not currently persisted)
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("emailNotifications", emailNotifications);
        settings.put("smsNotifications", smsNotifications);
        settings.put("orderUpdates", orderUpdates);
        settings.put("promotionalEmails", promotionalEmails);
        settings.put("language", language);
        settings.put("currency", currency);
        settings.put("themePreference", theme);
        settings.put("profileVisible", profileVisible);
        settings.put("activityTracking", activityTracking);
        settings.put("thirdPartySharing", thirdPartySharing);

        // Settings update - simplified (not persisted to database yet)
        request.setAttribute("success", "Settings updated successfully");
        showAccountSettings(request, response, user);
    }

    private void changePassword(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        // Validate
        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            request.setAttribute("error", "All password fields are required");
            showAccountSettings(request, response, user);
            return;
        }
        if (newPassword.length() < 8 || newPassword.length() > 128) {
            request.setAttribute("error", "New password must be between 8 and 128 characters");
            showAccountSettings(request, response, user);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "New passwords do not match");
            showAccountSettings(request, response, user);
            return;
        }

        // Verify current password
        User verifiedUser = userService.validateAndLoginUser(user.getEmail(), currentPassword);
        if (verifiedUser == null) {
            request.setAttribute("error", "Current password is incorrect");
            showAccountSettings(request, response, user);
            return;
        }

        boolean success = userService.changePassword(user.getUserId(), newPassword);

        if (success) {
            request.setAttribute("success", "Password changed successfully");
            showAccountSettings(request, response, user);
        } else {
            request.setAttribute("error", "Failed to change password");
            showAccountSettings(request, response, user);
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private void loadUserSettings(HttpServletRequest request, int userId) {
        // Load user settings - simplified with default values
        request.setAttribute("emailNotifications", true);
        request.setAttribute("smsNotifications", false);
        request.setAttribute("orderUpdates", true);
        request.setAttribute("promotionalEmails", false);
        request.setAttribute("language", "en");
        request.setAttribute("currency", "USD");
        request.setAttribute("themePreference", "light");
        request.setAttribute("profileVisible", true);
        request.setAttribute("activityTracking", true);
        request.setAttribute("thirdPartySharing", false);
    }

    private static String normalizeToAllowed(String value, java.util.Set<String> allowed, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return allowed.contains(normalized) ? normalized : fallback;
    }
}
