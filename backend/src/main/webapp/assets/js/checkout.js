/**
 * FashionStore - Checkout Page JavaScript
 * 
 * REFACTORED FOR MVC ARCHITECTURE:
 * - NO order total calculations (backend provides)
 * - NO payment amount calculations (backend provides)
 * - NO discount calculations (backend provides)
 * - NO tax calculations (backend provides)
 * - Only handles UI interactions and AJAX triggers
 * - Only displays backend-calculated totals
 * - Prevents frontend order/payment tampering
 * 
 * Responsibilities:
 * - Address selection UI
 * - Payment method selection UI
 * - Form validation (required fields, format)
 * - Stripe payment element initialization
 * - AJAX submission to backend
 * - Error display and loading states
 */

const Checkout = (function() {
    const contextPath = window.contextPath || '';
    const stripePublishableKey = window.stripePublishableKey || '';
    const userEmail = window.userEmail || '';
    
    let checkoutSubmissionInProgress = false;
    let checkoutIdempotencyKey = null;
    let stripe = null;
    let elements = null;
    let cardElement = null;
    
    /**
     * Initialize checkout page
     */
    function init() {
        initAddressSelection();
        initPaymentMethodSelection();
        initFormValidation();
        
        // Initialize Stripe if key is available
        if (stripePublishableKey && stripePublishableKey.trim() !== '') {
            initStripeElements();
        } else {
            console.warn('Stripe publishable key not configured - Stripe payments disabled');
            disableStripePayment();
        }
    }
    
    /**
     * Initialize address selection UI
     */
    function initAddressSelection() {
        const radios = document.querySelectorAll('input[name="shippingAddressId"]');
        const newAddressForm = document.getElementById('newAddressForm');
        const newAddressInputs = newAddressForm
            ? newAddressForm.querySelectorAll('input[name="fullName"], input[name="address"], input[name="city"], input[name="state"], input[name="zip"], input[name="phone"]')
            : [];
        
        function syncCardActive() {
            document.querySelectorAll('.fs-address-card').forEach(card => {
                const input = card.querySelector('input[type="radio"]');
                card.classList.toggle('fs-address-card--active', !!(input && input.checked));
            });
        }
        
        function toggleAddressForm() {
            const selected = document.querySelector('input[name="shippingAddressId"]:checked');
            const useNew = !selected || selected.value === 'new' || selected.value === '';
            if (newAddressForm) {
                newAddressForm.style.display = useNew ? 'block' : 'none';
                newAddressForm.classList.toggle('fs-new-address-form--visible', useNew);
            }
            newAddressInputs.forEach(inp => {
                if (useNew) {
                    inp.setAttribute('required', 'required');
                } else {
                    inp.removeAttribute('required');
                }
            });
            syncCardActive();
        }
        
        radios.forEach(radio => radio.addEventListener('change', toggleAddressForm));
        toggleAddressForm();
    }
    
    /**
     * Initialize payment method selection
     */
    function initPaymentMethodSelection() {
        const paymentMethodRadios = document.querySelectorAll('input[name="paymentMethod"]');
        const stripeContainer = document.getElementById('stripe-payment-element-container');
        
        paymentMethodRadios.forEach(radio => {
            radio.addEventListener('change', function() {
                if (this.value === 'STRIPE') {
                    stripeContainer.style.display = 'block';
                    if (stripePublishableKey && stripePublishableKey.trim() !== '') {
                        initStripeElements();
                    }
                } else {
                    stripeContainer.style.display = 'none';
                }
            });
        });
    }
    
    /**
     * Initialize form validation and submission
     */
    function initFormValidation() {
        const form = document.getElementById('checkoutForm');
        if (!form) return;
        
        form.addEventListener('submit', function(e) {
            const errorEl = document.getElementById('form-error');
            const selectedAddress = document.querySelector('input[name="shippingAddressId"]:checked');
            const selectedPaymentMethod = document.querySelector('input[name="paymentMethod"]:checked');
            
            // Prevent duplicate submission
            if (checkoutSubmissionInProgress) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.textContent = 'Order is being processed. Please wait...';
                    errorEl.classList.add('is-visible');
                }
                return;
            }
            
            const usingNew = !selectedAddress || selectedAddress.value === '' || selectedAddress.value === 'new';
            
            // Validate payment method is selected
            if (!selectedPaymentMethod) {
                e.preventDefault();
                if (errorEl) {
                    errorEl.textContent = 'Please select a payment method';
                    errorEl.classList.add('is-visible');
                }
                return;
            }
            
            // If using saved address, skip validation for new address fields
            if (!usingNew) {
                if (errorEl) errorEl.classList.remove('is-visible');
                checkoutSubmissionInProgress = true;
                checkoutIdempotencyKey = generateIdempotencyKey();
                const btn = form.querySelector('.place-order-btn');
                if (btn) {
                    btn.disabled = true;
                    btn.innerHTML = '<span class="spinner"></span> Processing...';
                }
                return;
            }
            
            // Validate new address fields
            const requiredFields = ['fullName', 'address', 'city', 'state', 'zip', 'phone'];
            
            for (const fieldName of requiredFields) {
                const input = form.querySelector('[name="' + fieldName + '"]');
                if (!input || !input.value || !input.value.trim()) {
                    e.preventDefault();
                    if (errorEl) {
                        const pretty = fieldName.replace(/([A-Z])/g, ' $1').toLowerCase();
                        errorEl.textContent = 'Please fill in the ' + pretty;
                        errorEl.classList.add('is-visible');
                    }
                    if (input) input.focus();
                    return;
                }
            }
            
            if (errorEl) errorEl.classList.remove('is-visible');
            
            checkoutSubmissionInProgress = true;
            checkoutIdempotencyKey = generateIdempotencyKey();
            const btn = form.querySelector('button[type="submit"]');
            if (btn) {
                btn.disabled = true;
                btn.innerHTML = '<span class="spinner"></span> Processing...';
            }
        });
    }
    
    /**
     * Generate unique idempotency key for duplicate prevention
     */
    function generateIdempotencyKey() {
        return 'checkout_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }
    
    /**
     * Disable Stripe payment option if key not configured
     */
    function disableStripePayment() {
        const stripeRadio = document.querySelector('input[name="paymentMethod"][value="STRIPE"]');
        if (stripeRadio) {
            stripeRadio.disabled = true;
            stripeRadio.parentElement.classList.add('disabled');
            const label = stripeRadio.parentElement.querySelector('.fs-payment-method__name');
            if (label) {
                label.textContent = 'Digital Transaction (Not Available)';
            }
        }
    }
    
    /**
     * Initialize Stripe Elements
     */
    function initStripeElements() {
        if (stripe) return; // Already initialized
        
        if (!stripePublishableKey || stripePublishableKey.trim() === '') {
            console.error('Stripe publishable key not configured');
            disableStripePayment();
            return;
        }
        
        stripe = Stripe(stripePublishableKey);
        
        elements = stripe.elements({
            appearance: {
                theme: 'stripe',
                variables: {
                    colorPrimary: '#101010',
                    colorBackground: '#ffffff',
                    colorText: '#101010',
                }
            }
        });
        
        cardElement = elements.create('card', {
            style: {
                base: {
                    fontSize: '16px',
                    color: '#101010',
                    '::placeholder': {
                        color: '#888888',
                    },
                },
                invalid: {
                    color: '#fa755a',
                    iconColor: '#fa755a',
                },
            },
        });
        
        cardElement.mount('#stripe-card-element');
        
        // Handle real-time validation errors
        cardElement.on('change', function(event) {
            const errorElement = document.getElementById('stripe-payment-errors');
            if (event.error) {
                errorElement.textContent = event.error.message;
                errorElement.classList.add('is-visible');
            } else {
                errorElement.textContent = '';
                errorElement.classList.remove('is-visible');
            }
        });
    }
    
    /**
     * Navigate to checkout step
     */
    function goToCheckoutStep(step) {
        const steps = document.querySelectorAll('.fs-checkout-step');
        steps.forEach(s => s.classList.remove('fs-checkout-step--active'));
        
        const targetStep = document.getElementById('step' + step);
        if (targetStep) {
            targetStep.classList.add('fs-checkout-step--active');
        }
        
        // Update progress indicators
        const progressSteps = document.querySelectorAll('.fs-checkout-step');
        progressSteps.forEach((ps, index) => {
            if (index + 1 <= step) {
                ps.classList.add('fs-checkout-step--completed');
            } else {
                ps.classList.remove('fs-checkout-step--completed');
            }
        });
    }
    
    /**
     * Validate and proceed to payment
     * @param {HTMLElement} button - Optional button element for loading state
     */
    function validateAndProceedToPayment(button = null) {
        const form = document.getElementById('checkoutForm');
        const selectedAddress = document.querySelector('input[name="shippingAddressId"]:checked');
        const errorEl = document.getElementById('form-error');
        
        if (!selectedAddress) {
            if (errorEl) {
                errorEl.textContent = 'Please select a shipping address';
                errorEl.classList.add('is-visible');
            }
            return;
        }
        
        const usingNew = selectedAddress.value === 'new';
        if (usingNew) {
            const requiredFields = ['fullName', 'address', 'city', 'state', 'zip', 'phone'];
            for (const fieldName of requiredFields) {
                const input = form.querySelector('[name="' + fieldName + '"]');
                if (!input || !input.value || !input.value.trim()) {
                    if (errorEl) {
                        const pretty = fieldName.replace(/([A-Z])/g, ' $1').toLowerCase();
                        errorEl.textContent = 'Please fill in the ' + pretty;
                        errorEl.classList.add('is-visible');
                    }
                    if (input) input.focus();
                    return;
                }
            }
        }
        
        if (errorEl) errorEl.classList.remove('is-visible');
        goToCheckoutStep(2);
    }
    
    /**
     * Review order before submission
     */
    function reviewOrder() {
        const selectedPaymentMethod = document.querySelector('input[name="paymentMethod"]:checked');
        if (!selectedPaymentMethod) {
            const errorEl = document.getElementById('form-error');
            if (errorEl) {
                errorEl.textContent = 'Please select a payment method';
                errorEl.classList.add('is-visible');
            }
            return;
        }
        
        if (selectedPaymentMethod.value === 'STRIPE') {
            initiateStripePayment();
        } else {
            goToCheckoutStep(3);
        }
    }
    
    /**
     * Initiate Stripe payment
     */
    async function initiateStripePayment() {
        const form = document.getElementById('checkoutForm');
        const btn = document.querySelector('#step3 button[type="submit"][name="placeOrder"]');
        const errorEl = document.getElementById('form-error');
        
        if (!stripe) {
            if (errorEl) {
                errorEl.textContent = 'Stripe not initialized';
                errorEl.classList.add('is-visible');
            }
            return;
        }
        
        // Prevent duplicate submission
        if (checkoutSubmissionInProgress && !checkoutIdempotencyKey) {
            if (errorEl) {
                errorEl.textContent = 'Order is being processed. Please wait...';
                errorEl.classList.add('is-visible');
            }
            return;
        }
        
        try {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner"></span> Processing...';
            
            // Show loading overlay
            if (typeof StateManager !== 'undefined') {
                StateManager.showOverlay(true, 'Processing payment...');
            }
            
            // Collect form data
            const formData = new FormData(form);
            formData.append('action', 'initiate');
            formData.append('paymentMethod', 'STRIPE');
            
            // Add idempotency key for duplicate prevention
            const idempotencyKey = checkoutIdempotencyKey || generateIdempotencyKey();
            
            const response = await fetch(contextPath + '/payment', {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Idempotency-Key': idempotencyKey
                }
            });
            
            const data = await response.json();
            
            if (!response.ok || !data.clientSecret) {
                throw new Error(data.error || 'Failed to create payment intent');
            }
            
            // Confirm the payment with Stripe
            const { error, paymentIntent } = await stripe.confirmCardPayment(data.clientSecret, {
                payment_method: {
                    card: cardElement,
                    billing_details: {
                        name: formData.get('fullName'),
                        email: userEmail,
                        phone: formData.get('phone'),
                        address: {
                            line1: formData.get('address'),
                            city: formData.get('city'),
                            state: formData.get('state'),
                            postal_code: formData.get('zip'),
                        }
                    }
                }
            });
            
            if (error) {
                throw new Error(error.message);
            }
            
            // Payment successful, redirect to success page
            window.location.href = contextPath + '/payment?action=success&orderId=' + data.orderId;
            
        } catch (err) {
            console.error('Stripe payment error:', err);
            
            // Hide loading overlay
            if (typeof StateManager !== 'undefined') {
                StateManager.showOverlay(false);
            }
            
            // Show error state
            if (typeof StateManager !== 'undefined') {
                StateManager.showError('checkout-error', {
                    errorMessage: err.message || 'Payment failed. Please try again.',
                    onRetry: function() {
                        Checkout.validateAndProceedToPayment();
                    }
                });
            }
            
            if (errorEl) {
                errorEl.textContent = err.message || 'Payment failed. Please try again.';
                errorEl.classList.add('is-visible');
            }
            checkoutSubmissionInProgress = false;
            btn.disabled = false;
            btn.innerHTML = 'Review Order';
        }
    }
    
    // Public API
    return {
        init: init,
        goToCheckoutStep: goToCheckoutStep,
        validateAndProceedToPayment: validateAndProceedToPayment,
        reviewOrder: reviewOrder
    };
})();

// Initialize on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', Checkout.init);
} else {
    Checkout.init();
}

// Expose functions globally for inline handlers
window.Checkout = Checkout;
