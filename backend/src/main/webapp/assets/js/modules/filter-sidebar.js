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
        
        sidebar.classList.add('active');
        overlay.classList.add('active');
        if (btn) btn.setAttribute('aria-expanded', 'true');
        
        // Lock scroll position
        filterScrollY = window.scrollY;
        document.body.style.position = 'fixed';
        document.body.style.top = `-${filterScrollY}px`;
        document.body.style.width = '100%';
    }
    
    function closeFilterSidebar() {
        const sidebar = document.getElementById('filter-sidebar');
        const overlay = document.getElementById('filter-overlay');
        const btn = document.getElementById('filter-toggle-btn');
        
        if (!sidebar || !overlay) {
            console.warn('Filter sidebar elements not found');
            return;
        }
        
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
        if (btn) btn.setAttribute('aria-expanded', 'false');
        
        // Restore scroll position
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.width = '';
        window.scrollTo(0, filterScrollY);
    }
    
    function init() {
        // Make functions available globally for onclick handlers
        window.openFilterSidebar = openFilterSidebar;
        window.closeFilterSidebar = closeFilterSidebar;
        
        // Add event listeners
        const toggleBtn = document.getElementById('filter-toggle-btn');
        const closeBtn = document.getElementById('filter-close-btn');
        const overlay = document.getElementById('filter-overlay');
        
        if (toggleBtn) {
            toggleBtn.addEventListener('click', openFilterSidebar);
        }
        
        if (closeBtn) {
            closeBtn.addEventListener('click', closeFilterSidebar);
        }
        
        if (overlay) {
            overlay.addEventListener('click', closeFilterSidebar);
        }
        
        // Close on escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeFilterSidebar();
            }
        });
    }
    
    return {
        init: init,
        open: openFilterSidebar,
        close: closeFilterSidebar
    };
})();

// Auto-initialize if on products page
if (document.querySelector('.products-page')) {
    FilterSidebar.init();
}
