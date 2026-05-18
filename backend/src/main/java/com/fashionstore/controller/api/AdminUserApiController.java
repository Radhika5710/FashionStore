package com.fashionstore.controller.api;

import com.fashionstore.controller.ApiResponse;
import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.*;

/**
 * Modular API controller for user management in admin dashboard
 * Handles: GET /api/admin/users, GET /api/admin/users/{id}, PUT /api/admin/users/{id}, DELETE /api/admin/users/{id}
 */
@WebServlet("/api/admin/users/*")
public class AdminUserApiController extends AdminApiBaseController {

    private static final long serialVersionUID = 1L;

    private UserService userService;

    @Override
    public void init() {
        super.init();
        userService = ServiceRegistry.getInstance().getUserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/admin/users - List all users
                List<User> users = userService.getAllUsers();
                writeApiResponse(response, 200, ApiResponse.success("Users retrieved successfully", Map.of(
                    "users", users.stream().map(this::publicUser).toList(),
                    "count", users.size()
                )));
                return;
            }
            
            // GET /api/admin/users/recent - Get recent users
            if (pathInfo.equals("/recent")) {
                int limit = parseInt(request.getParameter("limit"), 10);
                List<User> users = userService.getAllUsers();
                writeApiResponse(response, 200, ApiResponse.success("Recent users retrieved successfully", Map.of(
                    "users", users.stream().limit(limit).map(this::publicUser).toList(),
                    "count", Math.min(limit, users.size())
                )));
                return;
            }
            
            // GET /api/admin/users/{id} - Get single user
            String[] segments = pathInfo.split("/");
            if (segments.length == 2) {
                try {
                    int userId = Integer.parseInt(segments[1]);
                    User user = userService.getUserById(userId);
                    if (user == null) {
                        writeApiResponse(response, 404, ApiResponse.error("User not found"));
                        return;
                    }
                    writeApiResponse(response, 200, ApiResponse.success("User retrieved successfully", publicUser(user)));
                } catch (NumberFormatException e) {
                    writeApiResponse(response, 400, ApiResponse.error("Invalid user ID"));
                }
                return;
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!isTrustedStateChangingRequest(request)) {
            writeApiResponse(response, 403, ApiResponse.error("Blocked by origin policy"));
            return;
        }
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && !pathInfo.equals("/")) {
                String[] segments = pathInfo.split("/");
                if (segments.length == 2) {
                    try {
                        int userId = Integer.parseInt(segments[1]);
                        User existing = userService.getUserById(userId);
                        if (existing == null) {
                            writeApiResponse(response, 404, ApiResponse.error("User not found"));
                            return;
                        }
                        
                        Map<String, Object> body = readJsonBody(request);
                        Object roleObj = body.get("role");
                        if (roleObj != null) {
                            boolean success = userService.updateUserRole(userId, String.valueOf(roleObj));
                            if (success) {
                                writeApiResponse(response, 200, ApiResponse.success("User role updated successfully", null));
                            } else {
                                writeApiResponse(response, 400, ApiResponse.error("Failed to update user role"));
                            }
                            return;
                        }
                        
                        Object blockedObj = body.get("blocked");
                        if (blockedObj != null) {
                            boolean blocked = Boolean.parseBoolean(String.valueOf(blockedObj));
                            boolean success = userService.updateUserRole(userId, blocked ? "disabled" : "user");
                            if (success) {
                                writeApiResponse(response, 200, ApiResponse.success("User status updated successfully", null));
                            } else {
                                writeApiResponse(response, 400, ApiResponse.error("Failed to update user status"));
                            }
                            return;
                        }
                        
                        writeApiResponse(response, 400, ApiResponse.error("No valid update field"));
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid user ID"));
                    }
                    return;
                }
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        if (!isTrustedStateChangingRequest(request)) {
            writeApiResponse(response, 403, ApiResponse.error("Blocked by origin policy"));
            return;
        }
        if (!ensureAdmin(request, response)) return;
        
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && !pathInfo.equals("/")) {
                String[] segments = pathInfo.split("/");
                if (segments.length == 2) {
                    try {
                        int userId = Integer.parseInt(segments[1]);
                        // Soft-delete by disabling
                        boolean success = userService.updateUserRole(userId, "disabled");
                        if (success) {
                            writeApiResponse(response, 200, ApiResponse.success("User disabled successfully", null));
                        } else {
                            writeApiResponse(response, 400, ApiResponse.error("Failed to disable user"));
                        }
                    } catch (NumberFormatException e) {
                        writeApiResponse(response, 400, ApiResponse.error("Invalid user ID"));
                    }
                    return;
                }
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        applyCors(request, response);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}
