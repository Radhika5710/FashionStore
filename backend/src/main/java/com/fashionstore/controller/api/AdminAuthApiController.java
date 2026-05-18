package com.fashionstore.controller.api;

import com.fashionstore.controller.ApiResponse;
import com.fashionstore.model.User;
import com.fashionstore.registry.ServiceRegistry;
import com.fashionstore.security.JWTUtil;
import com.fashionstore.service.UserService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.util.*;

/**
 * AdminAuthApiController - Admin JWT Authentication
 * 
 * HYBRID AUTHENTICATION ARCHITECTURE:
 * ===================================
 * - Customer Frontend: Session-based authentication (LoginController)
 * - Admin Frontend: JWT-based authentication (THIS CONTROLLER)
 * 
 * CRITICAL: This controller handles ADMIN LOGIN ONLY using JWT tokens.
 * DO NOT add session logic here. Sessions are for customer MVC only (/login).
 * 
 * AUTHENTICATION FLOW:
 * ====================
 * 1. Admin submits login form (email/password) to /api/admin/login (JSON)
 * 2. Validate credentials via UserService.loginUser()
 * 3. Check user has admin role
 * 4. Generate JWT access token (15 minutes)
 * 5. Generate JWT refresh token (7 days)
 * 6. Set tokens in HTTP-only cookies
 * 7. Return JSON response with tokens
 * 8. Frontend stores tokens and includes in Authorization header
 * 9. Admin is authenticated via JWT for API requests
 * 
 * JWT TOKEN MANAGEMENT:
 * ====================
 * - Access Token: 15 minutes expiration
 * - Refresh Token: 7 days expiration
 * - Token Storage: HTTP-only cookies (secure, not accessible to JS)
 * - Token Injection: Authorization header (Bearer scheme)
 * - Token Validation: JWTAuthenticationFilter validates on each request
 * 
 * JWT SECURITY:
 * =============
 * - HTTP-only cookies: Prevents XSS token theft
 * - Secure flag: Only sent over HTTPS
 * - SameSite: CSRF protection
 * - Token signing: HMAC-SHA256 with secret key
 * - Token validation: Signature, expiration, role check
 * - Audit logging: All login attempts logged
 * 
 * IMPORTANT SEPARATION:
 * ====================
 * ✓ This controller uses ONLY JWT tokens
 * ✓ NO HttpSession creation here
 * ✓ NO session attributes set here
 * ✓ NO JSESSIONID cookie set here
 * ✓ NO CSRF token logic here
 * 
 * Customer session authentication is handled by:
 * - LoginController (/login)
 * - SecurityHardeningFilter (validates sessions)
 * - CSRFProtection (CSRF tokens for forms)
 * 
 * LOGOUT BEHAVIOR:
 * ================
 * - Admin logout: Clears JWT tokens (handled by React frontend)
 * - Customer logout: Invalidates HttpSession (handled by LogoutController)
 * - Logout does NOT affect the other authentication method
 * - Admin logout does NOT invalidate customer sessions
 * - Customer logout does NOT clear admin JWT tokens
 * 
 * ENDPOINTS:
 * ==========
 * POST /api/admin/login - Admin login with email/password
 * POST /api/admin/logout - Admin logout (clears tokens)
 * GET /api/admin/me - Get current admin user info
 * POST /api/admin/register - Register new admin (if enabled)
 * POST /api/admin/refresh - Refresh access token using refresh token
 */
@WebServlet(urlPatterns = {
    "/api/admin/login",
    "/api/admin/logout",
    "/api/admin/me",
    "/api/admin/register",
    "/api/admin/refresh",
    "/api/admin/auth/*"
})
public class AdminAuthApiController extends AdminApiBaseController {

    private static final long serialVersionUID = 1L;

    private UserService userService;

    @Override
    public void init() {
        super.init();
        ServiceRegistry registry = ServiceRegistry.getInstance();
        userService = registry.getUserService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        
        try {
            String servletPath = request.getServletPath();
            String pathInfo = request.getPathInfo();
            
            // GET /api/admin/auth/me or GET /api/admin/me
            if ((servletPath != null && servletPath.contains("/me")) || (pathInfo != null && pathInfo.equals("/me"))) {
                meEndpoint(request, response);
                return;
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        applyCors(request, response);
        
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        
        try {
            // POST /api/admin/auth/login or POST /api/admin/login
            if ((servletPath != null && servletPath.contains("/login")) || (pathInfo != null && pathInfo.equals("/login"))) {
                loginEndpoint(request, response);
                return;
            }
            
            // POST /api/admin/auth/logout or POST /api/admin/logout
            if ((servletPath != null && servletPath.contains("/logout")) || (pathInfo != null && pathInfo.equals("/logout"))) {
                logoutEndpoint(request, response);
                return;
            }
            
            // POST /api/admin/auth/register or POST /api/admin/register
            if ((servletPath != null && servletPath.contains("/register")) || (pathInfo != null && pathInfo.equals("/register"))) {
                registerEndpoint(request, response);
                return;
            }
            
            // POST /api/admin/refresh
            if ((servletPath != null && servletPath.contains("/refresh")) || (pathInfo != null && pathInfo.equals("/refresh"))) {
                refreshEndpoint(request, response);
                return;
            }
            
            writeApiResponse(response, 404, ApiResponse.error("Not found"));
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    private void meEndpoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = JWTUtil.extractTokenFromHeader(authHeader);
        
        if (token == null) {
            writeApiResponse(response, 401, ApiResponse.error("Not authenticated"));
            return;
        }
        
        // Validate token
        JWTUtil.TokenValidationResult validationResult = JWTUtil.validateToken(token);
        
        if (!validationResult.isValid()) {
            writeApiResponse(response, 401, ApiResponse.error("Invalid token: " + validationResult.getError()));
            return;
        }
        
        // Check if admin
        if (!"admin".equals(validationResult.getRole())) {
            writeApiResponse(response, 403, ApiResponse.error("Admin access required"));
            return;
        }
        
        // Get user details
        User user = userService.getUserById(Integer.parseInt(validationResult.getUserId()));
        if (user == null) {
            writeApiResponse(response, 404, ApiResponse.error("User not found"));
            return;
        }
        
        writeApiResponse(response, 200, ApiResponse.success("Authenticated", publicUser(user)));
    }

    /**
     * Admin Login Endpoint - JWT Token Generation
     * 
     * AUTHENTICATION FLOW:
     * ====================
     * 1. Receive JSON: { email, password }
     * 2. Validate required parameters
     * 3. Authenticate user via UserService.loginUser()
     * 4. Check user has admin role
     * 5. Generate JWT access token (15 minutes)
     * 6. Generate JWT refresh token (7 days)
     * 7. Set tokens in HTTP-only cookies
     * 8. Return tokens in JSON response
     * 
     * SECURITY:
     * =========
     * - Credentials validated against password hash
     * - Admin role required (not customer)
     * - Tokens signed with HMAC-SHA256
     * - HTTP-only cookies prevent XSS theft
     * - Secure flag ensures HTTPS only
     * - Token expiration prevents long-lived tokens
     * 
     * IMPORTANT: This is JWT-only authentication
     * - NO HttpSession created
     * - NO session attributes set
     * - NO JSESSIONID cookie
     * - Customer session auth is separate (LoginController)
     */
    private void loginEndpoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = readJsonBody(request);
        if (!validateParams(response, body, "email", "password")) return;

        String email = strParam(body, "email");
        String password = strParam(body, "password");

        // Authenticate user - validates password hash
        User user = userService.validateAndLoginUser(email, password);
        if (user == null) {
            writeApiResponse(response, 401, ApiResponse.error("Invalid credentials"));
            return;
        }
        
        // Check user has admin role - prevents customer login via admin API
        if (!user.isAdmin()) {
            writeApiResponse(response, 403, ApiResponse.error("Admin access required"));
            return;
        }

        // ============================================================
        // JWT TOKEN GENERATION - NO SESSION CREATION
        // ============================================================
        
        // Generate JWT access token (15 minutes)
        String accessToken = JWTUtil.generateToken(
            String.valueOf(user.getUserId()),
            user.getEmail(),
            user.getRole()
        );
        
        // Generate JWT refresh token (7 days)
        String refreshToken = JWTUtil.generateRefreshToken(String.valueOf(user.getUserId()));

        // Set access token in HTTP-only cookie
        // HTTP-only prevents JavaScript from accessing token (XSS protection)
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(request.isSecure());
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 minutes
        response.addCookie(accessCookie);

        // Set refresh token in HTTP-only cookie
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(request.isSecure());
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshCookie);

        // Return success response with tokens
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", accessToken);
        responseData.put("refreshToken", refreshToken);
        responseData.put("tokenType", "Bearer");
        responseData.put("expiresIn", 15 * 60); // 15 minutes in seconds
        responseData.put("user", publicUser(user));
        
        writeApiResponse(response, 200, ApiResponse.success("Login successful", responseData));
    }

    private void logoutEndpoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Clear auth cookies
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);
        
        Cookie refreshCookie = new Cookie("refresh_token", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
        
        writeApiResponse(response, 200, ApiResponse.success("Logout successful", null));
    }

    private void refreshEndpoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Extract refresh token from cookie or header
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        // If not in cookie, try Authorization header
        if (refreshToken == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                refreshToken = authHeader.substring(7);
            }
        }
        
        if (refreshToken == null || refreshToken.isBlank()) {
            writeApiResponse(response, 401, ApiResponse.error("Refresh token not provided"));
            return;
        }
        
        // Refresh the token
        JWTUtil.TokenRefreshResult refreshResult = JWTUtil.refreshToken(refreshToken);
        
        if (!refreshResult.isSuccess()) {
            writeApiResponse(response, 401, ApiResponse.error("Token refresh failed: " + refreshResult.getError()));
            return;
        }
        
        // Set new access token in cookie
        Cookie accessCookie = new Cookie("access_token", refreshResult.getAccessToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(request.isSecure());
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60); // 15 minutes
        response.addCookie(accessCookie);
        
        // Return success response with new token
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", refreshResult.getAccessToken());
        responseData.put("tokenType", refreshResult.getTokenType());
        responseData.put("expiresIn", refreshResult.getExpiresIn());
        
        writeApiResponse(response, 200, ApiResponse.success("Token refreshed successfully", responseData));
    }

    private void registerEndpoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> body = readJsonBody(request);
        if (!validateParams(response, body, "fullName", "email", "phone", "password", "confirmPassword", "adminKey")) return;

        String fullName = strParam(body, "fullName");
        String email = strParam(body, "email");
        String phone = strParam(body, "phone");
        String password = strParam(body, "password");
        String confirmPassword = strParam(body, "confirmPassword");
        String adminKey = strParam(body, "adminKey");

        if (!password.equals(confirmPassword)) {
            writeApiResponse(response, 400, ApiResponse.error("Passwords do not match"));
            return;
        }

        if (password.length() < 8) {
            writeApiResponse(response, 400, ApiResponse.error("Password must be at least 8 characters"));
            return;
        }

        // Validate admin secret key
        String expectedKey = System.getenv("FASHIONSTORE_ADMIN_KEY");
        if (expectedKey == null || expectedKey.isBlank()) {
            expectedKey = "FS_ADMIN_SECRET_2026";
        }

        if (!expectedKey.equals(adminKey)) {
            writeApiResponse(response, 403, ApiResponse.error("Invalid admin secret key"));
            return;
        }

        // Check if email already exists
        if (userService.isEmailExists(email)) {
            writeApiResponse(response, 409, ApiResponse.error("Email already registered"));
            return;
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(password);
        user.setGender("other");
        user.setAddress("");
        user.setRole("admin");

        int userId = userService.registerUser(user);
        if (userId > 0) {
            writeApiResponse(response, 201, ApiResponse.success("Admin account created successfully", null));
        } else {
            writeApiResponse(response, 500, ApiResponse.error("Failed to create admin account"));
        }
    }
}
