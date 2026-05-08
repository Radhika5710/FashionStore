package com.fashionstore.filter;

import com.fashionstore.security.CSRFProtection;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*")
public class CSRFFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if ("GET".equalsIgnoreCase(req.getMethod())) {
            CSRFProtection.addTokenToRequest(req);
        }

        if (CSRFProtection.requiresProtection(req) && !CSRFProtection.validateRequest(req)) {
            if ("XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write("{\"success\":false,\"message\":\"CSRF validation failed\"}");
                return;
            }
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "CSRF validation failed.");
            return;
        }

        chain.doFilter(request, response);
    }
}
