package com.fashionstore.controller;

import com.fashionstore.dao.ProductDAO;
import com.fashionstore.dao.ProductSizeDAO;
import com.fashionstore.dao.CategoryDAO;
import com.fashionstore.daoimpl.CategoryDAOImpl;
import com.fashionstore.daoimpl.ProductDAOImpl;
import com.fashionstore.daoimpl.ProductSizeDAOImpl;
import com.fashionstore.model.Category;
import com.fashionstore.model.Product;
import com.fashionstore.model.ProductSize;
import com.fashionstore.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/admin/products")
public class AdminProductController extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private ProductDAO productDAO;
    private ProductSizeDAO sizeDAO;
    private CategoryDAO categoryDAO;

    @Override
    public void init() {
        productDAO = new ProductDAOImpl();
        sizeDAO = new ProductSizeDAOImpl();
        categoryDAO = new CategoryDAOImpl();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        if (!isAdmin(request)) {
            if (isAuthenticated(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "add":
                showAddForm(request, response);
                break;
            case "edit":
                showEditForm(request, response);
                break;
            case "delete":
                deleteProduct(request, response);
                break;
            default:
                listProducts(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        if (!isAdmin(request)) {
            if (isAuthenticated(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        String action = request.getParameter("action");
        if ("add".equals(action)) {
            saveProduct(request, response, true);
        } else if ("edit".equals(action)) {
            saveProduct(request, response, false);
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        return user != null && user.isAdmin();
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("user") instanceof User;
    }

    private void listProducts(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Product> products = productDAO.getAllProducts();
        request.setAttribute("products", products);
        request.getRequestDispatcher("/WEB-INF/views/admin-products.jsp").forward(request, response);
    }

    private void showAddForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("categories", safeActiveCategories());
        request.setAttribute("mode", "add");
        request.getRequestDispatcher("/WEB-INF/views/admin-product-form.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        Product product = productDAO.getProductById(id);
        request.setAttribute("product", product);
        request.setAttribute("categories", safeActiveCategories());
        request.setAttribute("mode", "edit");
        request.getRequestDispatcher("/WEB-INF/views/admin-product-form.jsp").forward(request, response);
    }

    private void saveProduct(HttpServletRequest request, HttpServletResponse response, boolean isNew)
            throws IOException, ServletException {
        
        try {
            Product p = new Product();
            int productId = 0;
            
            if (!isNew) {
                String pidStr = request.getParameter("productId");
                if (pidStr == null || pidStr.isEmpty()) {
                    request.setAttribute("error", "Invalid Product ID");
                    showAddForm(request, response);
                    return;
                }
                productId = Integer.parseInt(pidStr);
                p.setProductId(productId);
            }
            
            String name = request.getParameter("productName");
            String desc = request.getParameter("description");
            String priceStr = request.getParameter("price");
            String discountStr = request.getParameter("discountPercent");
            String imgUrl = request.getParameter("imageUrl");
            String brand = request.getParameter("brand");
            String categoryIdStr = request.getParameter("categoryId");
            boolean active = request.getParameter("active") != null || isNew;
            boolean badgeNew = request.getParameter("isNew") != null;
            boolean badgeSale = request.getParameter("isSale") != null;
            boolean badgeTrending = request.getParameter("isTrending") != null;

            // ── Backend Validation ──────────────────────────────
            if (name == null || name.trim().isEmpty() || priceStr == null || priceStr.isEmpty() || categoryIdStr == null || categoryIdStr.isBlank()) {
                request.setAttribute("error", "Product Name, Price, and Category are required.");
                if (isNew) showAddForm(request, response);
                else showEditForm(request, response);
                return;
            }

            p.setProductName(name);
            p.setDescription(desc);
            p.setPrice(Double.parseDouble(priceStr));
            p.setDiscountPercent(discountStr != null && !discountStr.isBlank() ? Double.parseDouble(discountStr) : 0.0);
            p.setImageUrl(imgUrl);
            p.setBrand(brand != null && !brand.isBlank() ? brand.trim() : null);
            p.setActive(active);
            p.setNew(badgeNew);
            p.setSale(badgeSale);
            p.setTrending(badgeTrending);
            p.setCategoryId(Integer.parseInt(categoryIdStr.trim()));

            if (isNew) {
                productId = productDAO.addProduct(p);
            } else {
                boolean updated = productDAO.updateProduct(p);
                if (updated) {
                    productId = p.getProductId();
                }
            }

            if (productId > 0) {
                // ── Architecture Upgrade: Handle Multiple Sizes ────────────────────────
                String[] labels = {"S", "M", "L", "XL"};
                int totalStock = 0;
                for (String label : labels) {
                    String stockStr = request.getParameter("stock_" + label);
                    if (stockStr != null) {
                        int stock = Integer.parseInt(stockStr);
                        totalStock += Math.max(0, stock);
                        ProductSize size = new ProductSize();
                        size.setProductId(productId);
                        size.setSizeLabel(label);
                        size.setStockQuantity(stock);
                        sizeDAO.addOrUpdateSize(size);
                    }
                }
                p.setProductId(productId);
                p.setStockQuantity(totalStock);
                productDAO.updateProduct(p);
                request.getSession(true).setAttribute("message", "Product " + (isNew ? "added" : "updated") + " successfully!");
            } else {
                request.getSession(true).setAttribute("error", "Failed to save product.");
            }

        } catch (NumberFormatException e) {
            request.getSession(true).setAttribute("error", "Invalid numeric values provided.");
        }

        response.sendRedirect(request.getContextPath() + "/admin/products");
    }

    private List<Category> safeActiveCategories() {
        try {
            List<Category> categories = categoryDAO.getActiveCategories();
            return categories != null ? categories : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void deleteProduct(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        productDAO.deleteProduct(id);
        response.sendRedirect(request.getContextPath() + "/admin/products");
    }
}
