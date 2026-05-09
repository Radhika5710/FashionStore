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

    <header class="navbar commerce-navbar" data-commerce-nav>
        <div class="container navbar-container">
            <button class="mobile-menu-btn nav-icon-btn" id="mobile-menu-btn" aria-label="Open navigation" aria-expanded="false">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
                    <line x1="4" y1="7" x2="20" y2="7"></line>
                    <line x1="4" y1="12" x2="20" y2="12"></line>
                    <line x1="4" y1="17" x2="20" y2="17"></line>
                </svg>
            </button>

            <a href="<%= request.getContextPath() %>/home" class="navbar-brand" aria-label="FashionStore home">
                <img src="<%= request.getContextPath() %>/assets/images/logo.svg" alt="FashionStore" class="brand-logo">
            </a>

            <nav class="nav-category-menu" aria-label="Primary categories">
                <a href="<%= request.getContextPath() %>/products?category=women" data-mega-trigger>Women</a>
                <a href="<%= request.getContextPath() %>/products?category=men" data-mega-trigger>Men</a>
                <a href="<%= request.getContextPath() %>/products?category=footwear" data-mega-trigger>Footwear</a>
                <a href="<%= request.getContextPath() %>/products?category=accessories" data-mega-trigger>Accessories</a>
                <a href="<%= request.getContextPath() %>/products?tag=new">New</a>
                <a href="<%= request.getContextPath() %>/products?tag=deals">Sale</a>
            </nav>

            <form class="nav-search" action="<%= request.getContextPath() %>/products" method="get" role="search">
                <label class="sr-only" for="nav-search-input">Search catalog</label>
                <input type="text" id="nav-search-input" name="search" placeholder="Search products..." autocomplete="off" data-search-input>
                <button type="submit" class="nav-search-btn" aria-label="Search">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                        <circle cx="11" cy="11" r="7"></circle>
                        <line x1="20" y1="20" x2="16.2" y2="16.2"></line>
                    </svg>
                </button>
            </form>

            <nav class="nav-actions" id="nav-actions" aria-label="Customer actions">
                <button id="dark-mode-toggle" class="nav-action-btn nav-icon-btn" aria-label="Switch to dark mode" type="button">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="5"></circle>
                        <line x1="12" y1="1" x2="12" y2="3"></line>
                        <line x1="12" y1="21" x2="12" y2="23"></line>
                        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                        <line x1="1" y1="12" x2="3" y2="12"></line>
                        <line x1="21" y1="12" x2="23" y2="12"></line>
                        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                    </svg>
                    <span class="nav-action-label">Theme</span>
                </button>
                <a href="<%= request.getContextPath() %>/wishlist" class="nav-action-btn nav-icon-btn" aria-label="Wishlist">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20.3 5.7a5.1 5.1 0 0 0-7.2 0L12 6.8l-1.1-1.1a5.1 5.1 0 0 0-7.2 7.2L12 21l8.3-8.1a5.1 5.1 0 0 0 0-7.2z"></path>
                    </svg>
                    <span class="nav-action-label">Wishlist</span>
                </a>
                <a href="<%= request.getContextPath() %>/cart" class="nav-action-btn nav-icon-btn" onclick="toggleMiniCart(event)" aria-label="Cart">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
                        <line x1="3" y1="6" x2="21" y2="6"></line>
                        <path d="M16 10a4 4 0 0 1-8 0"></path>
                    </svg>
                    <span class="cart-badge" id="nav-cart-badge"><%= initialCartCount %></span>
                    <span class="nav-action-label">Cart</span>
                </a>

                <div class="account-menu">
                    <button type="button" class="nav-action-btn nav-icon-btn account-trigger" aria-label="Account menu" aria-expanded="false" data-account-trigger>
                        <span class="account-avatar"><%= user != null ? "A" : "U" %></span>
                        <span class="nav-action-label">Account</span>
                    </button>
                    <div class="account-dropdown" data-account-dropdown>
                        <% if (user != null) { %>
                            <a href="<%= request.getContextPath() %>/orders">Order history</a>
                            <a href="<%= request.getContextPath() %>/wishlist">Saved wishlist</a>
                            <% if (isAdmin) { %><a href="<%= request.getContextPath() %>/admin/dashboard">Admin dashboard</a><% } %>
                            <a href="<%= request.getContextPath() %>/logout">Logout</a>
                        <% } else { %>
                            <a href="<%= request.getContextPath() %>/login">Sign in</a>
                            <a href="<%= request.getContextPath() %>/register">Create account</a>
                            <a href="<%= request.getContextPath() %>/orders">Track orders</a>
                        <% } %>
                    </div>
                </div>
            </nav>
        </div>

        <div class="mega-menu container" aria-label="Featured navigation">
            <div>
                <span class="mega-kicker">Collections</span>
                <a href="<%= request.getContextPath() %>/products?category=women">Women's tailoring</a>
                <a href="<%= request.getContextPath() %>/products?category=men">Men's essentials</a>
                <a href="<%= request.getContextPath() %>/products?category=footwear">Sneaker edit</a>
                <a href="<%= request.getContextPath() %>/products?category=accessories">Accessories</a>
            </div>
            <div>
                <span class="mega-kicker">Shopping</span>
                <a href="<%= request.getContextPath() %>/products?tag=new">New arrivals</a>
                <a href="<%= request.getContextPath() %>/products?tag=trending">Trending now</a>
                <a href="<%= request.getContextPath() %>/products?tag=deals">Sale</a>
                <a href="<%= request.getContextPath() %>/products">All products</a>
            </div>
            <div class="mega-feature">
                <span class="mega-kicker">Editorial</span>
                <strong>Modern uniform dressing</strong>
                <p>Sharp silhouettes, wearable layers, and elevated everyday accessories.</p>
                <a href="<%= request.getContextPath() %>/products" class="btn btn-outline btn-sm" style="margin-top: var(--space-3);">Shop the collection</a>
            </div>
        </div>
    </header>

    <div class="mobile-nav-overlay" id="mobile-nav-overlay"></div>

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
        
        // Commerce navigation
        const mobileMenuBtn = document.getElementById('mobile-menu-btn');
        const navActions = document.getElementById('nav-actions');
        const mobileOverlay = document.getElementById('mobile-nav-overlay');
        if (mobileMenuBtn && navActions) {
            mobileMenuBtn.addEventListener('click', () => {
                const isOpen = navActions.classList.toggle('open');
                mobileMenuBtn.setAttribute('aria-expanded', isOpen);
                mobileOverlay?.classList.toggle('active', isOpen);
                document.body.classList.toggle('nav-drawer-open', isOpen);
            });
        }
        mobileOverlay?.addEventListener('click', () => {
            navActions?.classList.remove('open');
            mobileMenuBtn?.setAttribute('aria-expanded', 'false');
            mobileOverlay.classList.remove('active');
            document.body.classList.remove('nav-drawer-open');
        });

        const commerceNavbar = document.querySelector('[data-commerce-nav]');
        const megaMenu = document.querySelector('.mega-menu');
        const megaTriggers = document.querySelectorAll('[data-mega-trigger]');
        
        // Mega menu hover behavior
        let megaMenuTimeout;
        
        megaTriggers.forEach(trigger => {
            trigger.addEventListener('mouseenter', () => {
                clearTimeout(megaMenuTimeout);
                megaMenu?.classList.add('active');
            });
        });
        
        if (megaMenu) {
            megaMenu.addEventListener('mouseenter', () => {
                clearTimeout(megaMenuTimeout);
            });
            
            megaMenu.addEventListener('mouseleave', () => {
                megaMenuTimeout = setTimeout(() => {
                    megaMenu.classList.remove('active');
                }, 200);
            });
        }
        
        document.querySelectorAll('.nav-category-menu a:not([data-mega-trigger])').forEach(link => {
            link.addEventListener('mouseenter', () => {
                megaMenuTimeout = setTimeout(() => {
                    megaMenu?.classList.remove('active');
                }, 200);
            });
        });

        window.addEventListener('scroll', () => {
            commerceNavbar?.classList.toggle('scrolled', window.scrollY > 12);
        }, { passive: true });

        const accountTrigger = document.querySelector('[data-account-trigger]');
        const accountDropdown = document.querySelector('[data-account-dropdown]');
        accountTrigger?.addEventListener('click', () => {
            const isOpen = accountDropdown?.classList.toggle('open');
            accountTrigger.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        });
        document.addEventListener('click', (event) => {
            if (!event.target.closest('.account-menu')) {
                accountDropdown?.classList.remove('open');
                accountTrigger?.setAttribute('aria-expanded', 'false');
            }
        });

        document.querySelectorAll('[data-search-input]').forEach((input) => {
            input.addEventListener('focus', () => input.closest('.nav-search')?.classList.add('suggestions-open'));
            input.addEventListener('input', () => input.closest('.nav-search')?.classList.add('suggestions-open'));
            input.addEventListener('blur', () => {
                window.setTimeout(() => input.closest('.nav-search')?.classList.remove('suggestions-open'), 160);
            });
        });
        
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
