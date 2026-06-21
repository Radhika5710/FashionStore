/**
 * FashionStore - Navbar Module
 * Navbar interactions, scroll behavior, and mobile menu
 * Conditional rendering for mobile navigation (not CSS-only hiding)
 */

const FashionStoreNavbar = (function() {
    let lastScrollY = 0;
    let isScrollingDown = false;
    let mobileNavElement = null;
    let navbarElement = null;
    
    // Track event listeners for proper cleanup
    const eventListeners = [];
    
    // Mobile navigation template (only rendered on mobile)
    const mobileNavTemplate = `
<nav class="fs-mobile-nav" aria-label="Mobile navigation">
    <a href="${window.contextPath}/home" class="fs-mobile-nav__item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
            <polyline points="9 22 9 12 15 12 15 22"></polyline>
        </svg>
        <span>Home</span>
    </a>
    <a href="${window.contextPath}/products" class="fs-mobile-nav__item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
            <line x1="7" y1="7" x2="7.01" y2="7"></line>
        </svg>
        <span>Shop</span>
    </a>
    <a href="${window.contextPath}/cart" class="fs-mobile-nav__item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"></path>
            <line x1="3" y1="6" x2="21" y2="6"></line>
            <path d="M16 10a4 4 0 0 1-8 0"></path>
        </svg>
        <span>Cart</span>
        <span class="fs-mobile-nav__badge" id="mobile-cart-badge">0</span>
    </a>
    <a href="${window.contextPath}/wishlist" class="fs-mobile-nav__item">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.3 5.7a5.1 5.1 0 0 0-7.2 0L12 6.8l-1.1-1.1a5.1 5.1 0 0 0-7.2 7.2L12 21l8.3-8.1a5.1 5.1 0 0 0 0-7.2z"></path>
        </svg>
        <span>Wishlist</span>
    </a>
</nav>`;
    
    // Responsive hook - similar to React's useIsMobile()
    function useIsMobile() {
        return window.innerWidth <= 768;
    }
    
    function init() {
        setupScrollBehavior();
        setupMobileMenu();
        setupCartDrawerToggle();
        setupConditionalMobileNav();
    }
    
    // Conditional rendering for mobile navigation (not CSS-only hiding)
    function setupConditionalMobileNav() {
        // Debounce resize handler to prevent excessive calls
        let resizeTimeout;
        const debouncedHandleViewportChange = () => {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(handleViewportChange, 150);
        };
        
        function handleViewportChange() {
            const isMobile = useIsMobile();
            
            if (isMobile) {
                // Render mobile nav only on mobile (< 768px)
                if (!mobileNavElement) {
                    mobileNavElement = createMobileNav();
                    document.body.appendChild(mobileNavElement);
                    updateMobileCartBadge();
                }
            } else {
                // Remove mobile nav from DOM on desktop (> 768px)
                if (mobileNavElement && mobileNavElement.parentNode) {
                    mobileNavElement.parentNode.removeChild(mobileNavElement);
                    mobileNavElement = null;
                }
            }
        }
        
        // Initial check
        handleViewportChange();
        
        // Listen to resize events with debouncing
        window.addEventListener('resize', debouncedHandleViewportChange);
        eventListeners.push({ target: window, event: 'resize', handler: debouncedHandleViewportChange });
    }
    
    function createMobileNav() {
        const container = document.createElement('div');
        container.innerHTML = mobileNavTemplate;
        const navElement = container.firstElementChild;
        
        // Add account link based on login state
        const isLoggedIn = document.getElementById('user-logged-in')?.value === 'true';
        const accountLink = isLoggedIn 
            ? `<a href="${window.contextPath}/account/profile" class="fs-mobile-nav__item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span>Account</span>
            </a>`
            : `<a href="${window.contextPath}/login" class="fs-mobile-nav__item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                    <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <span>Login</span>
            </a>`;
        
        navElement.insertAdjacentHTML('beforeend', accountLink);
        
        return navElement;
    }
    
    function updateMobileCartBadge() {
        const navBadge = document.getElementById('nav-cart-badge');
        const mobileBadge = document.getElementById('mobile-cart-badge');
        
        if (navBadge && mobileBadge) {
            mobileBadge.textContent = navBadge.textContent;
        }
    }
    
    function setupScrollBehavior() {
        // Prefer the actual .navbar header; fall back to legacy .fs-storefront-nav
        navbarElement = document.querySelector('.navbar') || document.querySelector('.fs-storefront-nav');
        if (!navbarElement) return;

        // Determine which scrolled class to toggle based on which element we found
        const scrolledClass = navbarElement.classList.contains('navbar')
            ? 'navbar--scrolled'
            : 'fs-storefront-nav--scrolled';

        const handleScroll = () => {
            const currentScrollY = window.scrollY;

            // Detect scroll direction
            isScrollingDown = currentScrollY > lastScrollY;
            lastScrollY = currentScrollY;

            // Add scrolled state for visual feedback (shadow + blur)
            if (currentScrollY > 10) {
                navbarElement.classList.add(scrolledClass);
                // Keep legacy class in sync if element has both
                navbarElement.classList.add('fs-storefront-nav--scrolled');
            } else {
                navbarElement.classList.remove(scrolledClass);
                navbarElement.classList.remove('fs-storefront-nav--scrolled');
            }

            // Hide/show navbar based on scroll direction
            if (currentScrollY > 100) {
                if (isScrollingDown) {
                    navbarElement.classList.add('navbar-hidden');
                } else {
                    navbarElement.classList.remove('navbar-hidden');
                }
            } else {
                navbarElement.classList.remove('navbar-hidden');
            }
        };

        window.addEventListener('scroll', handleScroll, { passive: true });

        // Track listener for cleanup
        eventListeners.push({ target: window, event: 'scroll', handler: handleScroll });
    }
    
    function setupMobileMenu() {
        const mobileMenuBtn = document.getElementById('mobile-menu-btn');
        const mobileNavOverlay = document.getElementById('mobile-nav-overlay');
        
        if (!mobileMenuBtn || !mobileNavOverlay) return;
        
        // Register event handlers with centralized event delegation
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('click', '#mobile-menu-btn', function() {
                const isExpanded = mobileMenuBtn.getAttribute('aria-expanded') === 'true';
                if (isExpanded) {
                    closeMobileMenu();
                } else {
                    // Use OverlayManager for centralized overlay management
                    if (typeof OverlayManager !== 'undefined') {
                        // Register overlay if not already registered
                        if (!mobileNavOverlay.dataset.overlayId) {
                            mobileNavOverlay.dataset.overlayId = 'mobile-nav';
                        }
                        const mobileNavContent = document.querySelector('.mobile-nav-content');
                        if (mobileNavContent && !mobileNavContent.dataset.overlayContent) {
                            mobileNavContent.dataset.overlayContent = 'mobile-nav';
                        }
                        OverlayManager.openOverlay('mobile-nav', mobileNavOverlay, mobileNavContent);
                        mobileMenuBtn.setAttribute('aria-expanded', 'true');
                    } else {
                        // Fallback to direct manipulation
                        mobileMenuBtn.setAttribute('aria-expanded', 'true');
                        mobileNavOverlay.classList.add('active');
                        document.body.classList.add('nav-drawer-open');
                        lockScroll();
                    }
                }
            });

            EventDelegation.on('click', '#mobile-nav-overlay', function(event, target) {
                if (event.target === target) {
                    closeMobileMenu();
                }
            });

            // Close menu when a navigation link is clicked
            EventDelegation.on('click', '.fs-storefront-nav a', function() {
                const mobileMenuBtn = document.getElementById('mobile-menu-btn');
                if (mobileMenuBtn && mobileMenuBtn.getAttribute('aria-expanded') === 'true') {
                    closeMobileMenu();
                }
            });

            // Close menu on escape key
            EventDelegation.on('keydown', document, function(event) {
                if (event.key === 'Escape') {
                    const mobileMenuBtn = document.getElementById('mobile-menu-btn');
                    if (mobileMenuBtn && mobileMenuBtn.getAttribute('aria-expanded') === 'true') {
                        closeMobileMenu();
                    }
                }
            });
        }
    }
    
    function closeMobileMenu() {
        const mobileMenuBtn = document.getElementById('mobile-menu-btn');
        const mobileNavOverlay = document.getElementById('mobile-nav-overlay');
        
        // Use OverlayManager for centralized overlay management
        if (typeof OverlayManager !== 'undefined') {
            OverlayManager.closeOverlay('mobile-nav');
            if (mobileMenuBtn) {
                mobileMenuBtn.setAttribute('aria-expanded', 'false');
            }
        } else {
            // Fallback to direct manipulation
            if (mobileMenuBtn) {
                mobileMenuBtn.setAttribute('aria-expanded', 'false');
            }
            if (mobileNavOverlay) {
                mobileNavOverlay.classList.remove('active');
            }
            document.body.classList.remove('nav-drawer-open');
            unlockScroll();
        }
    }
    
    function setupCartDrawerToggle() {
        // Cart drawer toggle is handled by cart-drawer.js
        // This is a placeholder for any navbar-specific cart interactions
    }
    
    function lockScroll() {
        if (typeof ScrollLock !== 'undefined') {
            ScrollLock.lock();
        } else {
            // Fallback to local implementation
            const scrollY = window.scrollY;
            document.body.style.position = 'fixed';
            document.body.style.top = `-${scrollY}px`;
            document.body.style.width = '100%';
        }
    }
    
    function unlockScroll() {
        if (typeof ScrollLock !== 'undefined') {
            ScrollLock.unlock();
        } else {
            // Fallback to local implementation
            const scrollY = document.body.style.top;
            document.body.style.position = '';
            document.body.style.top = '';
            document.body.style.width = '';
            window.scrollTo(0, parseInt(scrollY || '0') * -1);
        }
    }
    
    function cleanup() {
        // Remove all tracked event listeners
        eventListeners.forEach(({ target, event, handler }) => {
            if (target && handler) {
                target.removeEventListener(event, handler);
            }
        });
        
        // Clear the listeners array
        eventListeners.length = 0;
    }
    
    // Public API
    return {
        init,
        closeMobileMenu,
        lockScroll,
        unlockScroll,
        cleanup
    };
})();

// Make navbar available globally
if (typeof window.FashionStore === 'undefined') {
    window.FashionStore = {};
}
window.FashionStore.navbar = FashionStoreNavbar;

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('navbar', FashionStoreNavbar.init, 5);
}

// Cleanup on page navigation to prevent memory leaks
window.addEventListener('beforeunload', () => {
    FashionStoreNavbar.cleanup();
});

window.addEventListener('pagehide', () => {
    FashionStoreNavbar.cleanup();
});

// Export for ES6 modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FashionStoreNavbar;
}
