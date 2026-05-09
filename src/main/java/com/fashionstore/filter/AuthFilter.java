package com.fashionstore.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Authentication Filter to protect private pages like Cart, Checkout, and Orders.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();
        String contextPath = req.getContextPath();
        String relativePath = path.substring(contextPath.length());

        // ── PUBLIC PATHS (No Login Required) ──────────────────────────
        boolean isPublic = relativePath.equals("/") ||
                           relativePath.equals("/home") ||
                           relativePath.equals("/products") ||
                           relativePath.equals("/product") ||
                           relativePath.equals("/login") ||
                           relativePath.equals("/register") ||
                           relativePath.equals("/404") ||
                           relativePath.equals("/error") ||
                           relativePath.startsWith("/assets/");

        if (req.getDispatcherType() == DispatcherType.ERROR) {
            chain.doFilter(request, response);
            return;
        }

        if (isPublic) {
            chain.doFilter(request, response);
            return;
        }

        // ── PRIVATE PATHS (Login Required) ─────────────────────────────
        HttpSession session = req.getSession(false);
        com.fashionstore.model.User user = (session != null) ? (com.fashionstore.model.User) session.getAttribute("user") : null;
        boolean isLoggedIn = (user != null);

        if (!isLoggedIn) {
            boolean isAjax = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
            if (isAjax) {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"Please login to continue.\",\"redirect\":\"" + contextPath + "/login\"}");
            } else {
                resp.sendRedirect(contextPath + "/login");
            }
            return;
        }

        // ── ADMIN PROTECTION ───────────────────────────────────────────
        boolean isAdminPath = relativePath.equals("/admin") ||
                              relativePath.startsWith("/admin/");
        if (isAdminPath && !user.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin only.");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}
