package com.fashionstore.controller;

import com.fashionstore.model.Address;
import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.security.CSRFProtection;
import com.fashionstore.service.AddressService;
import com.fashionstore.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/account/addresses/*")
public class AddressController extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);
    private final AddressService addressService;

    public AddressController() {
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
            // Show address list
            listAddresses(request, response, user);
        } else if (pathInfo.equals("/add")) {
            // Show add address form
            showAddAddressForm(request, response, user);
        } else if (pathInfo.startsWith("/edit/")) {
            // Show edit address form
            showEditAddressForm(request, response, user, pathInfo);
        } else if (pathInfo.equals("/api/list")) {
            // AJAX: Get address list
            getAddressListAjax(request, response, user);
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
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(JsonUtil.toJson(Map.of(
                    "success", false,
                    "message", "Please login to continue",
                    "redirect", request.getContextPath() + "/login"
                )));
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        // CSRF validation
        if (!CSRFProtection.validateRequest(request)) {
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write(JsonUtil.toJson(Map.of(
                    "success", false,
                    "message", "Invalid CSRF token"
                )));
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            }
            return;
        }

        String action = request.getParameter("action");

        if ("add".equals(action)) {
            addAddress(request, response, user);
        } else if ("edit".equals(action)) {
            editAddress(request, response, user);
        } else if ("delete".equals(action)) {
            deleteAddress(request, response, user);
        } else if ("setDefault".equals(action)) {
            setDefaultAddress(request, response, user);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }

    private void listAddresses(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        List<Address> addresses = addressService.getAddressesByUserId(user.getUserId());
        request.setAttribute("addresses", addresses);
        request.setAttribute("addressCount", addresses.size());
        request.getRequestDispatcher("/WEB-INF/views/account/address-management.jsp").forward(request, response);
    }

    private void showAddAddressForm(HttpServletRequest request, HttpServletResponse response, User user) 
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/account/address-form.jsp").forward(request, response);
    }

    private void showEditAddressForm(HttpServletRequest request, HttpServletResponse response, User user, String pathInfo) 
            throws ServletException, IOException {
        try {
            if (pathInfo == null || pathInfo.length() < 7) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID format");
                return;
            }
            int addressId = Integer.parseInt(pathInfo.substring(6));
            Address address = addressService.getAddressById(addressId, user.getUserId());
            
            if (address == null) {
                request.setAttribute("error", "Address not found");
                listAddresses(request, response, user);
                return;
            }
            
            request.setAttribute("address", address);
            request.getRequestDispatcher("/WEB-INF/views/account/address-form.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            logger.error("Invalid address ID: {}", pathInfo);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID");
        }
    }

    private void getAddressListAjax(HttpServletRequest request, HttpServletResponse response, User user) 
            throws IOException {
        response.setContentType("application/json");
        List<Address> addresses = addressService.getAddressesByUserId(user.getUserId());
        
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("addresses", addresses);
        data.put("count", addresses.size());
        
        response.getWriter().write(JsonUtil.toJson(data));
    }

    private void addAddress(HttpServletRequest request, HttpServletResponse response, User user) 
            throws IOException, ServletException {
        Address address = extractAddressFromRequest(request, user.getUserId());

        // Server-side validation
        Map<String, String> validationErrors = addressService.validate(address);
        if (!validationErrors.isEmpty()) {
            respondWithErrors(request, response, address, validationErrors);
            return;
        }

        // Set as default if it's the first address
        if (addressService.getAddressCount(user.getUserId()) == 0) {
            address.setDefault(true);
        }
        
        boolean success = addressService.addAddress(address);
        
        if (isAjax(request)) {
            response.setContentType("application/json");
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            data.put("message", success ? "Address added successfully" : "Failed to add address");
            data.put("address", success ? address : null);
            response.getWriter().write(JsonUtil.toJson(data));
        } else {
            if (success) {
                response.sendRedirect(request.getContextPath() + "/account/addresses");
            } else {
                request.setAttribute("error", "Failed to add address");
                request.setAttribute("address", address);
                request.getRequestDispatcher("/WEB-INF/views/account/address-form.jsp").forward(request, response);
            }
        }
    }

    private void editAddress(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException, ServletException {
        try {
            int addressId = Integer.parseInt(request.getParameter("addressId"));

            // Ownership check: verify this address belongs to the current user
            Address existing = addressService.getAddressById(addressId, user.getUserId());
            if (existing == null) {
                if (acceptsJson(request)) {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(JsonUtil.toJson(Map.of(
                            "success", false,
                            "message", "Address not found or access denied"
                    )));
                } else {
                    request.setAttribute("error", "Address not found");
                    listAddresses(request, response, user);
                }
                return;
            }

            Address address = extractAddressFromRequest(request, user.getUserId());
            address.setAddressId(addressId);

            // Server-side validation
            Map<String, String> validationErrors = addressService.validate(address);
            if (!validationErrors.isEmpty()) {
                respondWithErrors(request, response, address, validationErrors);
                return;
            }

            boolean success = addressService.updateAddress(address);
            
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                Map<String, Object> data = new HashMap<>();
                data.put("success", success);
                data.put("message", success ? "Address updated successfully" : "Failed to update address");
                response.getWriter().write(JsonUtil.toJson(data));
            } else {
                if (success) {
                    response.sendRedirect(request.getContextPath() + "/account/addresses");
                } else {
                    request.setAttribute("error", "Failed to update address");
                    request.setAttribute("address", address);
                    request.getRequestDispatcher("/WEB-INF/views/account/address-form.jsp").forward(request, response);
                }
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid address ID: {}", request.getParameter("addressId"));
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                response.getWriter().write(JsonUtil.toJson(Map.of(
                    "success", false,
                    "message", "Invalid address ID"
                )));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID");
            }
        }
    }

    private void deleteAddress(HttpServletRequest request, HttpServletResponse response, User user) 
            throws IOException {
        try {
            int addressId = Integer.parseInt(request.getParameter("addressId"));
            boolean success = addressService.deleteAddress(addressId, user.getUserId());
            
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                Map<String, Object> data = new HashMap<>();
                data.put("success", success);
                data.put("message", success ? "Address deleted successfully" : "Failed to delete address");
                response.getWriter().write(JsonUtil.toJson(data));
            } else {
                response.sendRedirect(request.getContextPath() + "/account/addresses");
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid address ID: {}", request.getParameter("addressId"));
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                response.getWriter().write(JsonUtil.toJson(Map.of(
                    "success", false,
                    "message", "Invalid address ID"
                )));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID");
            }
        }
    }

    private void setDefaultAddress(HttpServletRequest request, HttpServletResponse response, User user) 
            throws IOException {
        try {
            int addressId = Integer.parseInt(request.getParameter("addressId"));
            boolean success = addressService.setDefaultAddress(addressId, user.getUserId());
            
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                Map<String, Object> data = new HashMap<>();
                data.put("success", success);
                data.put("message", success ? "Default address updated successfully" : "Failed to update default address");
                response.getWriter().write(JsonUtil.toJson(data));
            } else {
                response.sendRedirect(request.getContextPath() + "/account/addresses");
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid address ID: {}", request.getParameter("addressId"));
            if (acceptsJson(request)) {
                response.setContentType("application/json");
                response.getWriter().write(JsonUtil.toJson(Map.of(
                    "success", false,
                    "message", "Invalid address ID"
                )));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid address ID");
            }
        }
    }

    private boolean isAjax(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String requestedWith = request.getHeader("X-Requested-With");
        return (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    private boolean acceptsJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void respondWithErrors(HttpServletRequest request, HttpServletResponse response,
                                   Address address, Map<String, String> validationErrors)
            throws IOException, ServletException {
        if (isAjax(request)) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            Map<String, Object> data = new HashMap<>();
            data.put("success", false);
            data.put("message", "Please correct the highlighted fields");
            data.put("errors", validationErrors);
            response.getWriter().write(JsonUtil.toJson(data));
        } else {
            String firstMessage = validationErrors.values().iterator().next();
            request.setAttribute("error", firstMessage);
            request.setAttribute("fieldErrors", validationErrors);
            request.setAttribute("address", address);
            CSRFProtection.addTokenToRequest(request);
            request.getRequestDispatcher("/WEB-INF/views/account/address-form.jsp")
                    .forward(request, response);
        }
    }

    private Address extractAddressFromRequest(HttpServletRequest request, int userId) {
        Address address = new Address();
        address.setUserId(userId);
        address.setAddressType(request.getParameter("addressType"));
        address.setFullName(request.getParameter("fullName"));
        address.setPhone(request.getParameter("phone"));
        address.setAddressLine1(request.getParameter("addressLine1"));
        address.setAddressLine2(request.getParameter("addressLine2"));
        address.setCity(request.getParameter("city"));
        address.setState(request.getParameter("state"));
        address.setPostalCode(request.getParameter("postalCode"));
        address.setCountry(request.getParameter("country"));
        address.setDefault("on".equals(request.getParameter("isDefault")));
        return address;
    }
}
