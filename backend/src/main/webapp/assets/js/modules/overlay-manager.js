/**
 * Centralized Overlay Manager
 * Manages all overlays (cart drawer, mobile nav, filter sidebar, etc.)
 * Prevents overlay conflicts, flickering, and scroll lock issues
 */
const OverlayManager = (function() {
    const activeOverlays = new Set();
    
    /**
     * Register an overlay
     * @param {string} overlayId - Unique overlay identifier
     * @param {HTMLElement} overlayElement - Overlay DOM element
     * @param {HTMLElement} contentElement - Content DOM element (drawer/sidebar)
     * @param {Function} onOpen - Callback when overlay opens
     * @param {Function} onClose - Callback when overlay closes
     */
    function registerOverlay(overlayId, overlayElement, contentElement, onOpen, onClose) {
        if (!overlayElement) return;
        
        // Store overlay data
        overlayElement.dataset.overlayId = overlayId;
        
        // Register event delegation for this overlay
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('click', `[data-overlay-id="${overlayId}"]`, function(event, target) {
                if (event.target === target) {
                    closeOverlay(overlayId);
                }
            });
        }
    }
    
    /**
     * Open an overlay
     * @param {string} overlayId - Overlay identifier
     * @param {HTMLElement} overlayElement - Overlay DOM element
     * @param {HTMLElement} contentElement - Content DOM element (optional)
     */
    function openOverlay(overlayId, overlayElement, contentElement) {
        // Close all other active overlays first
        activeOverlays.forEach(id => {
            if (id !== overlayId) {
                closeOverlay(id);
            }
        });
        
        // Add this overlay to active set
        activeOverlays.add(overlayId);
        
        // Show overlay
        if (overlayElement) {
            overlayElement.classList.add('active');
        }
        
        // Show content if provided
        if (contentElement) {
            contentElement.classList.add('active');
        }
        
        // Lock scroll
        if (typeof ScrollLock !== 'undefined') {
            ScrollLock.lock();
        }
    }
    
    /**
     * Close an overlay
     * @param {string} overlayId - Overlay identifier
     */
    function closeOverlay(overlayId) {
        // Find overlay element
        const overlayElement = document.querySelector(`[data-overlay-id="${overlayId}"]`);
        const contentElement = document.querySelector(`[data-overlay-content="${overlayId}"]`);
        
        // Remove from active set
        activeOverlays.delete(overlayId);
        
        // Hide overlay
        if (overlayElement) {
            overlayElement.classList.remove('active');
        }
        
        // Hide content
        if (contentElement) {
            contentElement.classList.remove('active');
        }
        
        // Unlock scroll if no overlays are active
        if (activeOverlays.size === 0) {
            if (typeof ScrollLock !== 'undefined') {
                ScrollLock.unlock();
            }
        }
    }
    
    /**
     * Close all overlays
     */
    function closeAllOverlays() {
        activeOverlays.forEach(id => closeOverlay(id));
    }
    
    /**
     * Check if an overlay is active
     * @param {string} overlayId - Overlay identifier
     * @returns {boolean}
     */
    function isOverlayActive(overlayId) {
        return activeOverlays.has(overlayId);
    }
    
    /**
     * Get active overlays
     * @returns {Set<string>}
     */
    function getActiveOverlays() {
        return new Set(activeOverlays);
    }
    
    function init() {
        // Register event delegation for closing overlays on escape
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('keydown', document, function(event) {
                if (event.key === 'Escape') {
                    closeAllOverlays();
                }
            });
        }
        
        // Cleanup on page navigation
        window.addEventListener('beforeunload', closeAllOverlays);
        window.addEventListener('pagehide', closeAllOverlays);
    }
    
    return {
        init,
        registerOverlay,
        openOverlay,
        closeOverlay,
        closeAllOverlays,
        isOverlayActive,
        getActiveOverlays
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('overlayManager', OverlayManager.init, 5); // Initialize early (priority 5)
}
