<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
<c:set var="_pageTitle" value="Shopping Cart" scope="request"/>
<c:set var="_pageCSS" value="cart" scope="request"/>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
<script src="<%= request.getContextPath() %>/assets/js/cart-page.js" defer></script>
</head>

<body>

<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<c:set var="freeShipThreshold" value="999.0"/>
<c:set var="amountAwayFromFreeShip" value="${freeShipThreshold - cartTotal}"/>
<c:if test="${amountAwayFromFreeShip < 0}">
    <c:set var="amountAwayFromFreeShip" value="0"/>
</c:if>
<c:set var="qualifiesFreeShip" value="${cartTotal >= freeShipThreshold}"/>

<main class="shell section-block" id="main-content">
    <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="${pageContext.request.contextPath}/home">Home</a>
        <span>/</span>
        <span aria-current="page">Shopping Cart</span>
    </nav>

    <div class="fs-cart-header">
        <div>
            <h1 class="editorial-heading">Shopping Cart</h1>
            <p class="text-secondary">
                <c:choose>
                    <c:when test="${fn:length(cartItems) == 1}">1 item</c:when>
                    <c:otherwise>${fn:length(cartItems)} items</c:otherwise>
                </c:choose>
            </p>
        </div>
        <c:if test="${not empty cartItems}">
        <a href="${pageContext.request.contextPath}/products" class="fs-btn fs-btn--outline">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
            Continue shopping
        </a>
        </c:if>
    </div>

    <c:if test="${not empty cartItems}">
    <div class="fs-free-ship-bar ${qualifiesFreeShip ? 'fs-free-ship-bar--complete' : ''}" role="status">
        <c:if test="${qualifiesFreeShip}">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
            <span>You qualify for <strong>FREE shipping</strong></span>
        </c:if>
        <c:if test="${not qualifiesFreeShip}">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
            <span>Add <strong>₹<fmt:formatNumber value="${amountAwayFromFreeShip}" pattern="0.00"/></strong> more to unlock <strong>FREE shipping</strong></span>
        </c:if>
        <div class="fs-free-ship-bar__progress">
            <div class="fs-free-ship-bar__bar" data-progress="${cartTotal > 0 ? (cartTotal / freeShipThreshold) * 100 : 0}"></div>
        </div>
    </div>
    </c:if>

    <c:if test="${not empty sessionScope.error}">
        <div class="alert alert-error">
            <c:out value="${sessionScope.error}"/>
        </div>
        <c:remove var="error" scope="session"/>
    </c:if>

    <c:if test="${not empty cartItems}">
    <div class="fs-cart-layout">
        <section class="fs-cart-items" aria-label="Cart items">
            <header class="fs-cart-items__header">
                <span>Items in your cart</span>
                <span>
                    <c:choose>
                        <c:when test="${fn:length(cartItems) == 1}">1 item</c:when>
                        <c:otherwise>${fn:length(cartItems)} items</c:otherwise>
                    </c:choose>
                </span>
            </header>

            <c:forEach var="item" items="${cartItems}">
            <article class="fs-cart-item" data-id="${item.cartItemId}">
                <a href="${pageContext.request.contextPath}/product?id=${item.productId}" class="fs-cart-item__image">
                    <img data-src="${item.imageUrl}" loading="lazy" class="lazy-load" onerror="this.src='${pageContext.request.contextPath}/assets/images/placeholder-product.jpg'; this.onerror=null;"
                         alt="${item.productName}">
                </a>

                <div class="fs-cart-item__body">
                    <div class="fs-cart-item__info">
                        <a href="${pageContext.request.contextPath}/product?id=${item.productId}" class="fs-cart-item__name">${item.productName}</a>
                        <ul class="fs-cart-item__meta">
                            <c:if test="${not empty item.sizeLabel}">
                                <li><span>Size</span><span>${item.sizeLabel}</span></li>
                            </c:if>
                            <li><span>Unit price</span><span>₹<fmt:formatNumber value="${item.price}" pattern="0.00"/></span></li>
                            <li class="fs-cart-item__stock"><span></span>In stock</li>
                        </ul>
                    </div>

                    <div class="fs-cart-item__actions">
                        <button type="button" class="fs-cart-item__action" data-id="${item.cartItemId}" aria-label="Save for later">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                            <span>Save for later</span>
                        </button>
                        <button type="button" class="fs-cart-item__action fs-cart-item__action--danger" data-id="${item.cartItemId}" aria-label="Remove from cart">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/></svg>
                            <span>Remove</span>
                        </button>
                    </div>
                </div>

                <div class="fs-cart-item__qty" aria-label="Quantity">
                    <span>Qty</span>
                    <div class="fs-qty-stepper">
                        <button type="button" class="fs-qty-stepper__btn"
                                data-action="decrease"
                                data-id="${item.cartItemId}"
                                data-qty="${item.quantity}"
                                aria-label="Decrease quantity"
                                ${item.quantity <= 1 ? 'disabled' : ''}>
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                        </button>
                        <input type="number" class="fs-qty-stepper__input"
                               value="${item.quantity}"
                               min="1" max="10"
                               data-id="${item.cartItemId}"
                               aria-label="Item quantity">
                        <button type="button" class="fs-qty-stepper__btn"
                                data-action="increase"
                                data-id="${item.cartItemId}"
                                data-qty="${item.quantity}"
                                aria-label="Increase quantity">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                        </button>
                    </div>
                </div>

                <div class="fs-cart-item__total">
                    <span>Subtotal</span>
                    <span id="item-total-${item.cartItemId}">₹<fmt:formatNumber value="${item.price * item.quantity}" pattern="0.00"/></span>
                </div>
            </article>
            </c:forEach>
        </section>

        <div class="fs-cart-summary">
            <div class="fs-cart-summary__card">
                <h2 class="editorial-heading">Order Summary</h2>

                <div class="fs-cart-summary__row">
                    <span>Subtotal (${fn:length(cartItems)} items)</span>
                    <span>₹<span id="summary-subtotal"><fmt:formatNumber value="${cartTotal}" pattern="0.00"/></span></span>
                </div>
                <div class="fs-cart-summary__row">
                    <span>Shipping</span>
                    <span class="text-success">FREE</span>
                </div>
                <div class="fs-cart-summary__row fs-cart-summary__row--discount fs-cart-summary__row--hidden" id="discountRow">
                    <span>Discount</span>
                    <span id="discount">₹0.00</span>
                </div>

                <div class="fs-cart-summary__coupon">
                    <label for="couponCode">Coupon Code</label>
                    <div class="fs-cart-summary__coupon-inputs">
                        <input type="text" id="couponCode" placeholder="Enter code" class="fs-form-input">
                        <button type="button" class="fs-btn fs-btn--primary" id="apply-coupon-btn">Apply</button>
                    </div>
                    <div id="couponMessage" class="text-sm"></div>
                </div>

                <div class="fs-cart-summary__divider"></div>

                <div class="fs-cart-summary__row fs-cart-summary__row--total">
                    <span>Total</span>
                    <span class="fs-cart-summary__total">₹<span id="summary-total"><fmt:formatNumber value="${cartTotal}" pattern="0.00"/></span></span>
                </div>

                <a href="${pageContext.request.contextPath}/checkout" class="fs-btn fs-btn--primary">Proceed to Checkout</a>

                <div class="fs-trust-badges">
                    <div class="fs-trust-badge">
                        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                        </svg>
                        <span>Secure Checkout</span>
                    </div>
                    <div class="fs-trust-badge">
                        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M5 13l4 4L19 7"/>
                        </svg>
                        <span>Free Shipping</span>
                    </div>
                    <div class="fs-trust-badge">
                        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                        </svg>
                        <span>Easy Returns</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
    </c:if>
    <c:if test="${empty cartItems}">
        <div class="fs-empty-state">
            <svg class="fs-empty-state__icon" width="64" height="64" viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
                <circle cx="26" cy="56" r="4"/>
                <circle cx="48" cy="56" r="4"/>
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 4h8l5.6 28H52l6-22H18"/>
            </svg>
            <h3>Your cart is empty</h3>
            <p class="text-secondary">Looks like you haven't added anything yet. Start shopping!</p>
            <a href="${pageContext.request.contextPath}/products" class="fs-btn fs-btn--primary">Browse Products</a>
        </div>
    </c:if>

    <c:if test="${not empty cartItems}">
    <section class="shell section-block">
        <div class="section-header">
            <h2 class="editorial-heading">You might also like</h2>
            <a href="${pageContext.request.contextPath}/products?tag=trending" class="inline-link">View All</a>
        </div>
        <div class="fs-product-grid">
        </div>
    </section>
    </c:if>
</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
