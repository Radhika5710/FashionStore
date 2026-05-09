<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.CartItem" %>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Checkout");
    request.setAttribute("_pageCSS", "checkout");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
</head>

<body>

<!-- NAVBAR -->
<jsp:include page="/WEB-INF/views/partials/navbar.jsp" />

<%
    List<CartItem> cartItems = new ArrayList<>();
    Object cartItemsObj = request.getAttribute("cartItems");
    if (cartItemsObj instanceof List<?>) {
        @SuppressWarnings("unchecked")
        List<CartItem> temp = (List<CartItem>) cartItemsObj;
        cartItems = temp;
    }
    
    Object totalObj = request.getAttribute("cartTotal");
    double cartTotal = (totalObj instanceof Number) ? ((Number) totalObj).doubleValue() : 0.0;
%>

<main class="checkout-page">
    <div class="checkout-container">
        
        <div class="checkout-header">
            <h1 class="checkout-title">Secure Checkout</h1>
            <p class="checkout-subtitle">Complete your order in a few simple steps</p>
        </div>

        <!-- ══ CHECKOUT PROGRESS INDICATOR ══ -->
        <div class="checkout-progress">
            <div class="progress-step active" data-step="1">
                <div class="step-number">1</div>
                <div class="step-label">Shipping</div>
            </div>
            <div class="progress-line"></div>
            <div class="progress-step" data-step="2">
                <div class="step-number">2</div>
                <div class="step-label">Payment</div>
            </div>
            <div class="progress-line"></div>
            <div class="progress-step" data-step="3">
                <div class="step-number">3</div>
                <div class="step-label">Review</div>
            </div>
        </div>

        <div class="checkout-layout">
            
            <!-- ══ SHIPPING & PAYMENT FORM ══ -->
            <div class="checkout-form-col">
                <form action="<%= request.getContextPath() %>/checkout" method="post" class="modern-form" id="checkoutForm">
                    <% String formError = (String) request.getAttribute("error"); %>
                    <div id="form-error" class="form-error" style="<%= formError != null ? "display:block;" : "display:none;" %>color:var(--color-danger);margin-bottom:var(--space-4);padding:var(--space-3);background:#FEF2F2;border-radius:var(--radius-md);font-size:var(--text-sm);">
                        <%= formError != null ? formError : "" %>
                    </div>
                    <input type="hidden" name="csrf_token" value="<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>">
                    
                    <!-- Step 1: Shipping -->
                    <div class="form-section checkout-step-section" id="step1">
                        <h3 class="checkout-section-title">Shipping Information</h3>
                        <div class="saved-addresses">
                            <button type="button" class="saved-address-card active">Home address</button>
                            <button type="button" class="saved-address-card">Office address</button>
                        </div>
                        <div class="form-grid">
                            <div class="form-group full-width">
                                <label for="fullName">Full Name</label>
                                <input type="text" id="fullName" name="fullName" placeholder="John Doe" value="<%= request.getAttribute("fullName") != null ? request.getAttribute("fullName") : "" %>" required>
                            </div>
                            <div class="form-group full-width">
                                <label for="address">Street Address</label>
                                <input type="text" id="address" name="address" placeholder="123 Fashion St, Area" value="<%= request.getAttribute("address") != null ? request.getAttribute("address") : "" %>" required>
                            </div>
                            <div class="form-group">
                                <label for="city">City</label>
                                <input type="text" id="city" name="city" placeholder="Mumbai" value="<%= request.getAttribute("city") != null ? request.getAttribute("city") : "" %>" required>
                            </div>
                            <div class="form-group">
                                <label for="state">State</label>
                                <input type="text" id="state" name="state" placeholder="Maharashtra" value="<%= request.getAttribute("state") != null ? request.getAttribute("state") : "" %>" required>
                            </div>
                            <div class="form-group">
                                <label for="zip">ZIP Code</label>
                                <input type="text" id="zip" name="zip" placeholder="400001" value="<%= request.getAttribute("zip") != null ? request.getAttribute("zip") : "" %>" required pattern="[0-9]{6}" title="6-digit ZIP code">
                            </div>
                            <div class="form-group">
                                <label for="phone">Phone Number</label>
                                <input type="tel" id="phone" name="phone" placeholder="9876543210" value="<%= request.getAttribute("phone") != null ? request.getAttribute("phone") : "" %>" required pattern="[6-9][0-9]{9}" title="10-digit mobile number">
                            </div>
                        </div>
                        <div class="shipping-methods">
                            <label class="payment-card">
                                <input type="radio" name="shippingMethod" value="STANDARD" checked>
                                <div class="payment-info">
                                    <span class="payment-name">Standard delivery</span>
                                    <span class="payment-desc">3-6 business days · Free</span>
                                </div>
                            </label>
                            <label class="payment-card">
                                <input type="radio" name="shippingMethod" value="EXPRESS">
                                <div class="payment-info">
                                    <span class="payment-name">Express delivery</span>
                                    <span class="payment-desc">1-2 business days · Calculated at dispatch</span>
                                </div>
                            </label>
                        </div>
                        <button type="button" class="place-order-btn" onclick="FashionStore.validateAndProceedToPayment()">
                            Continue to Payment
                        </button>
                    </div>

                    <!-- Step 2: Payment -->
                    <div class="form-section checkout-step-section" id="step2" style="display: none;">
                        <h3 class="checkout-section-title">Payment Method</h3>
                        <div class="payment-options">
                            <label class="payment-card">
                                <input type="radio" name="paymentMethod" value="COD" checked>
                                <div class="payment-info">
                                    <span class="payment-name">Cash on Delivery</span>
                                    <span class="payment-desc">Pay when you receive the package</span>
                                </div>
                            </label>
                            <label class="payment-card">
                                <input type="radio" name="paymentMethod" value="CARD">
                                <div class="payment-info">
                                    <span class="payment-name">Credit / Debit Card</span>
                                    <span class="payment-desc">Secure online payment via Razorpay</span>
                                </div>
                            </label>
                            <label class="payment-card">
                                <input type="radio" name="paymentMethod" value="UPI">
                                <div class="payment-info">
                                    <span class="payment-name">UPI Payment</span>
                                    <span class="payment-desc">Pay using Google Pay, PhonePe, or Paytm</span>
                                </div>
                            </label>
                        </div>
                        <button type="button" class="place-order-btn secondary" onclick="FashionStore.goToCheckoutStep(1)">
                            Back
                        </button>
                        <button type="button" class="place-order-btn" onclick="FashionStore.reviewOrder()">
                            Review Order
                        </button>
                    </div>

                    <!-- Step 3: Review -->
                    <div class="form-section checkout-step-section" id="step3" style="display: none;">
                        <h3 class="checkout-section-title">Review Your Order</h3>
                        <div class="order-review">
                            <p>Please review your shipping and payment details before placing the order.</p>
                        </div>
                        <button type="button" class="place-order-btn secondary" onclick="FashionStore.goToCheckoutStep(2)">
                            Back
                        </button>
                        <button type="submit" class="place-order-btn" name="placeOrder" value="true">
                            Complete Purchase – ₹<%= String.format("%.2f", cartTotal) %>
                        </button>
                    </div>
                </form>
            </div>

            <!-- ══ ORDER SUMMARY ══ -->
            <div class="checkout-summary-col">
                <div class="order-summary-card">
                    <h3 class="summary-title">Order Summary</h3>
                    
                    <div class="summary-items">
                        <% if (cartItems != null) { 
                            for (CartItem item : cartItems) { %>
                        <div class="summary-item">
                            <div class="item-img-wrap">
                                <img src="<%= item.getImageUrl() %>" alt="<%= item.getProductName() %>">
                                <span class="item-qty-badge"><%= item.getQuantity() %></span>
                            </div>
                            <div class="item-details">
                                <span class="item-name"><%= item.getProductName() %></span>
                                <span class="item-price">₹<%= String.format("%.2f", item.getPrice() * item.getQuantity()) %></span>
                            </div>
                        </div>
                        <% } 
                        } %>
                    </div>

                    <div class="summary-divider"></div>

                    <div class="summary-totals">
                        <div class="summary-row">
                            <span>Subtotal</span>
                            <span>₹<%= String.format("%.2f", cartTotal) %></span>
                        </div>
                        <div class="summary-row">
                            <span>Shipping</span>
                            <span class="free-text">FREE</span>
                        </div>
                        <div class="summary-divider"></div>
                        <div class="summary-row grand-total">
                            <span>Total</span>
                            <span class="total-amount">₹<%= String.format("%.2f", cartTotal) %></span>
                        </div>
                    </div>
                    <div class="checkout-assurance">
                        <span>SSL secure payment</span>
                        <span>Encrypted order data</span>
                        <span>7-day easy returns</span>
                    </div>
                </div>
            </div>

    </div>
</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

<script>
document.getElementById('checkoutForm').addEventListener('submit', function(e) {
    const form = this;
    const errorEl = document.getElementById('form-error');
    const requiredFields = ['fullName', 'address', 'city', 'state', 'zip', 'phone'];
    
    // NOTE: dollar-brace expressions below are JSP-escaped (\$\{) so Jasper's EL
    // parser ignores them and the literal dollar-brace reaches the browser as a
    // JS template literal. Without the backslash, Jasper would see the regex
    // /([A-Z])/g inside the dollar-brace and throw ELException at compile time,
    // causing HTTP 500 on /checkout.
    for (const fieldName of requiredFields) {
        const input = form.querySelector(`[name="\${fieldName}"]`);
        if (!input || !input.value || !input.value.trim()) {
            e.preventDefault();
            if (errorEl) {
                const pretty = fieldName.replace(/([A-Z])/g, ' $1').toLowerCase();
                errorEl.textContent = 'Please fill in the ' + pretty;
                errorEl.style.display = 'block';
            }
            if (input) input.focus();
            return;
        }
    }
    
    if (errorEl) errorEl.style.display = 'none';
    
    const btn = form.querySelector('.place-order-btn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Processing...';
});
</script>

</body>
</html>
