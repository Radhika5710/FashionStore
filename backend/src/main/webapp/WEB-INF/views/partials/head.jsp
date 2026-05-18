<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%-- 
    head.jsp: Shared meta tags and font loading.
    USAGE: Call request.setAttribute("_pageTitle","...") and request.setAttribute("_pageCSS","css-name")
    before including this file. Do NOT put a page contentType directive here.
--%>
<%
    String _pageTitle = (String) request.getAttribute("_pageTitle");
    if (_pageTitle == null || _pageTitle.trim().isEmpty()) _pageTitle = "FashionStore";
    String _pageDescription = (String) request.getAttribute("_pageDescription");
    if (_pageDescription == null || _pageDescription.trim().isEmpty()) _pageDescription = "FashionStore - premium fashion marketplace with curated styles for every season.";
    String _canonical = (String) request.getAttribute("_canonical");
    String _pageCSS   = (String) request.getAttribute("_pageCSS");
%>
<meta charset="UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
<meta name="description" content="<%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(_pageDescription) %>">

<%-- Skip to content link for keyboard accessibility --%>
<style nonce="<%= request.getAttribute("cspNonce") %>">
    .skip-to-content {
        position: absolute;
        top: -40px;
        left: 0;
        background: var(--color-ink);
        color: var(--color-surface);
        padding: 8px 16px;
        z-index: 10000;
        text-decoration: none;
        transition: top 0.3s;
    }
    .skip-to-content:focus {
        top: 0;
    }
</style>
<a href="#main-content" class="skip-to-content">Skip to main content</a>

<% if (_canonical != null && !_canonical.trim().isEmpty()) { %>
<link rel="canonical" href="<%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(_canonical) %>">
<% } %>
<title><%= _pageTitle %> | FashionStore</title>

<%-- Open Graph / Social Sharing --%>
<meta property="og:title" content="<%= _pageTitle %> | FashionStore">
<meta property="og:description" content="<%= org.apache.commons.text.StringEscapeUtils.escapeHtml4(_pageDescription) %>">
<meta property="og:type" content="website">
<meta property="og:url" content="<%= request.getRequestURL() %>">
<meta property="og:image" content="<%= request.getContextPath() %>/assets/images/logo-mark.svg">

<%-- Favicon & Brand Icons --%>
<link rel="icon" type="image/svg+xml" href="<%= request.getContextPath() %>/assets/images/logo-mark.svg">
<link rel="apple-touch-icon" href="<%= request.getContextPath() %>/assets/images/logo-mark.svg">

<%-- Google Fonts: Optimized loading with display=swap --%>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="preload" href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,400;0,500;0,600;0,700;1,400;1,500&family=Inter:wght@300;400;500;600;700&display=swap" as="style">
<link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,400;0,500;0,600;0,700;1,400;1,500&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">

<%-- Preload critical CSS for faster rendering --%>
<link rel="preload" href="<%= request.getContextPath() %>/assets/css/design-tokens.css" as="style">
<link rel="preload" href="<%= request.getContextPath() %>/assets/css/reset.css" as="style">
<link rel="preload" href="<%= request.getContextPath() %>/assets/css/base.css" as="style">
<link rel="preload" href="<%= request.getContextPath() %>/assets/css/layout.css" as="style">
<link rel="preload" href="<%= request.getContextPath() %>/assets/css/main.css" as="style">

<%-- 1. CRITICAL CSS (loads synchronously) --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/design-tokens.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/reset.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/base.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/layout.css">

<%-- 2. COMPONENT CSS (Consolidated) --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/buttons.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/forms.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/navbar.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/footer.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/skeleton.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/loading-states.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/components/mini-cart.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/toast-premium.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/main.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/premium-core.css">

<%-- DEMO HARDENING MODE CSS (loads last to override all animations) --%>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/demo-hardening.css">

<%-- DNS prefetch for external resources --%>
<link rel="dns-prefetch" href="//fonts.googleapis.com">
<link rel="dns-prefetch" href="//fonts.gstatic.com">

<%-- Global context and CSRF must be set before any external script reads them --%>
<script nonce="<%= request.getAttribute("cspNonce") %>">
    window.contextPath = '<%= request.getContextPath() %>';
    window.csrfToken = '<%= request.getAttribute("csrfToken") != null ? org.apache.commons.text.StringEscapeUtils.escapeEcmaScript(request.getAttribute("csrfToken").toString()) : "" %>';
</script>

<%-- Critical JavaScript (inline for performance) --%>
<script nonce="<%= request.getAttribute("cspNonce") %>">
    // Theme initialization MUST run before CSS loads to prevent flicker
    (function() {
        const STORAGE_KEY = 'fashionstore-theme';
        const THEME_ATTR = 'data-theme';
        const DARK_CLASS = 'dark-mode';
        
        // Check localStorage first
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored === 'dark') {
            document.documentElement.setAttribute(THEME_ATTR, 'dark');
            document.documentElement.classList.add(DARK_CLASS);
        } else if (stored === 'light') {
            document.documentElement.removeAttribute(THEME_ATTR);
            document.documentElement.classList.remove(DARK_CLASS);
        } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            // Fall back to system preference
            document.documentElement.setAttribute(THEME_ATTR, 'dark');
            document.documentElement.classList.add(DARK_CLASS);
        }
    })();
    
    // Splash screen - must run before page renders
    document.documentElement.classList.add('splash-active');
    window.addEventListener('load', function() {
        document.documentElement.classList.remove('splash-active');
    });
</script>

<%-- Modular JavaScript - deferred for non-blocking load --%>
<%-- Core utilities (must load first) --%>
<script src="<%= request.getContextPath() %>/assets/js/modules/utilities.js" defer></script>

<%-- API client (must load before modules that use it) --%>
<script src="<%= request.getContextPath() %>/assets/js/api/client.js" defer></script>

<%-- State manager (used by search, checkout, product-interactions) --%>
<script src="<%= request.getContextPath() %>/assets/js/modules/state-manager.js" defer></script>

<%-- Theme module --%>
<script src="<%= request.getContextPath() %>/assets/js/modules/theme.js" defer></script>

<%-- Navbar module --%>
<script src="<%= request.getContextPath() %>/assets/js/modules/navbar.js" defer></script>

<%-- Search module --%>
<script src="<%= request.getContextPath() %>/assets/js/modules/search.js" defer></script>

<%-- Product interactions module --%>
<% if (_pageCSS == null || !_pageCSS.contains("auth")) { %>
<script src="<%= request.getContextPath() %>/assets/js/modules/product-interactions.js" defer></script>
<% } %>

<%-- CartManager.js - Consolidated cart operations - skip on auth pages to reduce loading --%>
<% if (_pageCSS == null || !_pageCSS.contains("auth")) { %>
<script src="<%= request.getContextPath() %>/assets/js/managers/CartManager.js" defer></script>
<% } %>

<%-- Lazy-loading.js - skip on auth pages to reduce loading --%>
<% if (_pageCSS == null || !_pageCSS.contains("auth")) { %>
<script src="<%= request.getContextPath() %>/assets/js/lazy-loading.js" defer></script>
<% } %>

<%-- 3. PAGE CSS (Synchronous – page CSS is render-critical) --%>
<% if (_pageCSS != null && !_pageCSS.trim().isEmpty()) {
       for (String _css : _pageCSS.split(",")) {
           _css = _css.trim();
           if (!_css.isEmpty()) { %>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/<%= ("account".equals(_css) || "admin".equals(_css)) ? _css + ".css" : "pages/" + _css + ".css" %>">
<%         }
       }
   } %>
