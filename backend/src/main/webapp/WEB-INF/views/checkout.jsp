<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="com.fashionstore.model.CartItem" %>
<%@ page import="com.fashionstore.model.Address" %>
<%@ page import="com.fashionstore.model.User" %>
<%@ page import="java.util.Map" %>

<%--
CHECKOUT PAGE - MVC ARCHITECTURE

REFACTORED FOR PROPER MVC:
- Backend (CheckoutController) calculates ALL totals
- Backend provides cart items, addresses, totals via request attributes
- JSP displays backend-calculated values (NO calculations)
- JavaScript only handles UI interactions (address selection, payment method)
- NO frontend order total calculations
- NO frontend payment amount calculations
- NO frontend discount/tax calculations
- Backend is single source of truth

Data Flow:
1. CheckoutController.displayCheckoutPage() loads cart items and calculates totals
2. CheckoutService.calculateCheckoutTotals() computes subtotal, tax, shipping, discount
3. JSP displays backend-calculated values
4. JavaScript handles address/payment selection UI only
5. Form submission sends address and payment method to backend
6. CheckoutController validates and creates order

Security:
- All calculations performed on backend
- Frontend cannot modify prices or totals
- Payment amount validated on backend before processing
- CSRF token required for form submission
--%>

<!DOCTYPE html>
<html lang="en">
<head>
<%
    request.setAttribute("_pageTitle", "Checkout");
    request.setAttribute("_pageCSS", "checkout");
%>
<jsp:include page="/WEB-INF/views/partials/head.jsp" />
<!-- Stripe.js -->
<script src="https://js.stripe.com/v3/"></script>
<script>
    // Safe console logging
    const safeLog = (message, data) => {
        try {
            if (typeof console !== 'undefined' && console.log) {
                console.log(message, data);
            }
        } catch (e) {
            // Ignore console errors
        }
    };
    
    // Safe error logging
    const safeError = (message, error) => {
        try {
            if (typeof console !== 'undefined' && console.error) {
                console.error(message, error);
            }
        } catch (e) {
            // Ignore console errors
        }
    };
</script>
<script src="<%= request.getContextPath() %>/assets/js/modules/config.js"></script>
<script src="<%= request.getContextPath() %>/assets/js/checkout.js"></script>
<script>
    // Initialize config with server-side variables after config.js loads
    (function() {
        try {
            const serverConfig = {
                contextPath: '<%= request.getContextPath() %>',
                stripePublishableKey: '<%= (request.getAttribute("stripePublishableKey") != null) ? request.getAttribute("stripePublishableKey") : "" %>',
                userEmail: '<%= (session.getAttribute("email") != null) ? session.getAttribute("email") : "" %>',
                csrfToken: '<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>'
            };
            
            safeLog('Checkout: Server config loaded', serverConfig);
            
            // Validate Stripe key
            if (!serverConfig.stripePublishableKey) {
                safeError('Checkout: Stripe publishable key is missing', serverConfig);
            }
            
            // Initialize Config if available, otherwise fallback to window globals
            if (typeof Config !== 'undefined' && Config.init) {
                Config.init(serverConfig);
                safeLog('Checkout: Config initialized successfully');
            } else {
                safeLog('Checkout: Config not available, using window globals fallback');
                window.contextPath = serverConfig.contextPath;
                window.stripePublishableKey = serverConfig.stripePublishableKey;
                window.userEmail = serverConfig.userEmail;
                window.csrfToken = serverConfig.csrfToken;
            }
            
            // Initialize Stripe if key is available
            if (typeof Stripe !== 'undefined' && serverConfig.stripePublishableKey) {
                try {
                    window.stripe = Stripe(serverConfig.stripePublishableKey);
                    safeLog('Checkout: Stripe initialized successfully');
                } catch (e) {
                    safeError('Checkout: Stripe initialization failed', e);
                }
            } else {
                safeError('Checkout: Stripe not available or no publishable key', {
                    stripeAvailable: typeof Stripe !== 'undefined',
                    hasKey: !!serverConfig.stripePublishableKey
                });
            }
        } catch (e) {
            safeError('Checkout: Configuration initialization failed', e);
        }
    })();
</script>
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

    // Get saved addresses
    List<Address> addresses = (List<Address>) request.getAttribute("addresses");
    Address defaultShipping = (Address) request.getAttribute("defaultShipping");
    Address defaultBilling = (Address) request.getAttribute("defaultBilling");

    String selectedShippingId = (String) request.getAttribute("selectedShippingAddressId");
    String selectedBillingId = (String) request.getAttribute("selectedBillingAddressId");
    Boolean useNewShipping = (Boolean) request.getAttribute("useNewShipping");
    Boolean useNewBilling = (Boolean) request.getAttribute("useNewBilling");
    @SuppressWarnings("unchecked")
    Map<String, String> fieldErrors = (Map<String, String>) request.getAttribute("fieldErrors");

    // Resolve which option should be checked initially:
    //  1) Whatever the user previously selected (after a validation error)
    //  2) The default shipping address
    //  3) The first available shipping address
    //  4) Otherwise: "new"
    String effectiveSelectedId = selectedShippingId;
    if ((effectiveSelectedId == null || effectiveSelectedId.isEmpty()) && !Boolean.TRUE.equals(useNewShipping)) {
        if (defaultShipping != null) {
            effectiveSelectedId = String.valueOf(defaultShipping.getAddressId());
        } else if (addresses != null) {
            for (Address a : addresses) {
                if ("shipping".equals(a.getAddressType()) || "both".equals(a.getAddressType())) {
                    effectiveSelectedId = String.valueOf(a.getAddressId());
                    break;
                }
            }
        }
    }
    boolean showNewForm = Boolean.TRUE.equals(useNewShipping)
            || (effectiveSelectedId == null || effectiveSelectedId.isEmpty() || "new".equals(effectiveSelectedId));
%>

<main class="shell section-block" id="main-content">
    <div class="fs-checkout-header">
        <span class="fs-checkout-header__tag">Secure Transaction</span>
        <h1 class="editorial-heading">Checkout</h1>
        <p class="text-secondary">Complete your curated selection with our express secure checkout.</p>
    </div>

    <div class="fs-checkout-progress">
        <div class="fs-checkout-step fs-checkout-step--active" data-step="1">
            <div class="fs-checkout-step__number">1</div>
            <div class="fs-checkout-step__label">Shipping</div>
        </div>
        <div class="fs-checkout-progress__line"></div>
        <div class="fs-checkout-step" data-step="2">
            <div class="fs-checkout-step__number">2</div>
            <div class="fs-checkout-step__label">Payment</div>
        </div>
        <div class="fs-checkout-progress__line"></div>
        <div class="fs-checkout-step" data-step="3">
            <div class="fs-checkout-step__number">3</div>
            <div class="fs-checkout-step__label">Review</div>
        </div>
    </div>

    <div class="fs-checkout-layout">
            
            <div class="fs-checkout-main">
                <form action="<%= request.getContextPath() %>/checkout" method="post" id="checkoutForm">
                    <div class="fs-checkout-step fs-checkout-step--active" id="step1">
                        <h2 class="editorial-heading">Shipping Information</h2>
                        <p class="text-secondary">Where should we deliver your heritage collection?</p>

                        <% if (addresses != null && !addresses.isEmpty()) { %>
                            <div class="fs-saved-addresses">
                                <div class="fs-saved-addresses__header">
                                    <h4>Deliver to a saved address</h4>
                                    <a href="<%= request.getContextPath() %>/account/addresses" target="_blank" rel="noopener" class="inline-link">Manage</a>
                                </div>
                                <div class="fs-saved-addresses__list" id="savedAddressesList">
                                    <% for (Address addr : addresses) {
                                        if (!("shipping".equals(addr.getAddressType()) || "both".equals(addr.getAddressType()))) continue;
                                        boolean isChecked = effectiveSelectedId != null
                                                && effectiveSelectedId.equals(String.valueOf(addr.getAddressId()));
                                    %>
                                        <label class="fs-address-card <%= isChecked ? "fs-address-card--active" : "" %>" data-address-id="<%= addr.getAddressId() %>">
                                            <input type="radio" name="shippingAddressId" value="<%= addr.getAddressId() %>" <%= isChecked ? "checked" : "" %>>
                                            <div class="fs-address-card__content">
                                                <div class="fs-address-card__label">
                                                    <%= addr.getFullName() %>
                                                    <% if (addr.isDefault()) { %>
                                                        <span class="fs-badge">Default</span>
                                                    <% } %>
                                                </div>
                                                <div class="fs-address-card__details">
                                                    <%= addr.getAddressLine1() %><% if (addr.getAddressLine2() != null && !addr.getAddressLine2().isEmpty()) { %>, <%= addr.getAddressLine2() %><% } %><br>
                                                    <%= addr.getCity() %>, <%= addr.getState() %> <%= addr.getPostalCode() %><br>
                                                    <%= addr.getPhone() %>
                                                </div>
                                            </div>
                                        </label>
                                    <% } %>

                                    <label class="fs-address-card fs-address-card--new <%= showNewForm ? "fs-address-card--active" : "" %>" id="newAddressOption">
                                        <input type="radio" name="shippingAddressId" value="new" <%= showNewForm ? "checked" : "" %>>
                                        <div class="fs-address-card__content">
                                            <div class="fs-address-card__label">+ Add a new address</div>
                                            <div class="fs-address-card__details">Use a different shipping address</div>
                                        </div>
                                    </label>
                                </div>
                            </div>
                        <% } else { %>
                            <input type="hidden" name="shippingAddressId" value="new">
                        <% } %>

                        <div class="fs-new-address-form<%= showNewForm ? " fs-new-address-form--visible fs-new-address-form--show" : "" %>" id="newAddressForm">
                            <% if (addresses != null && !addresses.isEmpty()) { %>
                                <h4>New shipping address</h4>
                            <% } %>
                            <div class="fs-form-grid">
                            <div class="fs-form-group">
                                <label for="fullName">Full Name</label>
                                <input type="text" id="fullName" name="fullName" placeholder="John Doe" value="<%= request.getAttribute("fullName") != null ? request.getAttribute("fullName") : "" %>" required class="fs-form-input">
                            </div>
                            <div class="fs-form-group">
                                <label for="address">Street Address</label>
                                <input type="text" id="address" name="address" placeholder="123 Fashion St, Area" value="<%= request.getAttribute("address") != null ? request.getAttribute("address") : "" %>" required class="fs-form-input">
                            </div>
                            <div class="fs-form-group">
                                <label for="city">City</label>
                                <input type="text" id="city" name="city" placeholder="Mumbai" value="<%= request.getAttribute("city") != null ? request.getAttribute("city") : "" %>" required class="fs-form-input">
                            </div>
                            <div class="fs-form-group">
                                <label for="state">State</label>
                                <input type="text" id="state" name="state" placeholder="Maharashtra" value="<%= request.getAttribute("state") != null ? request.getAttribute("state") : "" %>" required class="fs-form-input">
                            </div>
                            <div class="fs-form-group">
                                <label for="zip">ZIP Code</label>
                                <input type="text" id="zip" name="zip" placeholder="400001" value="<%= request.getAttribute("zip") != null ? request.getAttribute("zip") : "" %>" pattern="[0-9]{6}" title="6-digit ZIP code" class="fs-form-input">
                                <% if (fieldErrors != null && fieldErrors.get("postalCode") != null) { %>
                                    <span class="text-sm text-danger"><%= fieldErrors.get("postalCode") %></span>
                                <% } %>
                            </div>
                            <div class="fs-form-group">
                                <label for="phone">Phone Number</label>
                                <input type="tel" id="phone" name="phone" placeholder="9876543210" value="<%= request.getAttribute("phone") != null ? request.getAttribute("phone") : "" %>" pattern="[6-9][0-9]{9}" title="10-digit mobile number" class="fs-form-input">
                                <% if (fieldErrors != null && fieldErrors.get("phone") != null) { %>
                                    <span class="text-sm text-danger"><%= fieldErrors.get("phone") %></span>
                                <% } %>
                            </div>
                        </div>
                        <div class="fs-form-group">
                            <label class="fs-form-checkbox">
                                <input type="checkbox" name="saveAddress" value="true" checked>
                                <span>Save this address to my account</span>
                            </label>
                        </div>
                        </div>
                        <div class="fs-shipping-methods">
                            <label class="fs-shipping-method">
                                <input type="radio" name="shippingMethod" value="STANDARD" checked>
                                <div class="fs-shipping-method__info">
                                    <span class="fs-shipping-method__name">Standard delivery</span>
                                    <span class="text-sm text-secondary">3-6 business days · Free</span>
                                </div>
                            </label>
                            <label class="fs-shipping-method">
                                <input type="radio" name="shippingMethod" value="EXPRESS">
                                <div class="fs-shipping-method__info">
                                    <span class="fs-shipping-method__name">Express delivery</span>
                                    <span class="text-sm text-secondary">1-2 business days · Calculated at dispatch</span>
                                </div>
                            </label>
                        </div>
                        <button type="button" class="fs-btn fs-btn--primary" id="continueToPaymentBtn">
                            Continue to Payment
                        </button>
                        <script>
                            // Defensive checkout button binding
                            (function() {
                                try {
                                    const continueBtn = document.getElementById('continueToPaymentBtn');
                                    if (continueBtn) {
                                        continueBtn.addEventListener('click', function() {
                                            if (typeof Checkout !== 'undefined' && Checkout.validateAndProceedToPayment) {
                                                Checkout.validateAndProceedToPayment();
                                            } else {
                                                safeError('Checkout: Checkout object not available');
                                                alert('Payment system is not ready. Please refresh the page and try again.');
                                            }
                                        });
                                    } else {
                                        safeError('Checkout: Continue to Payment button not found');
                                    }
                                } catch (e) {
                                    safeError('Checkout: Button binding failed', e);
                                }
                            })();
                        </script>
                    </div>

                    <div class="fs-checkout-step" id="step2">
                        <h2 class="editorial-heading">Payment Method</h2>
                        <p class="text-secondary">All transactions are secure and encrypted.</p>
                        <div class="fs-payment-methods">
                            <label class="fs-payment-method">
                                <input type="radio" name="paymentMethod" value="COD" checked>
                                <div class="fs-payment-method__info">
                                    <span class="fs-payment-method__name">Cash on Delivery</span>
                                    <span class="text-sm text-secondary">Pay upon successful heritage collection delivery</span>
                                </div>
                            </label>
                            <label class="fs-payment-method">
                                <input type="radio" name="paymentMethod" value="STRIPE">
                                <div class="fs-payment-method__info">
                                    <span class="fs-payment-method__name">Digital Transaction</span>
                                    <span class="text-sm text-secondary">Secure credit / debit card processing via Stripe</span>
                                </div>
                            </label>
                        </div>
                        
                        <div id="stripe-payment-element-container" class="stripe-payment-element-container--hidden">
                            <div id="stripe-card-element"></div>
                            <div id="stripe-payment-errors" class="text-sm text-danger"></div>
                        </div>
                        
                        <div class="fs-checkout-actions">
                            <button type="button" class="fs-btn fs-btn--outline" onclick="Checkout.goToCheckoutStep(1)">
                                Back to Shipping
                            </button>
                            <button type="button" class="fs-btn fs-btn--primary" onclick="Checkout.reviewOrder()">
                                Review Selection
                            </button>
                        </div>
                    </div>

                    <div class="fs-checkout-step" id="step3">
                        <h2 class="editorial-heading">Final Review</h2>
                        <div class="fs-order-review">
                            <p class="text-secondary">Verify your curated selection and shipping details before we begin processing your order.</p>
                        </div>
                        <div class="fs-checkout-actions">
                            <button type="button" class="fs-btn fs-btn--outline" onclick="Checkout.goToCheckoutStep(2)">
                                Modify Payment
                            </button>
                            <button type="submit" class="fs-btn fs-btn--primary" name="placeOrder" value="true">
                                Confirm Purchase — ₹<%= String.format("%.2f", cartTotal) %>
                            </button>
                        </div>
                    </div>
                </form>
            </div>

            <div class="fs-checkout-summary">
                <div class="fs-order-summary">
                    <h2 class="editorial-heading">Your Selection</h2>
                    
                    <div class="fs-order-summary__items">
                        <% if (cartItems != null) { 
                            for (CartItem item : cartItems) { %>
                        <div class="fs-order-summary__item">
                            <div class="fs-order-summary__image">
                                <img data-src="<%= item.getImageUrl() %>" alt="<%= item.getProductName() %>" loading="lazy" class="lazy-load" onerror="this.src='<%= request.getContextPath() %>/assets/images/placeholder-product.jpg'; this.onerror=null;"
                                <span class="fs-order-summary__qty"><%= item.getQuantity() %></span>
                            </div>
                            <div class="fs-order-summary__details">
                                <span><%= item.getProductName() %></span>
                                <span>₹<%= String.format("%.2f", item.getPrice() * item.getQuantity()) %></span>
                            </div>
                        </div>
                        <% } 
                        } %>
                    </div>

                    <div class="fs-order-summary__divider"></div>

                    <div class="fs-order-summary__totals">
                        <div class="fs-order-summary__row">
                            <span>Subtotal</span>
                            <span>₹<%= String.format("%.2f", cartTotal) %></span>
                        </div>
                        <div class="fs-order-summary__row">
                            <span>Boutique Shipping</span>
                            <span class="text-success">Complimentary</span>
                        </div>
                        <div class="fs-order-summary__divider"></div>
                        <div class="fs-order-summary__row fs-order-summary__row--total">
                            <span>Grand Total</span>
                            <span class="fs-order-summary__total">₹<%= String.format("%.2f", cartTotal) %></span>
                        </div>
                    </div>
                    
                    <div class="fs-checkout-assurance">
                        <div class="fs-checkout-assurance__item">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
                            <span>Encrypted Transaction</span>
                        </div>
                        <div class="fs-checkout-assurance__item">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><path d="M16 8l-4 4-4-4"></path><path d="M12 12v8"></path></svg>
                            <span>Authenticity Guaranteed</span>
                        </div>
                    </div>
                </div>
            </div>

        </div>

    </div>
</main>

<!-- FOOTER -->
<jsp:include page="/WEB-INF/views/partials/footer.jsp" />

</body>
</html>
