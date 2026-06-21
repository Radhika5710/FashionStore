/**
 * Filter Sidebar Module
 * Handles product filter sidebar UI interactions only
 * 
 * REFACTORED FOR MVC ARCHITECTURE:
 * - No filtering logic (backend handles all filtering)
 * - Only manages sidebar UI state (open/close)
 * - Filter form submission goes to backend
 * - Backend returns filtered products via ProductController
 */
const FilterSidebar = (function() {
    
    let filterScrollY = 0;
    
    function openFilterSidebar() {
        const sidebar = document.getElementById('filter-sidebar');
        const overlay = document.getElementById('filter-overlay');
        const btn = document.getElementById('filter-toggle-btn');
        
        if (!sidebar || !overlay) {
            console.warn('Filter sidebar elements not found');
            return;
        }
        
        // Use OverlayManager for centralized overlay management
        if (typeof OverlayManager !== 'undefined') {
            // Register overlay if not already registered
            if (!overlay.dataset.overlayId) {
                overlay.dataset.overlayId = 'filter-sidebar';
            }
            if (!sidebar.dataset.overlayContent) {
                sidebar.dataset.overlayContent = 'filter-sidebar';
            }
            OverlayManager.openOverlay('filter-sidebar', overlay, sidebar);
            if (btn) btn.setAttribute('aria-expanded', 'true');
        } else {
            // Fallback to direct manipulation
            sidebar.classList.add('active');
            overlay.classList.add('active');
            if (btn) btn.setAttribute('aria-expanded', 'true');
            
            // Use centralized scroll lock
            if (typeof ScrollLock !== 'undefined') {
                ScrollLock.lock();
            } else {
                // Fallback to local implementation
                filterScrollY = window.scrollY;
                document.body.style.position = 'fixed';
                document.body.style.top = `-${filterScrollY}px`;
                document.body.style.width = '100%';
            }
        }
    }
    
    function closeFilterSidebar() {
        const sidebar = document.getElementById('filter-sidebar');
        const overlay = document.getElementById('filter-overlay');
        const btn = document.getElementById('filter-toggle-btn');
        
        if (!sidebar || !overlay) {
            console.warn('Filter sidebar elements not found');
            return;
        }
        
        // Use OverlayManager for centralized overlay management
        if (typeof OverlayManager !== 'undefined') {
            OverlayManager.closeOverlay('filter-sidebar');
            if (btn) btn.setAttribute('aria-expanded', 'false');
        } else {
            // Fallback to direct manipulation
            sidebar.classList.remove('active');
            overlay.classList.remove('active');
            if (btn) btn.setAttribute('aria-expanded', 'false');
            
            // Use centralized scroll unlock
            if (typeof ScrollLock !== 'undefined') {
                ScrollLock.unlock();
            } else {
                // Fallback to local implementation
                document.body.style.position = '';
                document.body.style.top = '';
                document.body.style.width = '';
                window.scrollTo(0, filterScrollY);
            }
        }
    }
    
    function init() {
        // Register event handlers with centralized event delegation
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('click', '#filter-toggle-btn', function(event) {
                event.preventDefault();
                openFilterSidebar();
            });

            EventDelegation.on('click', '#filter-close-btn', function(event) {
                event.preventDefault();
                closeFilterSidebar();
            });

            EventDelegation.on('click', '#filter-overlay', function(event) {
                event.preventDefault();
                closeFilterSidebar();
            });

            // Close on escape key
            EventDelegation.on('keydown', document, function(event) {
                if (event.key === 'Escape') {
                    closeFilterSidebar();
                }
            });
        }
    }
    
    function cleanup() {
        // Event delegation handles cleanup automatically
        // Reset scroll position if locked
        if (typeof ScrollLock !== 'undefined') {
            ScrollLock.forceUnlock();
        } else {
            document.body.style.position = '';
            document.body.style.top = '';
            document.body.style.width = '';
            window.scrollTo(0, filterScrollY);
        }
    }
    
    return {
        init: init,
        open: openFilterSidebar,
        close: closeFilterSidebar
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('filterSidebar', () => {
        if (document.querySelector('.products-page')) {
            FilterSidebar.init();
        }
    }, 15);
}
