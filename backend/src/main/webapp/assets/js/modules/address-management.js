/**
 * Address Management Module
 * Handles address management functionality with state management
 */
const AddressManagement = (function() {
    
    function setDefaultAddress(addressId) {
        if (!confirm('Set this address as default?')) {
            return;
        }
        
        // Show loading state on the button
        const btn = document.querySelector(`[data-address-id="${addressId}"].set-default-btn`);
        if (btn) {
            btn.classList.add('loading');
            if (typeof StateManager !== 'undefined') {
                StateManager.setButtonLoading('set-default-btn-' + addressId, true);
            }
        }
        
        const formData = new URLSearchParams();
        formData.append('action', 'setDefault');
        formData.append('addressId', addressId);
        formData.append('csrfToken', window.csrfToken || '');
        
        fetch(window.contextPath + '/account/addresses', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: formData
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                FashionStore.showToast(data.message, 'success');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                FashionStore.showToast(data.message, 'error');
            }
        })
        .catch(err => {
            console.error('Error setting default address:', err);
            FashionStore.showToast('Failed to set default address. Please try again.', 'error');
        })
        .finally(() => {
            if (btn) {
                btn.classList.remove('loading');
                if (typeof StateManager !== 'undefined') {
                    StateManager.setButtonLoading('set-default-btn-' + addressId, false);
                }
            }
        });
    }
    
    function deleteAddress(addressId) {
        if (!confirm('Are you sure you want to delete this address?')) {
            return;
        }
        
        // Show loading state on the button
        const btn = document.querySelector(`[data-address-id="${addressId}"].delete-address-btn`);
        if (btn) {
            btn.classList.add('loading');
            if (typeof StateManager !== 'undefined') {
                StateManager.setButtonLoading('delete-address-btn-' + addressId, true);
            }
        }
        
        const formData = new URLSearchParams();
        formData.append('action', 'delete');
        formData.append('addressId', addressId);
        formData.append('csrfToken', window.csrfToken || '');
        
        fetch(window.contextPath + '/account/addresses', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-Token': window.csrfToken || ''
            },
            body: formData
        })
        .then(res => res.json())
        .then(data => {
            if (data.success) {
                FashionStore.showToast(data.message, 'success');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                FashionStore.showToast(data.message, 'error');
            }
        })
        .catch(err => {
            console.error('Error deleting address:', err);
            FashionStore.showToast('Failed to delete address. Please try again.', 'error');
        })
        .finally(() => {
            if (btn) {
                btn.classList.remove('loading');
                if (typeof StateManager !== 'undefined') {
                    StateManager.setButtonLoading('delete-address-btn-' + addressId, false);
                }
            }
        });
    }
    
    function init() {
        // Register event handlers with centralized event delegation
        if (typeof EventDelegation !== 'undefined') {
            EventDelegation.on('click', '.set-default-btn', function(event, target) {
                const addressId = target.dataset.addressId;
                if (addressId) {
                    event.preventDefault();
                    setDefaultAddress(addressId);
                }
            });

            EventDelegation.on('click', '.delete-address-btn', function(event, target) {
                const addressId = target.dataset.addressId;
                if (addressId) {
                    event.preventDefault();
                    deleteAddress(addressId);
                }
            });
        }
        
        // Check if address list is empty and show empty state
        const addressList = document.querySelector('.address-list');
        if (addressList && !addressList.children.length) {
            if (typeof StateManager !== 'undefined') {
                StateManager.showEmpty('address-list', {
                    emptyTitle: 'No addresses saved',
                    emptyMessage: 'Add a new address to get started',
                    actionText: 'Add Address',
                    onAction: function() { window.location.href = window.contextPath + '/account/addresses/new'; }
                });
            }
        }
    }
    
    return {
        init: init,
        setDefaultAddress: setDefaultAddress,
        deleteAddress: deleteAddress
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('addressManagement', AddressManagement.init, 30);
}
