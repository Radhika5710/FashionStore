<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.Product" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Product Management");
    request.setAttribute("_pageCSS", "admin");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>
<body class="admin-dashboard">

<div class="admin-layout">
    <!-- SIDEBAR -->
    <aside class="admin-sidebar">
        <div class="sidebar-brand">FashionStore Admin</div>
        <nav class="sidebar-nav">
            <a href="<%= request.getContextPath() %>/admin/dashboard" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="7" height="7"/>
                    <rect x="14" y="3" width="7" height="7"/>
                    <rect x="14" y="14" width="7" height="7"/>
                    <rect x="3" y="14" width="7" height="7"/>
                </svg>
                Dashboard
            </a>
            <a href="<%= request.getContextPath() %>/admin/products" class="sidebar-link active">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                    <line x1="7" y1="7" x2="7.01" y2="7"/>
                </svg>
                Products
            </a>
            <a href="<%= request.getContextPath() %>/admin/orders" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
                    <line x1="3" y1="6" x2="21" y2="6"/>
                    <path d="M16 10a4 4 0 0 1-8 0"/>
                </svg>
                Orders
            </a>
            <a href="<%= request.getContextPath() %>/admin/users" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                    <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                Users
            </a>
            <a href="<%= request.getContextPath() %>/products" class="sidebar-link">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/>
                </svg>
                Back to Store
            </a>
        </nav>
    </aside>

    <!-- MAIN CONTENT -->
    <main class="admin-content">
        <div class="admin-header">
            <h1 class="admin-title">Product Management</h1>
            <a href="<%= request.getContextPath() %>/admin/products?action=add" class="add-btn">+ Add New Product</a>
        </div>

        <!-- PRODUCT SEARCH -->
        <div class="glass-card" style="padding: var(--space-4); margin-bottom: var(--space-5);">
            <div style="display: flex; gap: var(--space-3); flex-wrap: wrap; align-items: center;">
                <div style="flex: 1; min-width: 200px;">
                    <label style="font-size: 11px; font-weight: 600; text-transform: uppercase; color: var(--color-secondary); margin-bottom: 4px; display: block;">Search Products</label>
                    <input type="text" id="searchProducts" placeholder="Search by name, brand..." style="width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 14px;">
                </div>
                <div style="min-width: 150px;">
                    <label style="font-size: 11px; font-weight: 600; text-transform: uppercase; color: var(--color-secondary); margin-bottom: 4px; display: block;">Stock Status</label>
                    <select id="stockFilter" style="width: 100%; padding: 10px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 14px; background: var(--color-surface);">
                        <option value="">All Products</option>
                        <option value="instock">In Stock</option>
                        <option value="lowstock">Low Stock</option>
                        <option value="outofstock">Out of Stock</option>
                    </select>
                </div>
            </div>
        </div>

        <!-- UI FEEDBACK -->
        <% String message = (String) session.getAttribute("message"); %>
        <% if (message != null) { %>
            <div class="alert alert-success" style="margin-bottom: var(--space-4);"><%= message %></div>
            <% session.removeAttribute("message"); %>
        <% } %>

        <% String error = (String) session.getAttribute("error"); %>
        <% if (error != null) { %>
            <div class="alert alert-danger" style="margin-bottom: var(--space-4);"><%= error %></div>
            <% session.removeAttribute("error"); %>
        <% } %>

        <!-- PRODUCTS TABLE -->
        <div class="admin-table-container glass-card">
            <table class="admin-table" role="table" aria-label="Products table">
                <thead>
                    <tr>
                        <th scope="col">Image</th>
                        <th scope="col">Name</th>
                        <th scope="col">Price</th>
                        <th scope="col">Sizes &amp; Stock</th>
                        <th scope="col">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        List<Product> products = (List<Product>) request.getAttribute("products");
                        if (products != null) {
                            for (Product p : products) {
                    %>
                    <tr data-product-name="<%= p.getProductName().toLowerCase() %>" data-brand="<%= (p.getBrand() != null ? p.getBrand().toLowerCase() : "") %>" data-stock="<%= getStockStatus(p) %>">
                        <td>
                            <div class="product-image-wrapper">
                                <img src="<%= p.getImageUrl() %>" class="prod-img" alt="<%= p.getProductName() %>" onclick="showImagePreview('<%= p.getImageUrl() %>', '<%= p.getProductName() %>')">
                            </div>
                        </td>
                        <td>
                            <div style="font-weight: 500;"><%= p.getProductName() %></div>
                            <div style="font-size: 12px; color: var(--color-secondary); margin-top: 4px;"><%= p.getBrand() != null ? p.getBrand() : "" %></div>
                        </td>
                        <td>₹<%= String.format("%.2f", p.getPrice()) %></td>
                        <td>
                            <%
                                if (p.getSizes() != null && !p.getSizes().isEmpty()) {
                                    for (com.fashionstore.model.ProductSize s : p.getSizes()) {
                            %>
                                <span class="size-badge <%= s.getStockQuantity() < 10 ? "low-stock" : "" %>"><%= s.getSizeLabel() %>: <%= s.getStockQuantity() %></span>
                            <%
                                    }
                                } else {
                            %>
                                <span class="no-sizes" style="color: var(--color-secondary); font-size: 13px;">No sizes set</span>
                            <% } %>
                        </td>
                        <td class="action-btns">
                            <a href="<%= request.getContextPath() %>/admin/products?action=edit&id=<%= p.getProductId() %>" class="edit-link">Edit</a>
                            <a href="<%= request.getContextPath() %>/admin/products?action=delete&id=<%= p.getProductId() %>" class="delete-link" onclick="return confirm('Delete this product?')">Delete</a>
                        </td>
                    </tr>
                    <%
                            }
                        }
                    %>
                </tbody>
            </table>
        </div>
    </main>
</div>

<!-- IMAGE PREVIEW MODAL -->
<div id="imageModal" class="modal" style="display: none;">
    <div class="modal-content" style="max-width: 600px;">
        <span class="close-modal" onclick="closeImagePreview()">&times;</span>
        <img id="modalImage" src="" alt="Product Preview" style="width: 100%; border-radius: var(--radius-md);">
        <p id="modalImageName" style="text-align: center; margin-top: var(--space-3); font-weight: 500;"></p>
    </div>
</div>

<style>
.modal {
    position: fixed;
    z-index: 1000;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0,0,0,0.8);
    display: none;
}

.modal-content {
    background: var(--color-surface);
    margin: 10% auto;
    padding: var(--space-5);
    border-radius: var(--radius-lg);
    position: relative;
}

.close-modal {
    position: absolute;
    right: 20px;
    top: 10px;
    font-size: 28px;
    font-weight: bold;
    color: var(--text-primary);
    cursor: pointer;
}

.product-image-wrapper {
    position: relative;
    cursor: pointer;
    display: inline-block;
}

.product-image-wrapper:hover::after {
    content: '🔍';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    font-size: 24px;
    color: white;
    text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.size-badge.low-stock {
    background: #fef3c7;
    color: #92400e;
}
</style>

<script>
// Product search and filter functionality
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchProducts');
    const stockFilter = document.getElementById('stockFilter');
    
    if (searchInput && stockFilter) {
        function filterProducts() {
            const searchTerm = searchInput.value.toLowerCase();
            const stockValue = stockFilter.value;
            
            document.querySelectorAll('tbody tr').forEach(row => {
                const productName = row.getAttribute('data-product-name') || '';
                const brand = row.getAttribute('data-brand') || '';
                const stockStatus = row.getAttribute('data-stock') || '';
                
                const matchesSearch = productName.includes(searchTerm) || brand.includes(searchTerm);
                const matchesStock = stockValue === '' || stockStatus === stockValue;
                
                row.style.display = matchesSearch && matchesStock ? '' : 'none';
            });
        }
        
        searchInput.addEventListener('input', filterProducts);
        stockFilter.addEventListener('change', filterProducts);
    }
});

// Image preview functionality
function showImagePreview(imageUrl, productName) {
    const modal = document.getElementById('imageModal');
    const modalImage = document.getElementById('modalImage');
    const modalImageName = document.getElementById('modalImageName');
    
    modalImage.src = imageUrl;
    modalImageName.textContent = productName;
    modal.style.display = 'block';
}

function closeImagePreview() {
    document.getElementById('imageModal').style.display = 'none';
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('imageModal');
    if (event.target == modal) {
        closeImagePreview();
    }
}

<%!
    private String getStockStatus(Product p) {
        if (p.getSizes() == null || p.getSizes().isEmpty()) {
            return "outofstock";
        }
        boolean hasStock = false;
        boolean lowStock = false;
        for (com.fashionstore.model.ProductSize s : p.getSizes()) {
            if (s.getStockQuantity() > 0) {
                hasStock = true;
                if (s.getStockQuantity() < 10) {
                    lowStock = true;
                }
            }
        }
        if (!hasStock) return "outofstock";
        if (lowStock) return "lowstock";
        return "instock";
    }
%>
</script>

</body>
</html>
