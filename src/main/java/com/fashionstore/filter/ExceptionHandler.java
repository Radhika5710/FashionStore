package com.fashionstore.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebFilter("/*")
public class ExceptionHandler implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Unhandled exception at {}: {}", httpRequest.getRequestURI(), e.getMessage(), e);

            boolean isAjax = "XMLHttpRequest".equals(httpRequest.getHeader("X-Requested-With")) ||
                             (httpRequest.getHeader("Accept") != null && httpRequest.getHeader("Accept").contains("application/json"));
            
            if (isAjax) {
                sendSanitizedError(httpResponse);
            } else {
                if (e instanceof ServletException) {
                    throw (ServletException) e;
                } else if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new ServletException(e);
                }
            }
        }
    }

    @Override
    public void destroy() {
    }

    private void sendSanitizedError(HttpServletResponse response) throws IOException {
        if (!response.isCommitted()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            PrintWriter out = response.getWriter();
            out.print("{\"success\":false,\"message\":\"An unexpected error occurred. Please try again later.\",\"status\":\"error\"}");
            out.flush();
        }
    }
}
