<%@ page contentType="text/html;charset=UTF-8" %>
<%
    Object user = session.getAttribute("user");
    boolean isAdmin = false;
    if (user != null) {
        try {
            Object result = user.getClass().getMethod("isAdmin").invoke(user);
            if (result instanceof Boolean) {
                isAdmin = (Boolean) result;
            }
        } catch (Exception ignored) {
            isAdmin = false;
        }
    }

    int initialCartCount = 0;
    // cartItems in session is a List<CartItem>; compute total quantity
    Object sessionCartItems = session.getAttribute("cartItems");
    if (sessionCartItems instanceof java.util.List) {
        java.util.List<?> items = (java.util.List<?>) sessionCartItems;
        for (Object o : items) {
            if (o instanceof com.fashionstore.model.CartItem) {
                initialCartCount += ((com.fashionstore.model.CartItem) o).getQuantity();
            }
        }
    }
%>

    <header class="navbar">
        <div class="container navbar-container">
            <a href="<%= request.getContextPath() %>/home" class="navbar-brand">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z"></path>
                    <line x1="16" y1="8" x2="2" y2="22"></line>
                </svg>
                <span>FashionStore</span>
            </a>

            <form class="nav-search" action="<%= request.getContextPath() %>/products" method="get" role="search">
                <input type="text" id="nav-search-input" name="search" placeholder="Search for products, brands and more" autocomplete="off">
                <button type="submit" class="nav-search-btn" aria-label="Search">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                </button>
            </form>

            <nav class="nav-actions" id="nav-actions">
                <form class="nav-search nav-search-mobile" action="<%= request.getContextPath() %>/products" method="get" role="search">
                    <input type="text" name="search" placeholder="Search products" autocomplete="off">
                    <button type="submit" class="nav-search-btn" aria-label="Search">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                    </button>
                </form>

                <a href="<%= request.getContextPath() %>/products" class="nav-action-btn" aria-label="Shop">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="3" width="7" height="7"></rect>
                        <rect x="14" y="3" width="7" height="7"></rect>
                        <rect x="14" y="14" width="7" height="7"></rect>
                        <rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                    <span class="nav-action-label">Shop</span>
                </a>
                <a href="<%= request.getContextPath() %>/wishlist" class="nav-action-btn" aria-label="Wishlist">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                    <span class="nav-action-label">Wishlist</span>
                </a>
                <a href="<%= request.getContextPath() %>/cart" class="nav-action-btn" onclick="toggleMiniCart(event)" aria-label="Cart">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
                        <line x1="3" y1="6" x2="21" y2="6"></line>
                        <path d="M16 10a4 4 0 0 1-8 0"></path>
                    </svg>
                    <span class="cart-badge" id="nav-cart-badge"><%= initialCartCount %></span>
                    <span class="nav-action-label">Cart</span>
                </a>
                
                <div class="nav-divider"></div>

                <% if (user != null) { %>
                    <a href="<%= request.getContextPath() %>/orders" class="nav-action-btn" aria-label="Account">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                            <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                        <span class="nav-action-label">Account</span>
                    </a>
                    <a href="<%= request.getContextPath() %>/logout" class="nav-action-btn" aria-label="Logout" title="Logout">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                            <polyline points="16 17 21 12 16 7"></polyline>
                            <line x1="21" y1="12" x2="9" y2="12"></line>
                        </svg>
                        <span class="nav-action-label">Logout</span>
                    </a>
                <% } else { %>
                    <a href="<%= request.getContextPath() %>/login" class="nav-action-btn" aria-label="Login">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                            <circle cx="12" cy="7" r="4"></circle>
                        </svg>
                        <span class="nav-action-label">Login</span>
                    </a>
                <% } %>
            </nav>

            <button class="mobile-menu-btn" id="mobile-menu-btn" aria-label="Toggle menu" aria-expanded="false">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="3" y1="12" x2="21" y2="12"></line>
                    <line x1="3" y1="6" x2="21" y2="6"></line>
                    <line x1="3" y1="18" x2="21" y2="18"></line>
                </svg>
            </button>
        </div>
    </header>

    <!-- Hidden element for user login status -->
    <input type="hidden" id="user-logged-in" value="<%= user != null ? "true" : "false" %>">

    <!-- TOAST CONTAINER -->
    <div id="toast-container" class="toast-container"></div>

    <!-- MINI CART DRAWER -->
    <div id="mini-cart-overlay" class="mini-cart-overlay" onclick="toggleMiniCart(event)"></div>
    <div id="mini-cart-drawer" class="mini-cart-drawer">
        <div class="mini-cart-header">
            <h3>Your Cart</h3>
            <button class="close-cart-btn" onclick="toggleMiniCart(event)" aria-label="Close cart">×</button>
        </div>
        <div class="mini-cart-items" id="mini-cart-items">
            <!-- Items injected here by JS -->
        </div>
        <div class="mini-cart-footer">
            <div class="mini-cart-total">
                <span>Total:</span>
                <span id="mini-cart-total-price">₹0.00</span>
            </div>
            <a href="<%= request.getContextPath() %>/cart" class="btn btn-secondary btn-block">View Cart</a>
            <a href="<%= request.getContextPath() %>/checkout" class="btn btn-primary btn-block">Checkout</a>
        </div>
    </div>

    <script>
        const contextPath = '<%= request.getContextPath() %>';
        const csrfToken = '<%= request.getAttribute("csrfToken") != null ? request.getAttribute("csrfToken") : "" %>';
        window.contextPath = contextPath;
        window.csrfToken = csrfToken;
        
        // Mobile menu toggle
        const mobileMenuBtn = document.getElementById('mobile-menu-btn');
        const navActions = document.getElementById('nav-actions');
        if (mobileMenuBtn && navActions) {
            mobileMenuBtn.addEventListener('click', () => {
                const isOpen = navActions.classList.toggle('open');
                mobileMenuBtn.setAttribute('aria-expanded', isOpen);
            });
        }
        
        // Fetch cart count on page load for logged-in users
        document.addEventListener('DOMContentLoaded', function() {
            const isLoggedIn = document.getElementById('user-logged-in').value === 'true';
            if (isLoggedIn) {
                fetch(contextPath + '/cart', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        'X-Requested-With': 'XMLHttpRequest',
                        'X-CSRF-Token': csrfToken
                    },
                    body: new URLSearchParams({ action: 'get' })
                })
                .then(res => res.json())
                .then(data => {
                    if (data.cartCount !== undefined) {
                        const badge = document.getElementById('nav-cart-badge');
                        if (badge) badge.innerText = data.cartCount;
                    }
                })
                .catch(err => {
                    // Silent error handling for cart count fetch
                });
            }
        });
    </script>
