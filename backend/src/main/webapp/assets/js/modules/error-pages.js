/**
 * Error Pages Module
 * Handles error page interactions (back button, account settings, etc.)
 */
const ErrorPages = (function() {
    
    function init() {
        // Register event handlers with centralized event delegation
        if (typeof EventDelegation !== 'undefined') {
            // Error page back button
            EventDelegation.on('click', '#back-btn', function(event) {
                event.preventDefault();
                history.back();
            });

            // Account settings buttons
            EventDelegation.on('click', '#deactivate-account-btn', function(event) {
                event.preventDefault();
                if (confirm('Are you sure you want to deactivate your account? You can reactivate it later.')) {
                    window.location.href = window.contextPath + '/account/settings?action=deactivate';
                }
            });

            EventDelegation.on('click', '#delete-account-btn', function(event) {
                event.preventDefault();
                if (confirm('Are you sure you want to permanently delete your account? This action cannot be undone.')) {
                    window.location.href = window.contextPath + '/account/settings?action=delete';
                }
            });
        }

        // Format timestamps on 404 page
        const timestampElements = document.querySelectorAll('.timestamp');
        timestampElements.forEach(function(element) {
            const timestamp = parseInt(element.textContent);
            if (!isNaN(timestamp)) {
                const date = new Date(timestamp);
                element.textContent = date.toLocaleString();
            }
        });
    }
    
    return {
        init: init
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('errorPages', ErrorPages.init, 40);
}
