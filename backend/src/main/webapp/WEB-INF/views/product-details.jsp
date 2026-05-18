<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.fashionstore.model.Product" %>

<%--
PRODUCT DETAILS PAGE - MVC ARCHITECTURE

REFACTORED FOR PROPER MVC:
- Backend (ProductController) loads product by ID
- Backend provides product data via request attributes
- JSP displays backend-provided product (NO calculations)
- JavaScript only handles UI interactions (gallery, reviews, cart)
- NO frontend price calculations
- NO frontend stock manipulations
- NO frontend discount calculations
- Backend is single source of truth

Data Flow:
1. Frontend requests product by ID
2. ProductController loads product from ProductService
3. ProductService retrieves from ProductDAO
4. JSP displays backend product data
5. JavaScript handles UI interactions only

Price Handling:
- Price from backend database
- Discount from backend database
- Discounted price calculated on backend
- JSP displays backend-calculated prices
- Frontend cannot modify prices

Stock Handling:
- Stock level from backend database
- Stock validation on backend
- JSP displays backend stock level
- Frontend cannot modify stock
--%>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    Product product = null;
    Object productObj = request.getAttribute("product");
    if (productObj instanceof Product) {
        product = (Product) productObj;
    }
    request.setAttribute("_pageTitle", product != null ? product.getProductName() : "Product Details");
    request.setAttribute("_pageCSS", "product-details");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
<script src="<%= request.getContextPath() %>/assets/js/product-gallery.js"></script>
<script src="<%= request.getContextPath() %>/assets/js/modules/product-reviews.js"></script>
</head>

<body>

<!-- NAVBAR -->
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />



<main class="shell section-block" id="main-content">

    <% if (product != null) { %>
    <div class="product-detail">
        <div class="product-gallery">
            <div class="product-gallery__main" id="mainImageContainer">
                <img data-src="<%= product.getImageUrl() %>" alt="<%= product.getProductName() %>" id="mainImage" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;" loading="eager" class="lazy-load">
                <div class="product-gallery__zoom-lens"></div>
                <div class="product-gallery__swipe-hint">← Swipe to see more →</div>
            </div>
            <div class="product-gallery__thumbnails" aria-label="Product gallery">
                <button type="button" class="product-gallery__thumb active" data-image="<%= product.getImageUrl() %>" onclick="ProductGallery.changeImage(this)">
                    <img src="<%= product.getImageUrl() %>" alt="Thumbnail 1" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;">
                </button>
                <button type="button" class="product-gallery__thumb" data-image="<%= product.getImageUrl() %>" onclick="ProductGallery.changeImage(this)">
                    <img src="<%= product.getImageUrl() %>" alt="Thumbnail 2" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;">
                </button>
                <button type="button" class="product-gallery__thumb" data-image="<%= product.getImageUrl() %>" onclick="ProductGallery.changeImage(this)">
                    <img src="<%= product.getImageUrl() %>" alt="Thumbnail 3" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;">
                </button>
                <button type="button" class="product-gallery__thumb" data-image="<%= product.getImageUrl() %>" onclick="ProductGallery.changeImage(this)">
                    <img src="<%= product.getImageUrl() %>" alt="Thumbnail 4" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;">
                </button>
            </div>
        </div>

        <div class="product-info">
            <div class="stack-sm">
                <span class="product-info__eyebrow"><%= product.getBrand() != null && !product.getBrand().isBlank() ? org.apache.commons.text.StringEscapeUtils.escapeHtml4(product.getBrand()) : "FashionStore" %></span>
                <% if (product.getCategoryName() != null) { %><span class="product-info__category"><%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(product.getCategoryName()) %></span><% } %>
            </div>

            <h1 class="product-info__title"><%= product.getProductName() %></h1>

            <%
                Object avgRatingObj = request.getAttribute("avgRating");
                Object reviewCountObj = request.getAttribute("reviewCount");
                Double avgRatingTop = (avgRatingObj instanceof Double) ? (Double) avgRatingObj : 0.0;
                Integer reviewCountTop = (reviewCountObj instanceof Integer) ? (Integer) reviewCountObj : 0;
                if (reviewCountTop != null && reviewCountTop > 0) {
            %>
                <div class="product-info__rating">
                    <span>★ <%= String.format("%.1f", avgRatingTop) %></span>
                    <a href="#reviews-section" onclick="document.querySelector('.reviews-section').scrollIntoView({behavior:'smooth'}); return false;">
                        (<%= reviewCountTop %> reviews)
                    </a>
                </div>
            <% } %>

            <div class="product-info__price">
                <span class="product-info__price-current">₹<%= String.format("%.2f", product.getPrice()) %></span>
                <% if (product.getDiscountPercent() > 0) { %>
                    <span class="product-info__discount"><%= product.getDiscountPercent() %>% OFF</span>
                <% } %>
            </div>

            <p class="product-info__description"><%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(product.getDescription()) %></p>

            <div class="product-info__stock <%= product.getStockQuantity() > 0 ? "product-info__stock--in" : "product-info__stock--out" %>">
                <%= product.getStockQuantity() > 0 ? product.getStockQuantity() + " pieces available" : "Currently unavailable" %>
            </div>

            <div id="addToCartForm" class="stack-lg">
                <input type="hidden" id="detailsProductId" value="<%= product.getProductId() %>">

                <div class="size-selector">
                    <div class="size-selector__label">Select Size</div>
                    <div class="size-selector__hint">Choose an available size to add this item to cart.</div>
                    <div class="size-selector__list">
                    <% 
                        if (product.getSizes() != null && !product.getSizes().isEmpty()) {
                            for (com.fashionstore.model.ProductSize size : product.getSizes()) { 
                                if (size.getStockQuantity() > 0) {
                    %>
                        <label class="size-selector__option" onclick="this.querySelector('input').checked = true; ProductGallery.updateSizeSelection()">
                            <input type="radio" name="size" value="<%= size.getSizeLabel() %>" required> 
                            <span><%= size.getSizeLabel() %></span>
                            <span class="text-xs text-tertiary">(<%= size.getStockQuantity() %> left)</span>
                        </label>
                    <% 
                            } else {
                    %>
                        <label class="size-selector__option size-selector__option--disabled">
                            <input type="radio" name="size" value="<%= size.getSizeLabel() %>" disabled> 
                            <span><%= size.getSizeLabel() %></span>
                            <span class="text-xs text-tertiary">(Out of stock)</span>
                        </label>
                    <%
                            }
                        }
                    } else {
                    %>
                        <p class="text-sm text-tertiary">No sizes available for this product.</p>
                    <% } %>
                    </div>
                </div>

                <div class="size-selector">
                    <div class="size-selector__label">Quantity</div>
                    <div class="quantity-stepper" aria-label="Quantity selector">
                        <button type="button" onclick="ProductGallery.adjustQuantity(-1)" aria-label="Decrease quantity">−</button>
                        <input type="number" id="detailsQuantity" value="1" min="1" max="10" aria-label="Quantity">
                        <button type="button" onclick="ProductGallery.adjustQuantity(1)" aria-label="Increase quantity">+</button>
                    </div>
                </div>

                <div class="product-highlights">
                    <div class="product-highlight">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 8l4 4m0 0l4-4m-4 4v12m7-12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                        <div><strong>Free delivery</strong><span>On all prepaid orders</span></div>
                    </div>
                    <div class="product-highlight">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                        </svg>
                        <div><strong>Easy returns</strong><span>7-day return window</span></div>
                    </div>
                    <div class="product-highlight">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                        </svg>
                        <div><strong>Secure checkout</strong><span>Encrypted payment flow</span></div>
                    </div>
                </div>

                <div class="product-actions">
                    <button class="fs-btn fs-btn--primary product-actions__primary" onclick="ProductGallery.addToCart()">Add to Cart</button>
                    <button class="fs-btn fs-btn--outline product-actions__secondary" onclick="FashionStore.toggleWishlist('<%= org.apache.commons.text.StringEscapeUtils.escapeEcmaScript(String.valueOf(product.getProductId())) %>', this)" aria-label="Add to wishlist">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
                        </svg>
                        Wishlist
                    </button>
                </div>
                <a href="<%= request.getContextPath() %>/products" class="inline-link">Continue Shopping</a>
            </div>
        </div>
    </div>
    <% } else { %>
    <div class="fs-empty-state">
        <h3>Product not found</h3>
        <a href="<%= request.getContextPath() %>/products" class="fs-btn fs-btn--primary">Back to Products</a>
    </div>
    <% } %>

    <% if (product != null) { %>
    <div class="mobile-cta-bar">
        <div class="mobile-cta-bar__content">
            <div class="mobile-cta-bar__price">
                <span class="mobile-cta-bar__price-current">₹<%= String.format("%.2f", product.getPrice()) %></span>
            </div>
            <button class="fs-btn fs-btn--primary mobile-cta-bar__btn" onclick="ProductGallery.addToCart()">Add to Cart</button>
        </div>
    </div>
    <% } %>

</main>

<% if (product != null) { %>
<section class="shell section-block">
    <div class="accordion">
        <details open class="accordion-item">
            <summary class="accordion-header">Product Details</summary>
            <div class="accordion-body"><%= product.getDescription() %></div>
        </details>
        <details class="accordion-item">
            <summary class="accordion-header">Shipping & Delivery</summary>
            <div class="accordion-body">Orders are packed within 24 hours. Standard delivery usually arrives within 3 to 6 business days depending on destination.</div>
        </details>
        <details class="accordion-item">
            <summary class="accordion-header">Returns & Care</summary>
            <div class="accordion-body">Return eligible products within 7 days in unused condition with original tags. Follow garment care instructions for best longevity.</div>
        </details>
        <details class="accordion-item">
            <summary class="accordion-header">Specifications</summary>
            <div class="accordion-body">
                <ul>
                    <li>Brand: <%= product.getBrand() != null ? product.getBrand() : "FashionStore" %></li>
                    <li>Category: <%= product.getCategoryName() != null ? product.getCategoryName() : "Catalog" %></li>
                    <li>Availability: <%= product.getStockQuantity() > 0 ? "In stock" : "Out of stock" %></li>
                </ul>
            </div>
        </details>
    </div>
</section>

<%
    java.util.List<Product> relatedProducts = new java.util.ArrayList<>();
    Object relatedProductsObj = request.getAttribute("relatedProducts");
    if (relatedProductsObj instanceof java.util.List<?>) {
        @SuppressWarnings("unchecked")
        java.util.List<Product> tempRelatedProducts = (java.util.List<Product>) relatedProductsObj;
        relatedProducts = tempRelatedProducts;
    }
%>
<% if (!relatedProducts.isEmpty()) { %>
<section class="shell section-block">
    <div class="section-header">
        <h2 class="editorial-heading">Related Products</h2>
        <a href="<%= request.getContextPath() %>/products?category=<%= product.getCategorySlug() %>" class="inline-link">View Category</a>
    </div>
    <div class="product-card-grid">
        <% for (Product related : relatedProducts) { %>
            <article class="product-card">
                <div class="product-card__media">
                    <a href="<%= request.getContextPath() %>/product?id=<%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(String.valueOf(related.getProductId())) %>">
                        <img data-src="<%= related.getImageUrl() %>" alt="<%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(related.getProductName()) %>" loading="lazy" class="lazy-load" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;">
                    </a>
                    <button class="product-card__wishlist" data-product-id="<%= related.getProductId() %>" onclick="FashionStore.productInteractions.toggleWishlist('<%= org.apache.commons.text.StringEscapeUtils.escapeEcmaScript(String.valueOf(related.getProductId())) %>', this)" aria-label="Add to wishlist">
                        <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                        </svg>
                    </button>
                </div>
                <div class="product-card__body">
                    <span class="product-card__eyebrow"><%= related.getBrand() != null && !related.getBrand().isBlank() ? org.apache.commons.text.StringEscapeUtils.escapeHtml4(related.getBrand()) : org.apache.commons.text.StringEscapeUtils.escapeHtml4(related.getCategoryName()) %></span>
                    <a href="<%= request.getContextPath() %>/product?id=<%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(String.valueOf(related.getProductId())) %>" class="product-card__title"><%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(related.getProductName()) %></a>
                    <div class="product-card__footer">
                        <p class="product-card__price">₹<%= String.format("%.2f", related.getPrice()) %></p>
                    </div>
                    <div class="product-card__actions">
                        <a href="<%= request.getContextPath() %>/product?id=<%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(String.valueOf(related.getProductId())) %>" class="product-card__btn product-card__btn--primary">View Details</a>
                    </div>
                </div>
            </article>
        <% } %>
    </div>
</section>
<% } %>

<section class="shell section-block fs-reviews-section">
    <h2 class="editorial-heading">Customer Reviews</h2>

    <div class="fs-reviews">
        <div class="fs-reviews__list">
            <%
                java.util.List<com.fashionstore.model.Review> reviews = new java.util.ArrayList<>();
                Object reviewsObj = request.getAttribute("reviews");
                if (reviewsObj instanceof java.util.List<?>) {
                    @SuppressWarnings("unchecked")
                    java.util.List<com.fashionstore.model.Review> temp = (java.util.List<com.fashionstore.model.Review>) reviewsObj;
                    reviews = temp;
                }
                if (reviews != null && !reviews.isEmpty()) {
                    for (com.fashionstore.model.Review r : reviews) {
            %>
                <div class="fs-review-card">
                    <div class="fs-review-card__header">
                        <span class="fs-review-card__author"><%= r.getUserName() %></span>
                        <span class="fs-review-card__stars">
                            <% for(int i=0; i<r.getRating(); i++) { %>★<% } %><% for(int i=r.getRating(); i<5; i++) { %>☆<% } %>
                        </span>
                    </div>
                    <p class="fs-review-card__text"><%= r.getComment() %></p>
                    <small class="fs-review-card__date"><%= new java.text.SimpleDateFormat("MMM dd, yyyy").format(r.getCreatedAt()) %></small>
                </div>
            <%
                    }
                } else {
            %>
                <p class="fs-reviews__empty">No reviews yet. Be the first to review this product!</p>
            <% } %>
        </div>

        <div class="fs-review-form">
            <h3>Write a Review</h3>
            <% if (session.getAttribute("customerAuth") != null) { %>
                <form id="reviewForm" onsubmit="return submitReview(event, '<%= org.apache.commons.text.StringEscapeUtils.escapeEcmaScript(String.valueOf(product.getProductId())) %>')">
                    <div class="fs-form-group">
                        <label for="reviewRating">Rating</label>
                        <select name="rating" id="reviewRating" required class="fs-form-select">
                            <option value="5">5 - Excellent</option>
                            <option value="4">4 - Good</option>
                            <option value="3">3 - Average</option>
                            <option value="2">2 - Poor</option>
                            <option value="1">1 - Terrible</option>
                        </select>
                    </div>
                    <div class="fs-form-group">
                        <label for="reviewComment">Review</label>
                        <textarea name="comment" id="reviewComment" rows="4" required placeholder="What did you think about this product?" class="fs-form-textarea"></textarea>
                    </div>
                    <button type="submit" class="fs-btn fs-btn--primary">Submit Review</button>
                </form>
            <% } else { %>
                <p class="text-secondary">Please log in to write a review.</p>
                <a href="<%= request.getContextPath() %>/login" class="fs-btn fs-btn--primary">Log In</a>
            <% } %>
        </div>
    </div>
</section>
<% } %>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
