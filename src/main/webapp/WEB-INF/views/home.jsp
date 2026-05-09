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
    <section class="hero" data-reveal>
        <div class="hero-gradient-orb" data-parallax="0.15"></div>
        <div class="container">
            <div class="hero-content">
                <span class="hero-badge">New Collection &middot; Spring / Summer 2026</span>
                <h1 class="hero-title">
                    <span>Luxury Everyday</span>
                    <span>Essentials</span>
                </h1>
                <p class="hero-subtitle">A cinematic edit of tailored separates, sculptural accessories, and city-ready footwear &mdash; engineered for the modern wardrobe.</p>
                <div class="hero-actions">
                    <a href="<%= request.getContextPath() %>/products?tag=new" class="btn btn-primary">Explore Collection</a>
                    <a href="<%= request.getContextPath() %>/products?tag=trending" class="btn btn-secondary">Discover the Edit</a>
                </div>
            </div>
        </div>
    </section>

    <% if (!categories.isEmpty()) { %>
    <section class="section-spacer-lg">
        <div class="container">
            <span class="section-label">Shop by Category</span>
            <h2 class="editorial-headline">Curated Collections</h2>
            <div class="category-grid" data-stagger data-stagger-delay="100">
            <% for (Category c : categories) { %>
                <a class="category-tile category-<%= c.getCategorySlug() %>" href="<%= request.getContextPath() %>/products?category=<%= java.net.URLEncoder.encode(c.getCategorySlug(), "UTF-8") %>">
                    <span><%= c.getCategoryName() %></span>
                    <small>Explore edit</small>
                </a>
            <% } %>
            <a class="category-tile category-new" href="<%= request.getContextPath() %>/products?tag=new"><span>New Arrivals</span><small>Latest drop</small></a>
            <a class="category-tile category-sale" href="<%= request.getContextPath() %>/products?tag=deals"><span>Sale</span><small>Limited offers</small></a>
        </div>
    </section>
    <% } %>

    <section class="container editorial-band reveal-on-scroll">
        <div class="editorial-copy">
            <span class="home-section-label">The Edit</span>
            <h2>Crafted for modern movement.</h2>
            <p>Quiet luxury, sculpted lines, and considered detailing &mdash; tailored separates, relaxed textures, polished footwear, and accessories that carry from morning commute to evening plans.</p>
            <a href="<%= request.getContextPath() %>/products" class="btn btn-outline">Build a look</a>
        </div>
        <div class="editorial-image editorial-image-primary" aria-hidden="true"></div>
        <div class="editorial-image editorial-image-secondary" aria-hidden="true"></div>
    </section>

    <section class="container editorial-band editorial-band-reverse reveal-on-scroll">
        <div class="editorial-image editorial-image-urban" aria-hidden="true"></div>
        <div class="editorial-copy editorial-copy-light">
            <span class="home-section-label">Story 02</span>
            <h2>Urban Essentials &mdash; Street Luxury.</h2>
            <p>Minimal tailoring meets relaxed silhouettes. Layered neutrals, structured outerwear, and accessories built for unscripted city days.</p>
            <a href="<%= request.getContextPath() %>/products?category=men" class="btn btn-outline">Shop the story</a>
        </div>
        <div class="editorial-image editorial-image-tailoring" aria-hidden="true"></div>
    </section>

    <section class="container featured-section reveal-on-scroll">
        <div class="section-head">
            <div>
                <span class="home-section-label">Curated edit</span>
                <h2 class="display-3">Featured Products</h2>
            </div>
            <a class="btn btn-outline" href="<%= request.getContextPath() %>/products">View all</a>
        </div>

        <div class="product-grid">
            <% if (!products.isEmpty()) { %>
                <% for (Product p : products) { %>
                    <article class="product-card">
                        <div class="product-card-image-wrapper">
                            <img class="product-card-image" src="<%= p.getImageUrl() %>" alt="<%= p.getProductName() %>" loading="lazy">
                            <button class="product-card-wishlist" data-product-id="<%= p.getProductId() %>" onclick="FashionStore.toggleWishlist(<%= p.getProductId() %>, this)" aria-label="Add to wishlist">
                                <svg class="wishlist-icon" width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                                </svg>
                            </button>
                            <% if (p.isSale()) { %>
                                <span class="product-card-badge badge-sale">Sale</span>
                            <% } else if (p.isNew()) { %>
                                <span class="product-card-badge badge-new">New</span>
                            <% } %>
                        </div>
                        <div class="product-card-content">
                            <span class="product-card-brand"><%= p.getBrand() != null && !p.getBrand().isBlank() ? p.getBrand() : p.getCategoryName() %></span>
                            <h3 class="product-card-name"><%= p.getProductName() %></h3>
                            <% if (p.getCategoryName() != null) { %>
                                <span class="product-card-category"><%= p.getCategoryName() %></span>
                            <% } %>
                            <div class="product-card-bottom">
                                <div class="product-card-price">
                                    <% if (p.getDiscountPercent() > 0) { %>
                                        <span class="product-card-price-current">₹<%= String.format("%.2f", p.getPrice() * (1 - p.getDiscountPercent() / 100)) %></span>
                                        <span class="product-card-price-original">₹<%= String.format("%.2f", p.getPrice()) %></span>
                                    <% } else { %>
                                        <span class="product-card-price-current">₹<%= String.format("%.2f", p.getPrice()) %></span>
                                    <% } %>
                                </div>
                                <div class="product-card-actions">
                                    <a href="<%= request.getContextPath() %>/product?id=<%= p.getProductId() %>" class="btn btn-primary product-card-add-btn">View Details</a>
                                </div>
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
                    <p class="empty-state-description">We're curating our collection. Check back soon for new arrivals and exclusive pieces.</p>
                    <div class="empty-state-action">
                        <a href="<%= request.getContextPath() %>/products?tag=new" class="btn btn-primary">Shop New Arrivals</a>
                        <a href="<%= request.getContextPath() %>/products" class="btn btn-outline">Browse All</a>
                    </div>
                </div>
            <% } %>
        </div>
    </section>

    <section class="container campaign-grid reveal-on-scroll">
        <a class="campaign-card campaign-dark" href="<%= request.getContextPath() %>/products?category=women">
            <span>Women's Edit</span>
            <strong>Fluid tailoring and elevated separates</strong>
        </a>
        <a class="campaign-card campaign-light" href="<%= request.getContextPath() %>/products?category=men">
            <span>Men's Edit</span>
            <strong>Sharp layers for the city uniform</strong>
        </a>
        <a class="campaign-card campaign-accent" href="<%= request.getContextPath() %>/products?category=footwear">
            <span>Footwear</span>
            <strong>Sneakers, boots, and refined leather</strong>
        </a>
    </section>

    <section class="section reveal-on-scroll">
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

    <section class="section reveal-on-scroll">
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

    <section class="container social-proof reveal-on-scroll">
        <div>
            <span class="home-section-label">Seen in the city</span>
            <h2 class="display-3">Styled by the FashionStore community</h2>
        </div>
        <div class="social-grid" aria-label="Social fashion gallery">
            <div class="social-shot shot-1"></div>
            <div class="social-shot shot-2"></div>
            <div class="social-shot shot-3"></div>
            <div class="social-shot shot-4"></div>
        </div>
    </section>

    <section class="newsletter-section reveal-on-scroll">
        <div class="container newsletter-inner">
            <span class="home-section-label">Private list</span>
            <h2>First access to new drops and seasonal edits.</h2>
            <form class="newsletter-form" action="<%= request.getContextPath() %>/products" method="get">
                <input type="email" placeholder="Email address" aria-label="Email address">
                <button class="btn btn-primary" type="submit">Join</button>
            </form>
        </div>
    </section>
</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
