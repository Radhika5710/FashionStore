/**
 * Scroll Lock Utility
 * Centralized scroll lock management to prevent conflicts
 * between multiple modules (CartManager, navbar, filter-sidebar, etc.)
 */

const ScrollLock = (function() {
    let lockCount = 0;
    let scrollY = 0;
    let isLocked = false;
    
    /**
     * Lock scroll
     * Multiple calls are tracked, scroll only unlocked when count reaches 0
     */
    function lock() {
        lockCount++;
        
        if (lockCount === 1 && !isLocked) {
            scrollY = window.scrollY;
            document.body.style.position = 'fixed';
            document.body.style.top = `-${scrollY}px`;
            document.body.style.width = '100%';
            document.body.style.overflow = 'hidden';
            isLocked = true;
        }
    }
    
    /**
     * Unlock scroll
     * Scroll only unlocked when all locks are released
     */
    function unlock() {
        lockCount--;
        
        if (lockCount <= 0) {
            lockCount = 0;
            
            if (isLocked) {
                document.body.style.position = '';
                document.body.style.top = '';
                document.body.style.width = '';
                document.body.style.overflow = '';
                window.scrollTo(0, scrollY);
                isLocked = false;
            }
        }
    }
    
    /**
     * Force unlock regardless of lock count
     * Use this for cleanup scenarios
     */
    function forceUnlock() {
        lockCount = 0;
        
        if (isLocked) {
            document.body.style.position = '';
            document.body.style.top = '';
            document.body.style.width = '';
            document.body.style.overflow = '';
            window.scrollTo(0, scrollY);
            isLocked = false;
        }
    }
    
    /**
     * Check if scroll is currently locked
     */
    function isScrollLocked() {
        return isLocked;
    }
    
    // Public API
    return {
        lock,
        unlock,
        forceUnlock,
        isScrollLocked
    };
})();

// Make available globally
window.ScrollLock = ScrollLock;

// Cleanup on page navigation to prevent scroll lock state bleeding
window.addEventListener('beforeunload', () => {
    ScrollLock.forceUnlock();
});

// Also cleanup on pagehide for better SPA-like behavior
window.addEventListener('pagehide', () => {
    ScrollLock.forceUnlock();
});

// Export for ES6 modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ScrollLock;
}
