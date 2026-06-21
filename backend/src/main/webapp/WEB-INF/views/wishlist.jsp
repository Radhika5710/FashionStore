<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.WishlistItem" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Wishlist");
    request.setAttribute("_pageCSS", "wishlist");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    List<WishlistItem> wishlistItems = new ArrayList<>();
    Object obj = request.getAttribute("wishlistItems");
    if (obj instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<WishlistItem> temp = (List<WishlistItem>) obj;
        wishlistItems = temp;
    }
%>

<main class="shell section-block" id="main-content">
    <div class="fs-wishlist-header">
        <span class="fs-wishlist-header__tag">Your Curated Selection</span>
        <h1 class="editorial-heading">My Wishlist</h1>
        <p class="text-secondary"><%= wishlistItems.size() %> items saved for later</p>
    </div>

    <% if (!wishlistItems.isEmpty()) { %>
        <div class="product-card-grid">
            <% for (WishlistItem item : wishlistItems) { %>
                <article class="product-card" id="wishlist-item-<%= item.getProductId() %>">
                    <div class="product-card__media">
                        <a href="<%= request.getContextPath() %>/product?id=<%= item.getProductId() %>">
                            <img data-src="<%= item.getImageUrl() %>" alt="<%= item.getProductName() %>" loading="lazy" class="lazy-load" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;">
                        </a>
                        <button class="product-card__wishlist product-card__wishlist--active" data-product-id="<%= item.getProductId() %>" aria-label="Remove from wishlist">
                            <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                            </svg>
                        </button>
                    </div>
                    <div class="product-card__body">
                        <span class="product-card__eyebrow">Heritage Collection</span>
                        <a href="<%= request.getContextPath() %>/product?id=<%= item.getProductId() %>" class="product-card__title"><%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(item.getProductName()) %></a>
                        <div class="product-card__footer">
                            <p class="product-card__price">₹<%= String.format("%.2f", item.getPrice()) %></p>
                            <div class="product-card__actions">
                                <button class="product-card__btn product-card__btn--primary" data-product-id="<%= item.getProductId() %>">Add to Cart</button>
                            </div>
                        </div>
                    </div>
                </article>
            <% } %>
        </div>
    <% } else { %>
        <div class="fs-empty-state">
            <div class="fs-empty-state__visual">
                <svg width="64" height="64" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
                </svg>
            </div>
            <h2 class="editorial-heading">Your wishlist is empty</h2>
            <p class="text-secondary">Save pieces you love to your private collection for easy access later.</p>
            <div class="wishlist-content--margin-top">
                <a href="<%= request.getContextPath() %>/products" class="fs-btn fs-btn--primary">Browse Collection</a>
            </div>
        </div>
    <% } %>
</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
