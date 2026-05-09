package com.fashionstore.controller;

import com.fashionstore.model.Product;
import com.fashionstore.service.SearchService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Search controller for intelligent search and autocomplete
 */
@WebServlet("/search")
public class SearchController extends HttpServlet {
    
    private SearchService searchService;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        searchService = new SearchService();
        gson = new GsonBuilder().create();
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        
        if ("autocomplete".equals(action)) {
            handleAutocomplete(req, resp);
        } else if ("suggestions".equals(action)) {
            handleSuggestions(req, resp);
        } else if ("categories".equals(action)) {
            handleCategorySuggestions(req, resp);
        } else if ("fuzzy".equals(action)) {
            handleFuzzySearch(req, resp);
        } else if ("advanced".equals(action)) {
            handleAdvancedSearch(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
        }
    }
    
    /**
     * Handle autocomplete search
     */
    private void handleAutocomplete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        int limit = parseIntDefault(req.getParameter("limit"), 10);
        
        List<Product> products = searchService.autocomplete(query, limit);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(products));
    }
    
    /**
     * Handle keyword suggestions
     */
    private void handleSuggestions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        int limit = parseIntDefault(req.getParameter("limit"), 10);
        
        List<String> suggestions = searchService.getKeywordSuggestions(query, limit);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(suggestions));
    }
    
    /**
     * Handle category suggestions
     */
    private void handleCategorySuggestions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        
        List<String> categories = searchService.getCategorySuggestions(query);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(categories));
    }
    
    /**
     * Handle fuzzy search (typo tolerance)
     */
    private void handleFuzzySearch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        int maxDistance = parseIntDefault(req.getParameter("distance"), 2);
        int limit = parseIntDefault(req.getParameter("limit"), 10);
        
        List<Product> products = searchService.fuzzySearch(query, maxDistance, limit);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(products));
    }
    
    /**
     * Handle advanced search with filters
     */
    private void handleAdvancedSearch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getParameter("q");
        String category = req.getParameter("category");
        String color = req.getParameter("color");
        String material = req.getParameter("material");
        String season = req.getParameter("season");
        String occasion = req.getParameter("occasion");
        double minPrice = parseDoubleDefault(req.getParameter("minPrice"), 0);
        double maxPrice = parseDoubleDefault(req.getParameter("maxPrice"), 999999);
        String sortBy = stringDefault(req.getParameter("sortBy"), "newest");
        int limit = parseIntDefault(req.getParameter("limit"), 20);
        
        List<Product> products = searchService.advancedSearch(query, category, color, material, season, occasion, 
                                                         minPrice, maxPrice, sortBy, limit);
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(products));
    }
    
    /**
     * Helper method to parse int with default value
     */
    private int parseIntDefault(String value, int defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Helper method to parse double with default value
     */
    private double parseDoubleDefault(String value, double defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Helper method to get string with default value
     */
    private String stringDefault(String value, String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
