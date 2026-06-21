/**
 * Orders Page Module
 * Handles orders page interactions (invoice download, etc.)
 */
const OrdersPage = (function() {
    
    function init() {
        // Register event handlers with centralized event delegation
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('click', '#download-invoice-btn', function(event, target) {
                const orderId = target.closest('.fs-order-card').dataset.orderId;
                if (orderId) {
                    event.preventDefault();
                    window.location.href = window.contextPath + '/orders/' + orderId + '/invoice?download=true';
                }
            });

            EventDelegation.on('click', '#invoice-btn', function(event, target) {
                const orderId = target.closest('.fs-order-card').dataset.orderId;
                if (orderId) {
                    event.preventDefault();
                    window.location.href = window.contextPath + '/orders/' + orderId + '/invoice';
                }
            });
        }
    }
    
    return {
        init: init
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('ordersPage', OrdersPage.init, 35);
}
