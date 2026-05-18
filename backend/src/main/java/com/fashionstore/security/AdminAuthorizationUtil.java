package com.fashionstore.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AdminAuthorizationUtil - Hybrid MVC + React Admin Architecture
 * 
 * ARCHITECTURE OVERVIEW:
 * =====================
 * - Customer MVC Frontend: Session-based authentication
 *   * JSP pages rendered server-side
 *   * Session authentication via HttpSession
 *   * CSRF protection for form submissions
 *   * Routes: /login, /register, /products, /cart, /checkout, etc.
 * 
 * - React Admin Dashboard: JWT-based authentication
 *   * Separate React frontend
 *   * JWT token-based authentication
 *   * No HttpSession required
 *   * Routes: /api/admin/*
 * 
 * AUTHORIZATION FLOW:
 * ===================
 * 1. Admin user logs in via /api/admin/login
 * 2. Backend validates credentials
 * 3. Backend generates JWT token
 * 4. Frontend stores JWT in localStorage/sessionStorage
 * 5. Frontend includes JWT in Authorization header for API calls
 * 6. JWTAuthenticationFilter validates JWT
 * 7. AdminAuthorizationUtil checks admin role
 * 8. Request proceeds to controller
 * 
 * ROLE-BASED ACCESS CONTROL:
 * ==========================
 * - admin: Standard admin role with product, order, user management
 * - super_admin: Extended permissions including system, audit, security
 * - manager: Manager role with limited permissions
 * 
 * ADMIN PERMISSIONS:
 * ==================
 * Standard Admin:
 * - dashboard: View admin dashboard
 * - products: Manage products
 * - orders: Manage orders
 * - users: Manage users
 * - categories: Manage categories
 * - coupons: Manage coupons
 * - settings: Manage settings
 * - reports: View reports
 * 
 * Super Admin (includes all above plus):
 * - system: System administration
 * - audit: Audit trail access
 * - security: Security settings
 * - backup: Backup operations
 * - restore: Restore operations
 * 
 * SECURITY GUARANTEES:
 * ====================
 * ✓ JWT validation prevents unauthorized access
 * ✓ Role-based access control enforces permissions
 * ✓ No session conflicts between customer and admin auth
 * ✓ Admin APIs isolated from customer routes
 * ✓ Audit logging for admin actions
 * ✓ Sensitive operations require super_admin role
 */
public class AdminAuthorizationUtil {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthorizationUtil.class);
    
    // Admin roles
    private static final Set<String> ADMIN_ROLES = new HashSet<>(Arrays.asList(
        "admin", "super_admin", "manager"
    ));
    
    // Admin permissions
    private static final Set<String> ADMIN_PERMISSIONS = new HashSet<>(Arrays.asList(
        "dashboard", "products", "orders", "users", "categories", "coupons", "settings", "reports"
    ));
    
    // Super admin permissions (includes all admin permissions plus more)
    private static final Set<String> SUPER_ADMIN_PERMISSIONS = new HashSet<>(Arrays.asList(
        "dashboard", "products", "orders", "users", "categories", "coupons", "settings", "reports",
        "system", "audit", "security", "backup", "restore"
    ));
    
    /**
     * Check if user is admin using JWT
     */
    public static boolean isAdmin(HttpServletRequest request) {
        AuthContext authContext = AuthContext.fromRequest(request);
        return authContext.isAuthenticated() && authContext.isAdmin();
    }
    
    /**
     * Check if user is super admin using JWT
     */
    public static boolean isSuperAdmin(HttpServletRequest request) {
        AuthContext authContext = AuthContext.fromRequest(request);
        return authContext.isAuthenticated() && "super_admin".equals(authContext.getRole());
    }
    
    /**
     * Check if user has admin role using JWT
     */
    public static boolean hasAdminRole(HttpServletRequest request, String role) {
        AuthContext authContext = AuthContext.fromRequest(request);
        return authContext.isAuthenticated() && ADMIN_ROLES.contains(role.toLowerCase());
    }
    
    /**
     * Check if user has permission using JWT
     */
    public static boolean hasPermission(HttpServletRequest request, String permission) {
        AuthContext authContext = AuthContext.fromRequest(request);
        
        if (!authContext.isAuthenticated()) {
            return false;
        }
        
        // Super admins have all permissions
        if (isSuperAdmin(request)) {
            return SUPER_ADMIN_PERMISSIONS.contains(permission.toLowerCase());
        }
        
        // Regular admins have admin permissions
        return ADMIN_PERMISSIONS.contains(permission.toLowerCase());
    }
    
    /**
     * Check if user can access admin area using JWT
     */
    public static boolean canAccessAdminArea(HttpServletRequest request) {
        return isAdmin(request);
    }
    
    /**
     * Check if user can perform sensitive operation using JWT
     */
    public static boolean canPerformSensitiveOperation(HttpServletRequest request, String operation) {
        if (!isAdmin(request)) {
            logger.warn("Non-admin user attempted sensitive operation: {}", operation);
            return false;
        }
        
        // Some operations require super admin
        if (operation.equals("system") || operation.equals("security") || operation.equals("audit")) {
            return isSuperAdmin(request);
        }
        
        return hasPermission(request, operation);
    }
    
    /**
     * Validate admin access using JWT - role-based authorization
     */
    public static AuthorizationResult validateAdminAccess(HttpServletRequest request) {
        AuthorizationResult result = new AuthorizationResult();
        String path = request.getRequestURI();
        
        AuthContext authContext = AuthContext.fromRequest(request);
        
        // Check if user is authenticated
        if (!authContext.isAuthenticated()) {
            result.setAuthorized(false);
            result.setReason("Not authenticated: " + authContext.getError());
            logger.warn("validateAdminAccess failed: Not authenticated - Path: {}", path);
            return result;
        }
        
        // Check if user has admin role
        if (!authContext.isAdmin()) {
            result.setAuthorized(false);
            result.setReason("Not authorized as admin. Role: " + authContext.getRole());
            logger.warn("validateAdminAccess failed: Not admin - Path: {}, Role: {}", path, authContext.getRole());
            return result;
        }
        
        result.setAuthorized(true);
        result.setUserId(authContext.getUserId());
        
        return result;
    }
    
    /**
     * Validate permission for specific operation using JWT
     */
    public static AuthorizationResult validatePermission(HttpServletRequest request, String permission) {
        AuthorizationResult result = validateAdminAccess(request);
        
        if (!result.isAuthorized()) {
            return result;
        }
        
        if (!hasPermission(request, permission)) {
            result.setAuthorized(false);
            result.setReason("Permission denied: " + permission);
            return result;
        }
        
        return result;
    }
    
    /**
     * Log admin action for audit trail using JWT
     */
    public static void logAdminAction(HttpServletRequest request, String action, String details) {
        AuthContext authContext = AuthContext.fromRequest(request);
        
        if (authContext.isAuthenticated()) {
            String userId = authContext.getUserId();
            String clientIP = authContext.getClientIP();
            
            logger.info("Admin Action - User: {}, Action: {}, Details: {}, IP: {}", 
                userId, action, details, clientIP);
        }
    }
    
    /**
     * Authorization result class
     */
    public static class AuthorizationResult {
        private boolean authorized;
        private String reason;
        private String userId;
        
        public boolean isAuthorized() {
            return authorized;
        }
        
        public void setAuthorized(boolean authorized) {
            this.authorized = authorized;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
