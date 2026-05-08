<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.fashionstore.model.Product" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    com.fashionstore.model.Product _pd = null;
    Object _pdObj = request.getAttribute("product");
    if (_pdObj instanceof com.fashionstore.model.Product) {
        _pd = (com.fashionstore.model.Product) _pdObj;
    }
    String _pdTitle = (_pd != null) ? _pd.getProductName() : "Product Details";
    request.setAttribute("_pageTitle", _pdTitle);
    request.setAttribute("_pageCSS", "product-details");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<!-- NAVBAR -->
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    Product product = null;
    Object productObj = request.getAttribute("product");
    if (productObj instanceof Product) {
        product = (Product) productObj;
    }
%>

<main class="details-container">

    <!-- IMAGE -->
    <% if (product != null) { %>
    <div class="image-section">
        <div class="product-gallery-main">
            <img src="<%= product.getImageUrl() %>" alt="<%= product.getProductName() %>">
        </div>
        <div class="product-gallery-thumbs" aria-label="Product gallery">
            <button type="button" class="gallery-thumb active"><img src="<%= product.getImageUrl() %>" alt=""></button>
            <button type="button" class="gallery-thumb"><img src="<%= product.getImageUrl() %>" alt=""></button>
            <button type="button" class="gallery-thumb"><img src="<%= product.getImageUrl() %>" alt=""></button>
        </div>
    </div>

    <!-- DETAILS -->
    <div class="details-section">

        <div class="details-kicker">
            <span><%= product.getBrand() != null && !product.getBrand().isBlank() ? product.getBrand() : "FashionStore" %></span>
            <% if (product.getCategoryName() != null) { %><span><%= product.getCategoryName() %></span><% } %>
        </div>

        <h2 class="product-title"><%= product.getProductName() %></h2>

        <%
            Object avgRatingObj = request.getAttribute("avgRating");
            Object reviewCountObj = request.getAttribute("reviewCount");
            Double avgRatingTop = (avgRatingObj instanceof Double) ? (Double) avgRatingObj : 0.0;
            Integer reviewCountTop = (reviewCountObj instanceof Integer) ? (Integer) reviewCountObj : 0;
            if (reviewCountTop != null && reviewCountTop > 0) {
        %>
            <div class="product-rating-row">
                <span class="product-avg-rating">★ <%= String.format("%.1f", avgRatingTop) %></span>
                <a href="#reviews-section" onclick="document.querySelector('.reviews-section').scrollIntoView({behavior:'smooth'}); return false;" class="product-review-count">
                    (<%= reviewCountTop %> reviews)
                </a>
            </div>
        <% } %>

        <div class="product-price">
            ₹<%= String.format("%.2f", product.getPrice()) %>
        </div>

        <div class="product-discount">
            <%= product.getDiscountPercent() %>% OFF
        </div>

        <div class="product-description">
            <%= product.getDescription() %>
        </div>

        <div class="stock-summary <%= product.getStockQuantity() > 0 ? "in-stock" : "out-of-stock" %>">
            <span></span>
            <%= product.getStockQuantity() > 0 ? product.getStockQuantity() + " pieces available" : "Currently unavailable" %>
        </div>

        <!-- ADD TO CART -->
        <div id="addToCartForm">
            <input type="hidden" id="detailsProductId" value="<%= product.getProductId() %>">

            <div class="size-select">
                <strong>Select Size</strong>
                <p class="select-help">Choose an available size to add this item to cart.</p>
                <div class="size-list">
                <% 
                    if (product.getSizes() != null && !product.getSizes().isEmpty()) {
                        for (com.fashionstore.model.ProductSize size : product.getSizes()) { 
                            if (size.getStockQuantity() > 0) {
                %>
                    <label class="size-option">
                        <input type="radio" name="size" value="<%= size.getSizeLabel() %>" required> 
                        <span class="size-pill"><%= size.getSizeLabel() %></span>
                        <span class="stock-info">(<%= size.getStockQuantity() %> left)</span>
                    </label>
                <% 
                            } else {
                %>
                    <label class="size-option out-of-stock">
                        <input type="radio" name="size" value="<%= size.getSizeLabel() %>" disabled> 
                        <span class="size-pill"><%= size.getSizeLabel() %></span>
                        <span class="stock-info">(Out of stock)</span>
                    </label>
                <%
                            }
                        }
                    } else {
                %>
                    <p class="no-sizes">No sizes available for this product.</p>
                <% } %>
                </div>
            </div>

            <div class="detail-actions">
                <button class="add-cart-btn btn btn-primary" onclick="submitProductDetailsCart()">
                    Add to Cart
                </button>
                <button class="wishlist-detail-btn" onclick="FashionStore.toggleWishlist(<%= product.getProductId() %>, this)" aria-label="Add to wishlist">
                    <svg class="wishlist-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
                    </svg>
                    Wishlist
                </button>
            </div>
            <a class="continue-link" href="<%= request.getContextPath() %>/products">Continue Shopping</a>
        </div>

        <script>
            function submitProductDetailsCart() {
                const productId = document.getElementById('detailsProductId').value;
                const sizeInput = document.querySelector('input[name="size"]:checked');
                if (!sizeInput && document.querySelector('input[name="size"]')) {
                    FashionStore.showToast('Please select a size');
                    return;
                }
                const size = sizeInput ? sizeInput.value : 'M';
                FashionStore.addToCart(productId, size);
            }
        </script>

    </div>
    <% } else { %>
    <div class="details-section">
        <h2 class="product-title">Product not found</h2>
        <a class="add-cart-btn btn btn-primary" href="<%= request.getContextPath() %>/products">Back to Products</a>
    </div>
    <% } %>

</main>

<% if (product != null) { %>
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
<section class="related-products-section">
    <div class="section-head">
        <h3 class="reviews-heading">Related Products</h3>
        <a href="<%= request.getContextPath() %>/products?category=<%= product.getCategorySlug() %>" class="continue-link">View Category</a>
    </div>
    <div class="product-grid related-products-grid">
        <% for (Product related : relatedProducts) { %>
            <article class="product-card">
                <div class="product-card-image-wrapper">
                    <img class="product-card-image" src="<%= related.getImageUrl() %>" alt="<%= related.getProductName() %>">
                    <button class="product-card-wishlist" onclick="event.preventDefault(); FashionStore.toggleWishlist(<%= related.getProductId() %>, this)" aria-label="Add to wishlist">
                        <svg class="wishlist-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
                        </svg>
                    </button>
                </div>
                <div class="product-card-content">
                    <span class="product-card-brand"><%= related.getBrand() != null && !related.getBrand().isBlank() ? related.getBrand() : related.getCategoryName() %></span>
                    <h4 class="product-card-name"><%= related.getProductName() %></h4>
                    <div class="product-card-price">
                        <span class="product-card-price-current">₹<%= String.format("%.2f", related.getPrice()) %></span>
                    </div>
                    <div class="product-card-actions">
                        <a href="<%= request.getContextPath() %>/product?id=<%= related.getProductId() %>" class="btn btn-primary product-card-add-btn">View Details</a>
                    </div>
                </div>
            </article>
        <% } %>
    </div>
</section>
<% } %>

<section class="reviews-section">
    <h3 class="reviews-heading">Customer Reviews</h3>

    <div class="reviews-layout">
        <!-- Reviews List -->
        <div class="reviews-list">
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
                <div class="review-card">
                    <div class="review-top">
                        <span class="review-author"><%= r.getUserName() %></span>
                        <span class="review-stars">
                            <% for(int i=0; i<r.getRating(); i++) { %>★<% } %><% for(int i=r.getRating(); i<5; i++) { %>☆<% } %>
                        </span>
                    </div>
                    <p class="review-text"><%= r.getComment() %></p>
                    <small class="review-date"><%= new java.text.SimpleDateFormat("MMM dd, yyyy").format(r.getCreatedAt()) %></small>
                </div>
            <%
                    }
                } else {
            %>
                <p class="reviews-empty">No reviews yet. Be the first to review this product!</p>
            <% } %>
        </div>

        <!-- Leave a Review Form -->
        <div class="review-form-card">
            <h4>Write a Review</h4>
            <% if (session.getAttribute("user") != null) { %>
                <form id="reviewForm" onsubmit="return submitReview(event, <%= product.getProductId() %>)">
                    <div class="review-form-group">
                        <label for="reviewRating">Rating</label>
                        <select name="rating" id="reviewRating" required>
                            <option value="5">5 - Excellent</option>
                            <option value="4">4 - Good</option>
                            <option value="3">3 - Average</option>
                            <option value="2">2 - Poor</option>
                            <option value="1">1 - Terrible</option>
                        </select>
                    </div>
                    <div class="review-form-group">
                        <label for="reviewComment">Review</label>
                        <textarea name="comment" id="reviewComment" rows="4" required placeholder="What did you think about this product?"></textarea>
                    </div>
                    <button type="submit" class="btn-primary btn-block">Submit Review</button>
                </form>
            <% } else { %>
                <p class="review-login-prompt">Please log in to write a review.</p>
                <a href="<%= request.getContextPath() %>/login" class="btn-primary btn-block" style="text-align: center;">Log In</a>
            <% } %>
        </div>
    </div>
</section>

<script>
function submitReview(event, productId) {
    event.preventDefault();
    const rating = document.getElementById('reviewRating').value;
    const comment = document.getElementById('reviewComment').value;
    
    fetch('<%= request.getContextPath() %>/review', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest',
            'X-CSRF-Token': window.csrfToken || ''
        },
        body: new URLSearchParams({
            productId: productId,
            rating: rating,
            comment: comment
        })
    })
    .then(res => res.json())
    .then(data => {
        if(data.redirect) {
            window.location.href = data.redirect;
            return;
        }
        if(data.success) {
            FashionStore.showToast("Review submitted successfully", 'success');
            
            // Update reviews section
            const reviewsContainer = document.querySelector('.reviews-list');
            if (reviewsContainer && data.review) {
                const reviewHTML = FashionStore.createReviewHTML(data.review);
                reviewsContainer.insertAdjacentHTML('afterbegin', reviewHTML);
            }
            
            // Clear form
            document.querySelector('#reviewRating').value = '5';
            document.querySelector('#reviewComment').value = '';
        } else {
            FashionStore.showToast(data.message || 'Error submitting review');
        }
    })
    .catch(err => console.error(err));
    return false;
}
</script>
<% } %>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
