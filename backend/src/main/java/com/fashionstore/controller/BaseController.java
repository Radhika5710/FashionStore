package com.fashionstore.controller;

import com.fashionstore.security.AuthContext;
import com.fashionstore.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Base Controller class that centralizes CORS, JSON parsing, validation abstractions, and centralized exception handling
 */
public abstract class BaseController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected Set<String> allowedOrigins;

    @Override
    public void init() {
        String allowedOriginsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            allowedOrigins = new HashSet<>(Arrays.asList(allowedOriginsEnv.split(",")));
        } else {
            // Development fallback origins
            allowedOrigins = new HashSet<>(Arrays.asList(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
            ));
        }
    }

    protected void applyCors(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && allowedOrigins != null && allowedOrigins.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type,X-Requested-With,X-CSRF-Token");
            response.setHeader("Access-Control-Max-Age", "3600");
        }
    }

    protected void writeJson(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JsonUtil.toJson(data));
    }

    protected <T> void writeApiResponse(HttpServletResponse response, int status, ApiResponse<T> apiResponse) throws IOException {
        writeJson(response, status, apiResponse);
    }

    protected Map<String, Object> readJsonBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = request.getReader()) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        String body = sb.toString().trim();
        if (body.isEmpty()) return new HashMap<>();
        try {
            Map<String, Object> parsed = JsonUtil.gson().fromJson(body,
                    new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
            return parsed != null ? parsed : new HashMap<>();
        } catch (Exception e) {
            Map<String, Object> form = new HashMap<>();
            for (String pair : body.split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0) {
                    form.put(java.net.URLDecoder.decode(pair.substring(0, idx), java.nio.charset.StandardCharsets.UTF_8),
                             java.net.URLDecoder.decode(pair.substring(idx + 1), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            return form;
        }
    }

    protected String strParam(Map<String, Object> body, String key) {
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    protected int parseInt(String s, int defaultVal) {
        if (s == null || s.isBlank()) return defaultVal;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return defaultVal; }
    }

    protected double parseDouble(Object v, double defaultVal) {
        if (v == null) return defaultVal;
        try { return Double.parseDouble(String.valueOf(v).trim()); } catch (NumberFormatException e) { return defaultVal; }
    }

    protected boolean parseBoolean(Object v, boolean defaultVal) {
        if (v == null) return defaultVal;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    protected int parseIntFromObject(Object v, int defaultVal) {
        if (v == null) return defaultVal;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(String.valueOf(v).trim());
            } catch (NumberFormatException ex) {
                return defaultVal;
            }
        }
    }

    protected boolean ensureAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Use JWT authentication for admin APIs (AuthContext)
        // Session authentication is for customer MVC pages only
        AuthContext authContext = AuthContext.fromRequest(request);
        
        if (!authContext.isAuthenticated()) {
            writeApiResponse(response, 401, ApiResponse.error("Authentication required"));
            return false;
        }
        
        if (!authContext.isAdmin()) {
            writeApiResponse(response, 403, ApiResponse.error("Admin access required"));
            return false;
        }
        
        return true;
    }

    protected boolean isTrustedStateChangingRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        String local = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());

        if (origin != null && !origin.isBlank()) {
            return origin.equals(local) || (allowedOrigins != null && allowedOrigins.contains(origin));
        }
        if (referer != null && !referer.isBlank()) {
            return referer.startsWith(local) || (allowedOrigins != null && allowedOrigins.stream().anyMatch(referer::startsWith));
        }
        return false;
    }

    protected void handleException(HttpServletResponse response, Exception ex) {
        logger.error("Controller runtime error: {}", ex.getMessage(), ex);
        try {
            writeApiResponse(response, 500, ApiResponse.error("Internal Server Error: " + ex.getMessage()));
        } catch (IOException e) {
            logger.error("Failed to write exception response", e);
        }
    }

    protected boolean validateParams(HttpServletResponse response, Map<String, Object> body, String... requiredFields) throws IOException {
        List<String> missing = new ArrayList<>();
        for (String field : requiredFields) {
            if (!body.containsKey(field) || String.valueOf(body.get(field)).trim().isEmpty()) {
                missing.add(field);
            }
        }
        if (!missing.isEmpty()) {
            writeApiResponse(response, 400, ApiResponse.error("Missing required fields: " + String.join(", ", missing)));
            return false;
        }
        return true;
    }
}
