<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.Product" %>
<%@ page import="com.fashionstore.model.Category" %>
<%@ page import="java.net.URLEncoder" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Catalog");
    request.setAttribute("_pageCSS", "products");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<!-- NAVBAR -->
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    List<Product> products = new ArrayList<>();
    Object obj = request.getAttribute("products");
    if (obj != null && obj instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<Product> temp = (List<Product>) obj;
        products = temp;
    }

    String searchVal = (String) request.getAttribute("search");
    if (searchVal == null) searchVal = "";

    String minPriceVal = (String) request.getAttribute("minPrice");
    if (minPriceVal == null) minPriceVal = "";

    String maxPriceVal = (String) request.getAttribute("maxPrice");
    if (maxPriceVal == null) maxPriceVal = "";

    String sortByVal = (String) request.getAttribute("sortBy");
    if (sortByVal == null) sortByVal = "";

    String brandVal = (String) request.getAttribute("brand");
    if (brandVal == null) brandVal = "";

    Integer currentCategoryId = (Integer) request.getAttribute("categoryId");
    String currentCategorySlug = (String) request.getAttribute("categorySlug");
    if (currentCategorySlug == null) currentCategorySlug = "";
    String currentTag = (String) request.getAttribute("tag");
    if (currentTag == null) currentTag = "";

    List<String> allSizes = Arrays.asList("S", "M", "L", "XL", "7", "8", "9", "10", "OS");
    List<String> selectedSizes = new ArrayList<>();
    Object selectedSizesObj = request.getAttribute("selectedSizes");
    if (selectedSizesObj instanceof List<?>) {
        for (Object s : (List<?>) selectedSizesObj) {
            if (s != null) selectedSizes.add(s.toString());
        }
    }

    List<Category> categories = new ArrayList<>();
    Object categoriesObj = request.getAttribute("categories");
    if (categoriesObj instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<Category> temp = (List<Category>) categoriesObj;
        categories = temp;
    }
%>

<nav aria-label="Product categories">
    <div class="category-pills">
        <%
            StringBuilder base = new StringBuilder();
            if (!searchVal.isBlank()) base.append("&search=").append(URLEncoder.encode(searchVal, "UTF-8"));
            if (!minPriceVal.isBlank()) base.append("&minPrice=").append(URLEncoder.encode(minPriceVal, "UTF-8"));
            if (!maxPriceVal.isBlank()) base.append("&maxPrice=").append(URLEncoder.encode(maxPriceVal, "UTF-8"));
            if (!brandVal.isBlank()) base.append("&brand=").append(URLEncoder.encode(brandVal, "UTF-8"));
            if (!sortByVal.isBlank()) base.append("&sortBy=").append(URLEncoder.encode(sortByVal, "UTF-8"));
            for (String s : selectedSizes) base.append("&size=").append(URLEncoder.encode(s, "UTF-8"));
        %>

        <a href="<%= request.getContextPath() %>/products?<%= base.length() > 0 ? base.substring(1) : "" %>"
           class="pill <%= (currentCategoryId == null && currentTag.isBlank()) ? "active" : "" %>">All</a>

        <a href="<%= request.getContextPath() %>/products?tag=deals<%= base.toString() %>"
           class="pill <%= "deals".equalsIgnoreCase(currentTag) || "sale".equalsIgnoreCase(currentTag) ? "active" : "" %>">Deals</a>

        <% for (Category c : categories) { %>
            <a href="<%= request.getContextPath() %>/products?category=<%= URLEncoder.encode(c.getCategorySlug(), "UTF-8") %><%= base.toString() %>"
               class="pill <%= (currentCategoryId != null && currentCategoryId.intValue() == c.getCategoryId()) ? "active" : "" %>"><%= c.getCategoryName() %></a>
        <% } %>
    </div>
</nav>

<section class="catalog-header container">
    <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="<%= request.getContextPath() %>/home">Home</a>
        <span>/</span>
        <a href="<%= request.getContextPath() %>/products">Catalog</a>
        <% if (!currentCategorySlug.isBlank()) { %>
            <span>/</span>
            <span><%= currentCategorySlug.substring(0, 1).toUpperCase() + currentCategorySlug.substring(1) %></span>
        <% } %>
    </nav>
    <div class="catalog-hero">
        <div>
            <span class="eyebrow">FashionStore Catalog</span>
            <h1>Shop the complete edit</h1>
            <p>Refined essentials, premium footwear, and polished accessories filtered by real category mapping.</p>
        </div>
        <form class="catalog-search" action="<%= request.getContextPath() %>/products" method="get">
            <input type="search" name="search" value="<%= searchVal %>" placeholder="Search products or brands" aria-label="Search products">
            <% if (currentCategoryId != null) { %><input type="hidden" name="category" value="<%= currentCategorySlug %>"><% } %>
            <button type="submit">Search</button>
        </form>
    </div>
</section>

<!-- Mobile filter overlay -->
<div class="filter-overlay" id="filter-overlay" onclick="closeFilterSidebar()"></div>

<div class="container">
    <div class="products-layout">

        <!-- Mobile Filter Toggle -->
        <button class="mobile-filter-toggle" onclick="openFilterSidebar()" aria-label="Open filters">
            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"/>
            </svg>
            Filters
        </button>

        <!-- SIDEBAR -->
        <aside class="filter-sidebar" id="filter-sidebar" aria-label="Product filters">
            <!-- Mobile close button -->
            <button class="filter-close-btn" onclick="closeFilterSidebar()" aria-label="Close filters">
                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
                Close Filters
            </button>

            <form action="<%= request.getContextPath() %>/products" method="get" aria-label="Filter products">
                <input type="hidden" name="search" value="<%= searchVal %>">
                <% if (currentCategoryId != null) { %>
                    <input type="hidden" name="category" value="<%= currentCategorySlug %>">
                <% } %>
                <% if (!currentTag.isBlank()) { %>
                    <input type="hidden" name="tag" value="<%= currentTag %>">
                <% } %>
                <% if (!sortByVal.isBlank()) { %>
                    <input type="hidden" name="sortBy" value="<%= sortByVal %>">
                <% } %>

                <!-- Price Range -->
                <div class="filter-group">
                    <h3 class="filter-title">Price Range</h3>
                    <div class="price-inputs" style="display: flex; gap: var(--space-2); align-items: center; margin-bottom: var(--space-3);">
                        <input type="number" name="minPrice" placeholder="Min" class="form-input" style="padding: var(--space-2);" value="<%= minPriceVal %>">
                        <span>-</span>
                        <input type="number" name="maxPrice" placeholder="Max" class="form-input" style="padding: var(--space-2);" value="<%= maxPriceVal %>">
                    </div>
                </div>

                <!-- Size -->
                <div class="filter-group">
                    <h3 class="filter-title">Size</h3>
                    <div class="checkbox-list">
                        <% for (String size : allSizes) { %>
                            <label class="form-checkbox">
                                <input type="checkbox" name="size" value="<%= size %>" <%= selectedSizes.contains(size) ? "checked" : "" %>>
                                <span><%= size %></span>
                            </label>
                        <% } %>
                    </div>
                </div>

                <button type="submit" class="btn btn-primary btn-block">Apply Filters</button>
                <a href="<%= request.getContextPath() %>/products" class="btn btn-secondary btn-block" style="margin-top: var(--space-2);">Clear All</a>
            </form>
        </aside>

        <!-- MAIN PRODUCT GRID -->
        <main class="products-main">
            <div class="catalog-toolbar">
                <div>
                    <strong><%= products.size() %></strong>
                    <span>styles shown</span>
                    <% if (!searchVal.isBlank()) { %><span class="catalog-query">for “<%= searchVal %>”</span><% } %>
                </div>
                <form action="<%= request.getContextPath() %>/products" method="get" class="sort-form">
                    <% if (!searchVal.isBlank()) { %><input type="hidden" name="search" value="<%= searchVal %>"><% } %>
                    <% if (currentCategoryId != null) { %><input type="hidden" name="category" value="<%= currentCategorySlug %>"><% } %>
                    <% if (!currentTag.isBlank()) { %><input type="hidden" name="tag" value="<%= currentTag %>"><% } %>
                    <% if (!minPriceVal.isBlank()) { %><input type="hidden" name="minPrice" value="<%= minPriceVal %>"><% } %>
                    <% if (!maxPriceVal.isBlank()) { %><input type="hidden" name="maxPrice" value="<%= maxPriceVal %>"><% } %>
                    <% for (String s : selectedSizes) { %><input type="hidden" name="size" value="<%= s %>"><% } %>
                    <label for="sortBy">Sort</label>
                    <select id="sortBy" name="sortBy" onchange="this.form.submit()">
                        <option value="" <%= sortByVal.isBlank() ? "selected" : "" %>>Newest</option>
                        <option value="popular" <%= "popular".equals(sortByVal) ? "selected" : "" %>>Trending</option>
                        <option value="price_asc" <%= "price_asc".equals(sortByVal) ? "selected" : "" %>>Price low to high</option>
                        <option value="price_desc" <%= "price_desc".equals(sortByVal) ? "selected" : "" %>>Price high to low</option>
                        <option value="name_asc" <%= "name_asc".equals(sortByVal) ? "selected" : "" %>>Name A-Z</option>
                    </select>
                </form>
            </div>
            <!-- Mobile filter toggle button -->
            <button class="filter-toggle-btn" id="filter-toggle-btn" onclick="openFilterSidebar()" aria-controls="filter-sidebar" aria-expanded="false">
                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 010 2H4a1 1 0 01-1-1zm3 6a1 1 0 011-1h10a1 1 0 010 2H7a1 1 0 01-1-1zm3 6a1 1 0 011-1h4a1 1 0 010 2h-4a1 1 0 01-1-1z"/>
                </svg>
                Filters
            </button>

            <div class="product-grid">
                <% if (!products.isEmpty()) { %>
                    <% for (int i = 0; i < products.size(); i++) {
                        Product p = products.get(i);
                        // Guard against divide-by-zero when discount_percent == 100 (schema allows up to 100).
                        double originalPrice = (p.getDiscountPercent() > 0 && p.getDiscountPercent() < 100)
                                ? p.getPrice() / (1 - (p.getDiscountPercent() / 100.0)) : 0;
                    %>
                        <article class="product-card">
                            <div class="product-card-image-wrapper">
                                <img class="product-card-image" src="<%= p.getImageUrl() %>" alt="<%= p.getProductName() %>">
                                
                                <!-- Product Badges -->
                                <div class="product-card-badges">
                                    <% if (p.isNew()) { %>
                                        <span class="product-card-badge new">New</span>
                                    <% } %>
                                    <% if (p.isSale()) { %>
                                        <span class="product-card-badge sale">Sale</span>
                                    <% } %>
                                    <% if (p.isTrending()) { %>
                                        <span class="product-card-badge">Trending</span>
                                    <% } %>
                                </div>
                                
                                <button class="product-card-wishlist" onclick="event.preventDefault(); FashionStore.toggleWishlist(<%= p.getProductId() %>, this)" aria-label="Add <%= p.getProductName() %> to wishlist">
                                    <svg class="wishlist-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
                                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
                                    </svg>
                                </button>
                            </div>
                            <div class="product-card-content">
                                <span class="product-card-brand"><%= p.getBrand() != null && !p.getBrand().isBlank() ? p.getBrand() : p.getCategoryName() %></span>
                                <h3 class="product-card-name"><%= p.getProductName() %></h3>
                                <% if (p.getCategoryName() != null) { %>
                                    <span class="product-card-category"><%= p.getCategoryName() %></span>
                                <% } %>
                                <div class="product-card-bottom">
                                    <div class="product-card-price">
                                        <span class="product-card-price-current">₹<%= String.format("%.2f", p.getPrice()) %></span>
                                        <% if (originalPrice > p.getPrice()) { %>
                                            <span class="product-card-price-original">₹<%= String.format("%.2f", originalPrice) %></span>
                                        <% } %>
                                    </div>
                                    <% if (p.getSizes() != null && !p.getSizes().isEmpty()) { %>
                                        <div class="product-card-sizes" aria-label="Available sizes">
                                            <%
                                                int renderedSizeCount = 0;
                                                for (com.fashionstore.model.ProductSize size : p.getSizes()) {
                                                    if (renderedSizeCount >= 4) break;
                                            %>
                                                <span class="product-card-size <%= size.getStockQuantity() <= 0 ? "out-of-stock" : "" %>"><%= size.getSizeLabel() %></span>
                                            <%
                                                    renderedSizeCount++;
                                                }
                                            %>
                                        </div>
                                    <% } %>
                                    <div class="product-card-actions">
                                        <a href="<%= request.getContextPath() %>/product?id=<%= p.getProductId() %>" class="btn btn-primary product-card-add-btn">View Details</a>
                                        <button class="btn btn-outline product-card-add-btn" onclick="event.preventDefault(); FashionStore.addToCart(<%= p.getProductId() %>)" aria-label="Add <%= p.getProductName() %> to cart">
                                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
                                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"></path>
                                            </svg>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </article>
                    <% } %>
                <% } else { %>
                    <div class="empty-state">
                        <svg class="empty-state-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                        </svg>
                        <h3 class="empty-state-title">No products found</h3>
                        <p class="empty-state-description">We couldn't find any products matching your criteria. Try adjusting your filters or search terms.</p>
                        <div class="empty-state-action">
                            <a href="<%= request.getContextPath() %>/products" class="btn btn-primary">Clear Filters</a>
                            <a href="<%= request.getContextPath() %>/products" class="btn btn-outline" style="margin-left: var(--space-2);">View All Products</a>
                        </div>
                    </div>
                <% } %>
            </div>
            
            <!-- Pagination Controls -->
            <% if (request.getAttribute("totalPages") != null && (Integer)request.getAttribute("totalPages") > 1) { %>
                <div class="pagination-container">
                    <div class="pagination">
                        <% 
                            int currentPage = (Integer)request.getAttribute("currentPage");
                            int totalPages = (Integer)request.getAttribute("totalPages");
                            String search = (String)request.getAttribute("search");
                            String minPrice = (String)request.getAttribute("minPrice");
                            String maxPrice = (String)request.getAttribute("maxPrice");
                            String sortBy = (String)request.getAttribute("sortBy");
                            String brand = (String)request.getAttribute("brand");
                            Integer categoryId = (Integer)request.getAttribute("categoryId");
                            String tag = (String)request.getAttribute("tag");
                            Object selectedSizesObj2 = request.getAttribute("selectedSizes");
                            List<String> selectedSizes2 = new ArrayList<>();
                            if (selectedSizesObj2 instanceof List<?>) {
                                for (Object s : (List<?>) selectedSizesObj2) {
                                    if (s != null) selectedSizes2.add(s.toString());
                                }
                            }
                            
                            // Build query string for preserving filters
                            StringBuilder queryParams = new StringBuilder();
                            if (search != null && !search.isEmpty()) {
                                queryParams.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"));
                            }
                            if (categoryId != null) {
                                String categorySlug = (String)request.getAttribute("categorySlug");
                                if (categorySlug != null && !categorySlug.isEmpty()) {
                                    queryParams.append("&category=").append(java.net.URLEncoder.encode(categorySlug, "UTF-8"));
                                } else {
                                    queryParams.append("&categoryId=").append(categoryId);
                                }
                            }
                            if (tag != null && !tag.isEmpty()) {
                                queryParams.append("&tag=").append(java.net.URLEncoder.encode(tag, "UTF-8"));
                            }
                            if (minPrice != null && !minPrice.isEmpty()) {
                                queryParams.append("&minPrice=").append(java.net.URLEncoder.encode(minPrice, "UTF-8"));
                            }
                            if (maxPrice != null && !maxPrice.isEmpty()) {
                                queryParams.append("&maxPrice=").append(maxPrice);
                            }
                            if (brand != null && !brand.isEmpty()) {
                                queryParams.append("&brand=").append(java.net.URLEncoder.encode(brand, "UTF-8"));
                            }
                            if (sortBy != null && !sortBy.isEmpty()) {
                                queryParams.append("&sortBy=").append(sortBy);
                            }
                            for (String s : selectedSizes2) {
                                queryParams.append("&size=").append(java.net.URLEncoder.encode(s, "UTF-8"));
                            }
                        %>
                        
                        <!-- Previous Button -->
                        <% if (currentPage > 1) { %>
                            <a href="<%= request.getContextPath() %>/products?page=<%= currentPage - 1 %><%= queryParams.toString() %>" 
                               class="pagination-link" aria-label="Previous page">
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                                </svg>
                                Prev
                            </a>
                        <% } else { %>
                            <span class="pagination-link disabled">
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                                </svg>
                                Prev
                            </span>
                        <% } %>
                        
                        <!-- Page Numbers -->
                        <% for (int i = 1; i <= totalPages; i++) { 
                            if (i == currentPage) { %>
                                <span class="pagination-link active" aria-current="page"><%= i %></span>
                            <% } else if (i == 1 || i == totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) { %>
                                <a href="<%= request.getContextPath() %>/products?page=<%= i %><%= queryParams.toString() %>" 
                                   class="pagination-link"><%= i %></a>
                            <% } else if (i == currentPage - 2 || i == currentPage + 2) { %>
                                <span class="pagination-ellipsis">...</span>
                            <% } 
                        } %>
                        
                        <!-- Next Button -->
                        <% if (currentPage < totalPages) { %>
                            <a href="<%= request.getContextPath() %>/products?page=<%= currentPage + 1 %><%= queryParams.toString() %>" 
                               class="pagination-link" aria-label="Next page">
                                Next
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                                </svg>
                            </a>
                        <% } else { %>
                            <span class="pagination-link disabled">
                                Next
                                <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
                                </svg>
                            </span>
                        <% } %>
                    </div>
                </div>
            <% } %>
        </main>

    </div>
</div>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<!-- Product Quick View Modal -->
<div class="modal-overlay" id="quickViewModal" aria-hidden="true" role="dialog" aria-modal="true">
    <div class="modal-content">
        <button class="modal-close" onclick="FashionStore.closeQuickView()" aria-label="Close modal">
            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
        </button>
        <div class="modal-body" id="modalContent">
            <!-- Content loaded dynamically via AJAX -->
        </div>
    </div>
</div>

<script>
function openFilterSidebar() {
    const sidebar = document.getElementById('filter-sidebar');
    const overlay = document.getElementById('filter-overlay');
    const btn = document.getElementById('filter-toggle-btn');
    sidebar.classList.add('active');
    overlay.classList.add('active');
    if (btn) btn.setAttribute('aria-expanded', 'true');
    document.body.style.overflow = 'hidden';
}

function closeFilterSidebar() {
    const sidebar = document.getElementById('filter-sidebar');
    const overlay = document.getElementById('filter-overlay');
    const btn = document.getElementById('filter-toggle-btn');
    sidebar.classList.remove('active');
    overlay.classList.remove('active');
    if (btn) btn.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = '';
}

</script>

</body>
</html>
