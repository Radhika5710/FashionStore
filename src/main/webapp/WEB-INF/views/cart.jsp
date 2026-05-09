<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.CartItem" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Cart");
    request.setAttribute("_pageCSS", "cart");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<!-- NAVBAR -->
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    List<CartItem> cartItems = new ArrayList<>();
    Object obj = request.getAttribute("cartItems");
    if (obj instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<CartItem> temp = (List<CartItem>) obj;
        cartItems = temp;
    }

    Object totalObj = request.getAttribute("cartTotal");
    double cartTotal = (totalObj instanceof Number) ? ((Number) totalObj).doubleValue() : 0.0;
%>

<main class="cart-page" data-context-path="<%= request.getContextPath() %>">

    <!-- ══ PAGE HEADER ══ -->
    <div class="cart-header">
        <h1 class="cart-title">My Cart</h1>
        <p class="cart-subtitle"><span id="cart-count-header"><%= cartItems.size() %></span> item<%= cartItems.size() != 1 ? "s" : "" %> in your bag</p>
    </div>

    <!-- UI FEEDBACK -->
    <% String error = (String) session.getAttribute("error"); %>
    <% if (error != null) { %>
        <div class="alert alert-danger"><%= error %></div>
        <% session.removeAttribute("error"); %>
    <% } %>

    <% if (!cartItems.isEmpty()) { %>

    <div class="cart-layout" id="cart-container">

        <!-- ══ CART ITEMS ══ -->
        <div class="cart-items-col" id="cart-items-list">

            <% for (CartItem item : cartItems) {
                double itemTotal = item.getPrice() * item.getQuantity();
            %>
            <div class="cart-card" id="cart-item-<%= item.getCartItemId() %>" data-id="<%= item.getCartItemId() %>">

                <!-- PRODUCT IMAGE -->
                <div class="cart-img-wrap">
                    <img src="<%= item.getImageUrl() %>" alt="<%= item.getProductName() %>" class="cart-img">
                </div>

                <!-- PRODUCT DETAILS -->
                <div class="cart-info">
                    <h3 class="cart-product-name"><%= item.getProductName() %></h3>
                    <p class="cart-unit-price">₹<%= String.format("%.2f", item.getPrice()) %> each</p>
                    <p class="cart-size-label">Size: <strong><%= item.getSizeLabel() %></strong></p>
                    <p class="cart-item-total">Subtotal: <strong>₹<span class="item-total-val" id="item-total-<%= item.getCartItemId() %>"><%= String.format("%.2f", itemTotal) %></span></strong></p>
                </div>

                <!-- QUANTITY CONTROLS -->
                <div class="cart-qty-section">
                    <span class="qty-label">Qty</span>
                    <div class="qty-controls">

                        <!-- DECREASE -->
                        <button type="button" class="qty-btn qty-decrease ajax-qty-btn" 
                                data-action="decrease" 
                                data-id="<%= item.getCartItemId() %>"
                                data-qty="<%= item.getQuantity() %>"
                                title="Decrease quantity">−</button>

                        <span class="qty-value" id="qty-val-<%= item.getCartItemId() %>"><%= item.getQuantity() %></span>

                        <!-- INCREASE -->
                        <button type="button" class="qty-btn qty-increase ajax-qty-btn" 
                                data-action="increase" 
                                data-id="<%= item.getCartItemId() %>"
                                data-qty="<%= item.getQuantity() %>"
                                title="Increase quantity">+</button>

                    </div>
                </div>

                <!-- REMOVE BUTTON -->
                <div class="cart-remove-col">
                    <button type="button" class="remove-btn ajax-remove-btn" data-id="<%= item.getCartItemId() %>" title="Remove Item">
                        <span class="remove-icon">✕</span>
                        <span class="remove-text">Remove</span>
                    </button>
                    <button type="button" class="save-later-btn ajax-save-later-btn" data-id="<%= item.getCartItemId() %>" title="Save for Later">
                        <span class="remove-icon">♥</span>
                        <span class="remove-text">Save</span>
                    </button>
                </div>

            </div>
            <% } %>

        </div>

        <!-- ══ ORDER SUMMARY ══ -->
        <div class="cart-summary-col">
            <div class="order-summary-card">
                <h2 class="summary-title">Order Summary</h2>

                <div class="summary-row">
                    <span>Subtotal (<span id="summary-count"><%= cartItems.size() %></span> items)</span>
                    <span>₹<span id="summary-subtotal"><%= String.format("%.2f", cartTotal) %></span></span>
                </div>
                <div class="summary-row">
                    <span>Shipping</span>
                    <span class="free-tag">FREE</span>
                </div>
                <div class="summary-row summary-discount">
                    <span>Discount</span>
                    <span>₹0.00</span>
                </div>
                <div class="shipping-estimator">
                    <label for="shippingPin">Delivery estimate</label>
                    <div>
                        <input type="text" id="shippingPin" placeholder="Enter PIN code" maxlength="6">
                        <button type="button" onclick="FashionStore.showToast('Estimated delivery: 3-6 business days', 'success')">Check</button>
                    </div>
                </div>
                
                <!-- Coupon Code Section -->
                <div class="coupon-section">
                    <div class="coupon-input-wrapper">
                        <input type="text" id="couponCode" placeholder="Enter coupon code" class="coupon-input">
                        <button type="button" class="apply-coupon-btn" onclick="FashionStore.applyCoupon()">Apply</button>
                    </div>
                    <div id="couponMessage" class="coupon-message"></div>
                </div>
                
                <div class="summary-row total">
                    <span>Total</span>
                    <span>₹<span id="summary-total"><%= String.format("%.2f", cartTotal) %></span></span>
                </div>
                
                <a href="<%= request.getContextPath() %>/checkout" class="checkout-btn">Proceed to Checkout</a>
                <div class="checkout-trust">
                    <span>Secure checkout</span>
                    <span>Free shipping</span>
                    <span>Easy returns</span>
                </div>
            </div>
        </div>

    </div>

    <% } else { %>

    <!-- ══ EMPTY CART STATE ══ -->
    <div class="cart-empty">
            <svg class="empty-icon cart-empty-icon" viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="26" cy="56" r="4"/>
            <circle cx="48" cy="56" r="4"/>
            <path stroke-linecap="round" stroke-linejoin="round" d="M4 4h8l5.6 28H52l6-22H18"/>
        </svg>
        <h2>Your cart is empty</h2>
        <p>Looks like you haven't added anything yet. Start shopping!</p>
        <a href="<%= request.getContextPath() %>/products" class="browse-btn">Browse Products</a>
    </div>

    <% } %>

    <section class="cart-recommendations">
        <div class="section-head">
            <h2 class="display-3">Complete the look</h2>
            <a href="<%= request.getContextPath() %>/products?tag=trending" class="btn btn-outline">Trending now</a>
        </div>
        <div class="recommendation-strip">
            <a href="<%= request.getContextPath() %>/products?category=accessories">Add accessories</a>
            <a href="<%= request.getContextPath() %>/products?category=footwear">Explore footwear</a>
            <a href="<%= request.getContextPath() %>/products?tag=deals">Shop sale</a>
        </div>
    </section>

</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<script src="<%= request.getContextPath() %>/assets/js/cart.js"></script>

</body>
</html>
