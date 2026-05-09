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

<main class="wishlist-page container">
    <div class="wishlist-header">
        <h1 class="wishlist-title">My Wishlist</h1>
        <p class="wishlist-count"><%= wishlistItems.size() %> items saved</p>
    </div>

    <% if (!wishlistItems.isEmpty()) { %>
        <div class="product-grid">
            <% for (WishlistItem item : wishlistItems) { %>
                <div class="product-card" id="wishlist-item-<%= item.getProductId() %>">
                    <div class="product-card-image-wrapper">
                        <img src="<%= item.getImageUrl() %>" alt="<%= item.getProductName() %>" class="product-card-image">
                        <button class="product-card-wishlist active" onclick="removeWishlistItem(<%= item.getProductId() %>)" aria-label="Remove from wishlist">
                            <svg width="20" height="20" fill="currentColor" viewBox="0 0 24 24">
                                <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
                            </svg>
                        </button>
                    </div>
                    <div class="product-card-content">
                        <span class="product-card-brand">Saved Item</span>
                        <h3 class="product-card-name"><%= item.getProductName() %></h3>
                        <div class="product-card-bottom">
                            <div class="product-card-price">
                                <span class="product-card-price-current">&#8377;<%= String.format("%.2f", item.getPrice()) %></span>
                            </div>
                            <div class="product-card-actions">
                                <button class="btn btn-primary btn-sm" onclick="FashionStore.addToCart(<%= item.getProductId() %>)">Add to Cart</button>
                            </div>
                        </div>
                    </div>
                </div>
            <% } %>
        </div>
    <% } else { %>
        <div class="wishlist-empty">
            <svg width="64" height="64" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"></path>
            </svg>
            <h2>Your wishlist is empty</h2>
            <p>Save items you love to your wishlist.</p>
            <a href="<%= request.getContextPath() %>/products" class="btn btn-primary">Browse Products</a>
        </div>
    <% } %>
</main>

<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<script>
function removeWishlistItem(productId) {
    if (!confirm('Are you sure you want to remove this item from wishlist?')) {
        return;
    }
    
    fetch(window.contextPath + '/wishlist', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest',
            'X-CSRF-Token': window.csrfToken || ''
        },
        body: new URLSearchParams({
            action: 'remove',
            productId: productId
        })
    })
    .then(res => res.json())
    .then(data => {
        if(data.success) {
            document.getElementById('wishlist-item-' + productId).remove();
            FashionStore.showToast(data.message, 'success');
        } else {
            FashionStore.showToast(data.message || 'Failed to remove item', 'error');
        }
    })
    .catch(err => {
        console.error("Error removing from wishlist:", err);
        FashionStore.showToast('Failed to remove item', 'error');
    });
}
</script>
</body>
</html>
