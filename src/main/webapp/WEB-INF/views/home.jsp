<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="com.fashionstore.model.Product" %>
<%@ page import="com.fashionstore.model.Category" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Home");
    request.setAttribute("_pageCSS", "home");
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

    List<Category> categories = new ArrayList<>();
    Object categoriesObj = request.getAttribute("categories");
    if (categoriesObj instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<Category> temp = (List<Category>) categoriesObj;
        categories = temp;
    }
%>

<main class="home-page">
    <section class="hero">
        <div class="hero-overlay"></div>
        <div class="container">
            <div class="hero-content">
                <span class="hero-tag">Spring / Summer Collection 2026</span>
                <h1 class="hero-title">Elevate Your Everyday Aesthetic</h1>
                <p class="hero-subtitle">Discover our curated selection of premium essentials, designed for the modern individual who values both style and comfort.</p>
                <div class="hero-actions">
                    <a href="<%= request.getContextPath() %>/products" class="btn btn-primary">Shop Collection</a>
                    <a href="<%= request.getContextPath() %>/products?tag=deals" class="btn btn-secondary">Explore Deals</a>
                </div>
            </div>
        </div>
    </section>

    <% if (!categories.isEmpty()) { %>
    <section class="container home-categories">
        <div class="category-pills home-category-pills">
            <% for (Category c : categories) { %>
                <a class="pill" href="<%= request.getContextPath() %>/products?category=<%= java.net.URLEncoder.encode(c.getCategorySlug(), "UTF-8") %>"><%= c.getCategoryName() %></a>
            <% } %>
        </div>
    </section>
    <% } %>

    <section class="container featured-section">
        <div class="section-head">
            <h2 class="display-3">Featured Products</h2>
            <a class="btn btn-outline" href="<%= request.getContextPath() %>/products">View all</a>
        </div>

        <div class="product-grid">
            <% if (!products.isEmpty()) { %>
                <% for (Product p : products) { %>
                    <article class="product-card">
                        <div class="product-card-image-wrapper">
                            <img class="product-card-image" src="<%= p.getImageUrl() %>" alt="<%= p.getProductName() %>">
                            <button class="product-card-wishlist" data-product-id="<%= p.getProductId() %>" onclick="FashionStore.toggleWishlist(<%= p.getProductId() %>, this)" aria-label="Add to wishlist">
                                <svg class="wishlist-icon" width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                                </svg>
                            </button>
                        </div>
                        <div class="product-card-content">
                            <span class="product-card-brand"><%= p.getBrand() != null && !p.getBrand().isBlank() ? p.getBrand() : p.getCategoryName() %></span>
                            <h3 class="product-card-name"><%= p.getProductName() %></h3>
                            <% if (p.getCategoryName() != null) { %>
                                <span class="product-card-category"><%= p.getCategoryName() %></span>
                            <% } %>
                            <div class="product-card-price">
                                <span class="product-card-price-current">₹<%= String.format("%.2f", p.getPrice()) %></span>
                            </div>
                            <div class="product-card-actions">
                                <a href="<%= request.getContextPath() %>/product?id=<%= p.getProductId() %>" class="btn btn-primary product-card-add-btn">View Details</a>
                            </div>
                        </div>
                    </article>
                <% } %>
            <% } else { %>
                <div class="empty-state">
                    <svg class="empty-state-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
                    </svg>
                    <h3 class="empty-state-title">No products available yet</h3>
                    <p class="empty-state-description">No products are available at the moment. Please check back soon.</p>
                    <div class="empty-state-action">
                        <a href="<%= request.getContextPath() %>/products" class="btn btn-primary">Go to Products</a>
                    </div>
                </div>
            <% } %>
        </div>
    </section>

    <section class="section">
        <div class="container">
            <div class="value-strip card-surface">
                <div class="value-item">
                    <h4 class="display-3">Premium Materials</h4>
                    <p class="text-secondary">Curated fabrics and build quality designed for everyday luxury.</p>
                </div>
                <div class="value-item">
                    <h4 class="display-3">Fast Delivery</h4>
                    <p class="text-secondary">Quick dispatch and real-time order tracking across major cities.</p>
                </div>
                <div class="value-item">
                    <h4 class="display-3">Secure Payments</h4>
                    <p class="text-secondary">Trusted checkout with encrypted transactions and reliable support.</p>
                </div>
            </div>
        </div>
    </section>

    <section class="section">
        <div class="container">
            <div class="section-head">
                <div>
                    <h2 class="display-3">Why customers stay</h2>
                    <p class="section-copy text-secondary">Built like a real brand experience, not a basic catalog.</p>
                </div>
            </div>
            <div class="trust-grid">
                <article class="trust-card card-surface">
                    <h4 class="display-3">Style-led Catalog</h4>
                    <p class="text-secondary">Collections are structured for discovery, helping customers browse by intent and season.</p>
                </article>
                <article class="trust-card card-surface">
                    <h4 class="display-3">Conversion-focused UX</h4>
                    <p class="text-secondary">Clear CTAs, polished cards, and frictionless flows increase add-to-cart confidence.</p>
                </article>
                <article class="trust-card card-surface">
                    <h4 class="display-3">Scalable Design System</h4>
                    <p class="text-secondary">Tokenized spacing, typography, and components keep every new page consistent.</p>
                </article>
            </div>
        </div>
    </section>
</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<script>
// Wishlist button event delegation
document.addEventListener('DOMContentLoaded', function() {
    document.addEventListener('click', function(e) {
        const wishlistBtn = e.target.closest('.product-card-wishlist');
        if (wishlistBtn && FashionStore && FashionStore.toggleWishlist) {
            const productId = wishlistBtn.getAttribute('data-product-id');
            if (productId) {
                FashionStore.toggleWishlist(productId, wishlistBtn);
            }
        }
    });
});
</script>

</body>
</html>
