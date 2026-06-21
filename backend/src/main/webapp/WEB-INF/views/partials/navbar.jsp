<%@ page contentType="text/html;charset=UTF-8" %>
<%
    Object customerAuthObj = session.getAttribute("customerAuth");
    com.fashionstore.model.User user = (customerAuthObj instanceof com.fashionstore.model.User) ? (com.fashionstore.model.User) customerAuthObj : null;
    String userInitials = "U";
    if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
        String[] parts = user.getFullName().trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 2); i++) {
            sb.append(parts[i].substring(0, 1).toUpperCase());
        }
        userInitials = sb.toString();
    }

    int initialCartCount = 0;
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
    <div class="container navbar-inner">
        <div class="nav-left">
            <a href="<%= request.getContextPath() %>/home" class="navbar-brand" aria-label="FashionStore home">
                <span class="navbar-logo">FashionStore</span>
            </a>
        </div>

        <nav class="nav-center" aria-label="Primary Categories">
            <ul class="nav-links">
                <li><a href="<%= request.getContextPath() %>/products?category=women" class="nav-link">Women</a></li>
                <li><a href="<%= request.getContextPath() %>/products?category=men" class="nav-link">Men</a></li>
                <li><a href="<%= request.getContextPath() %>/products?category=footwear" class="nav-link">Footwear</a></li>
                <li><a href="<%= request.getContextPath() %>/products?category=accessories" class="nav-link">Accessories</a></li>
            </ul>
        </nav>

        <div class="nav-right">
            <form class="navbar-search" action="<%= request.getContextPath() %>/products" method="get" role="search">
                <input type="text" name="search" placeholder="Search products..." autocomplete="off">
                <button type="submit" aria-label="Search">
                    <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="7"></circle><line x1="20" y1="20" x2="16.2" y2="16.2"></line></svg>
                </button>
            </form>

            <div class="navbar-actions">
                <button id="dark-mode-toggle" class="navbar-action-btn" aria-label="Toggle theme">
                    <svg class="sun-icon nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="5"></circle><line x1="12" y1="1" x2="12" y2="3"></line><line x1="12" y1="21" x2="12" y2="23"></line><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line><line x1="1" y1="12" x2="3" y2="12"></line><line x1="21" y1="12" x2="23" y2="12"></line><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line></svg>
                </button>
                
                <a href="<%= request.getContextPath() %>/wishlist" class="navbar-action-btn" aria-label="Wishlist">
                    <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.3 5.7a5.1 5.1 0 0 0-7.2 0L12 6.8l-1.1-1.1a5.1 5.1 0 0 0-7.2 7.2L12 21l8.3-8.1a5.1 5.1 0 0 0 0-7.2z"></path></svg>
                </a>

                <button type="button" class="navbar-action-btn cart-toggle" id="cart-toggle" aria-label="Shopping Cart">
                    <svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path><line x1="3" y1="6" x2="21" y2="6"></line><path d="M16 10a4 4 0 0 1-8 0"></path></svg>
                    <span class="nav-badge" id="nav-cart-badge"><%= initialCartCount %></span>
                </button>

                <div class="navbar-account-dropdown">
                    <button class="navbar-action-btn avatar-btn" aria-label="Account Menu">
                        <span class="navbar-avatar"><%= userInitials %></span>
                    </button>
                    <div class="dropdown-menu surface-card">
                        <% if (user != null) { %>
                            <div class="dropdown-header">
                                <span class="avatar-large"><%= userInitials %></span>
                                <div class="user-info">
                                    <span class="user-name"><%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(user.getFullName()) %></span>
                                    <span class="user-email text-muted"><%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(user.getEmail()) %></span>
                                </div>
                            </div>
                            <div class="dropdown-divider"></div>
                            <a href="<%= request.getContextPath() %>/account/profile" class="dropdown-item">My Profile</a>
                            <a href="<%= request.getContextPath() %>/orders" class="dropdown-item">Orders</a>
                            <a href="<%= request.getContextPath() %>/wishlist" class="dropdown-item">Wishlist</a>
                            <div class="dropdown-divider"></div>
                            <a href="<%= request.getContextPath() %>/logout" class="dropdown-item item-danger">Logout</a>
                        <% } else { %>
                            <a href="<%= request.getContextPath() %>/login" class="dropdown-item">Sign in</a>
                            <a href="<%= request.getContextPath() %>/register" class="dropdown-item">Create account</a>
                            <a href="<%= request.getContextPath() %>/orders" class="dropdown-item">Track orders</a>
                        <% } %>
                    </div>
                </div>


            </div>
        </div>
    </div>
</header>

<input type="hidden" id="user-logged-in" value="<%= user != null ? "true" : "false" %>">
<div id="toast-container" class="toast-container"></div>
