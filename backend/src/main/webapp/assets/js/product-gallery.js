/**
 * FashionStore - Product Gallery with Zoom
 * Premium ecommerce product gallery with zoom functionality,
 * thumbnail navigation, and mobile swipe support
 */

const ProductGallery = (function() {
    const contextPath = window.contextPath || '';
    
    let currentImageIndex = 0;
    let images = [];
    let isZoomed = false;
    
    /**
     * Initialize the product gallery
     */
    function init() {
        const mainImageContainer = document.getElementById('mainImageContainer');
        const mainImage = document.getElementById('mainImage');
        const thumbnails = document.querySelectorAll('.product-gallery__thumb');
        
        if (!mainImage || !mainImageContainer) return;
        
        // Collect all image URLs
        thumbnails.forEach((thumb, index) => {
            const imageUrl = thumb.dataset.image;
            if (imageUrl) {
                images.push(imageUrl);
            }
        });
        
        // Setup zoom functionality
        setupZoom(mainImageContainer, mainImage);
        
        // Setup keyboard navigation
        setupKeyboardNavigation();
        
        // Setup mobile swipe
        setupMobileSwipe(mainImageContainer);
        
        // Setup add to cart buttons
        setupAddToCartButtons();
        
        // Setup quantity stepper
        setupQuantityStepper();
        
        // Hide swipe hint after animation
        const swipeHint = mainImageContainer.querySelector('.product-gallery__swipe-hint');
        if (swipeHint) {
            setTimeout(() => {
                swipeHint.style.display = 'none';
            }, 3000);
        }
    }
    
    /**
     * Setup zoom functionality on hover
     */
    function setupZoom(container, image) {
        const zoomLens = container.querySelector('.product-gallery__zoom-lens');
        if (!zoomLens) return;
        
        container.addEventListener('mousemove', function(e) {
            if (window.innerWidth <= 768) return; // Disable zoom on mobile
            
            const rect = container.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            // Calculate position as percentage
            const xPercent = (x / rect.width) * 100;
            const yPercent = (y / rect.height) * 100;
            
            // Move the zoom lens
            zoomLens.style.left = (x - 75) + 'px';
            zoomLens.style.top = (y - 75) + 'px';
            
            // Apply zoom transform to image
            image.style.transformOrigin = `${xPercent}% ${yPercent}%`;
            image.style.transform = 'scale(2)';
        });
        
        container.addEventListener('mouseleave', function() {
            image.style.transform = 'scale(1)';
            isZoomed = false;
        });
        
        container.addEventListener('mouseenter', function() {
            isZoomed = true;
        });
    }
    
    /**
     * Setup keyboard navigation for gallery
     */
    function setupKeyboardNavigation() {
        document.addEventListener('keydown', function(e) {
            if (e.key === 'ArrowLeft') {
                navigateImage(-1);
            } else if (e.key === 'ArrowRight') {
                navigateImage(1);
            }
        });
    }
    
    /**
     * Setup mobile swipe gesture
     */
    function setupMobileSwipe(container) {
        let touchStartX = 0;
        let touchEndX = 0;
        
        container.addEventListener('touchstart', function(e) {
            touchStartX = e.changedTouches[0].screenX;
        }, { passive: true });
        
        container.addEventListener('touchend', function(e) {
            touchEndX = e.changedTouches[0].screenX;
            handleSwipe();
        }, { passive: true });
        
        function handleSwipe() {
            const swipeThreshold = 50;
            const diff = touchStartX - touchEndX;
            
            if (Math.abs(diff) > swipeThreshold) {
                if (diff > 0) {
                    // Swipe left - next image
                    navigateImage(1);
                } else {
                    // Swipe right - previous image
                    navigateImage(-1);
                }
            }
        }
    }
    
    /**
     * Navigate to next/previous image
     */
    function navigateImage(direction) {
        const thumbnails = document.querySelectorAll('.product-gallery__thumb');
        if (thumbnails.length === 0) return;
        
        currentImageIndex = (currentImageIndex + direction + thumbnails.length) % thumbnails.length;
        
        // Update active thumbnail
        thumbnails.forEach((thumb, index) => {
            thumb.classList.toggle('active', index === currentImageIndex);
        });
        
        // Update main image
        const mainImage = document.getElementById('mainImage');
        const activeThumb = thumbnails[currentImageIndex];
        if (mainImage && activeThumb) {
            mainImage.style.opacity = '0';
            setTimeout(() => {
                mainImage.src = activeThumb.dataset.image;
                mainImage.style.opacity = '1';
            }, 150);
        }
    }
    
    /**
     * Change main image when thumbnail is clicked
     */
    function changeImage(thumbnail) {
        const thumbnails = document.querySelectorAll('.product-gallery__thumb');
        const mainImage = document.getElementById('mainImage');
        
        if (!mainImage) return;
        
        // Update active state
        thumbnails.forEach(thumb => thumb.classList.remove('active'));
        thumbnail.classList.add('active');
        
        // Update current index
        currentImageIndex = Array.from(thumbnails).indexOf(thumbnail);
        
        // Update main image with fade effect
        mainImage.style.opacity = '0';
        mainImage.style.transition = 'opacity 0.15s ease-out';
        
        setTimeout(() => {
            mainImage.src = thumbnail.dataset.image;
            mainImage.style.opacity = '1';
        }, 150);
    }
    
    /**
     * Update size selection visual state
     */
    function updateSizeSelection() {
        const sizeOptions = document.querySelectorAll('.size-selector__option');
        sizeOptions.forEach(option => {
            const input = option.querySelector('input');
            if (input && input.checked) {
                option.classList.add('active');
            } else {
                option.classList.remove('active');
            }
        });
    }
    
    /**
     * Adjust quantity
     */
    function adjustQuantity(delta) {
        const input = document.getElementById('detailsQuantity');
        if (!input) return;
        
        const current = parseInt(input.value) || 1;
        const next = Math.max(1, Math.min(10, current + delta));
        input.value = next;
    }
    
    /**
     * Add to cart from product page
     */
    function addToCart() {
        // Check if CartManager is available
        if (typeof CartManager === 'undefined') {
            FashionStore.showToast('Cart not available. Please refresh the page.', 'error');
            console.error('CartManager is not defined');
            return;
        }

        const productId = document.getElementById('detailsProductId')?.value;
        const sizeInput = document.querySelector('input[name="size"]:checked');
        const quantityInput = document.getElementById('detailsQuantity');
        
        if (!productId) {
            FashionStore.showToast('Product ID not found', 'error');
            return;
        }
        
        // Check if size is required and selected
        const hasSizeOptions = document.querySelector('input[name="size"]');
        if (hasSizeOptions && !sizeInput) {
            FashionStore.showToast('Please select a size', 'error');
            return;
        }
        
        const size = sizeInput ? sizeInput.value : 'M';
        const quantity = quantityInput ? parseInt(quantityInput.value) || 1 : 1;
        
        CartManager.addToCart(productId, size, quantity);
    }

    /**
     * Setup add to cart buttons
     */
    function setupAddToCartButtons() {
        const addToCartBtn = document.getElementById('add-to-cart-btn');
        const mobileAddToCartBtn = document.getElementById('mobile-add-to-cart-btn');

        if (addToCartBtn) {
            addToCartBtn.addEventListener('click', function(e) {
                e.preventDefault();
                addToCart();
            });
        }

        if (mobileAddToCartBtn) {
            mobileAddToCartBtn.addEventListener('click', function(e) {
                e.preventDefault();
                addToCart();
            });
        }
    }

    /**
     * Setup quantity stepper buttons
     */
    function setupQuantityStepper() {
        const decreaseBtn = document.getElementById('decrease-qty-btn');
        const increaseBtn = document.getElementById('increase-qty-btn');

        if (decreaseBtn) {
            decreaseBtn.addEventListener('click', function(e) {
                e.preventDefault();
                adjustQuantity(-1);
            });
        }

        if (increaseBtn) {
            increaseBtn.addEventListener('click', function(e) {
                e.preventDefault();
                adjustQuantity(1);
            });
        }
    }
    
    // Public API
    return {
        init: init,
        changeImage: changeImage,
        updateSizeSelection: updateSizeSelection,
        adjustQuantity: adjustQuantity,
        addToCart: addToCart
    };
})();

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('productGallery', ProductGallery.init, 25);
} else {
    // Fallback: Initialize on DOM ready if FashionStoreApp not available
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', ProductGallery.init);
    } else {
        ProductGallery.init();
    }
}
