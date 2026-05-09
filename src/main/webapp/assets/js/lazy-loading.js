/**
 * FashionStore Lazy Loading and Performance Optimization
 * Implements lazy loading for images, infinite scroll, and asset optimization
 */

class FashionStorePerformance {
    constructor() {
        this.imageObserver = null;
        this.contentObserver = null;
        this.loadedImages = new Set();
        this.config = {
            imageThreshold: 100,
            contentThreshold: 200,
            placeholderImage: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgdmlld0JveD0iMCAwIDQwMCAzMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiBmaWxsPSIjRjVGNUVCIi8+CjxwYXRoIGQ9Ik0yMDAgMTUwQzIxNy4xNTQgMTUwIDIzMS41NDYgMTY0LjQ0NiAyMzEuNTQ2IDE4MS41QzIzMS41NDYgMTk4LjU1NCAyMTcuMTU0IDIxMyAyMDAgMjEzQzE4Mi44NDYgMjEzIDE2OC40NTQgMTk4LjU1NCAxNjguNDU0IDE4MS41QzE2OC40NTQgMTY0LjQ0NiAxODIuODQ2IDE1MCAyMDAgMTUwWiIgZmlsbD0iI0M0QzRDMiIvPgo8cGF0aCBkPSJNMTUwIDEwMEMxNjcuMTU0IDEwMCAxODEuNTQ2IDExNC40NDYgMTgxLjU0NiAxMzEuNUMxODEuNTQ2IDE0OC41NTQgMTY3LjE1NCAxNjMgMTUwIDE2M0pNMjUwIDEwMEMyNjcuMTU0IDEwMCAyODEuNTQ2IDExNC40NDYgMjgxLjU0NiAxMzEuNUMyODEuNTQ2IDE0OC41NTQgMjY3LjE1NCAxNjMgMjUwIDE2M1oiIGZpbGw9IiNDNEM0QzIiLz4KPC9zdmc+',
            retryCount: 3,
            retryDelay: 1000
        };
        this.init();
    }

    init() {
        this.setupImageLazyLoading();
        this.setupContentLazyLoading();
        this.setupInfiniteScroll();
        this.setupAssetOptimization();
        this.setupPerformanceMonitoring();
    }

    // Image Lazy Loading Implementation
    setupImageLazyLoading() {
        if ('IntersectionObserver' in window) {
            this.imageObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        this.loadImage(entry.target);
                    }
                });
            }, {
                rootMargin: `${this.config.imageThreshold}px`,
                threshold: 0.1
            });
        } else {
            // Fallback for older browsers
            this.setupLegacyImageLoading();
        }

        // Observe all images with data-src attribute
        document.querySelectorAll('img[data-src]').forEach(img => {
            this.imageObserver ? this.imageObserver.observe(img) : this.loadImage(img);
        });
    }

    loadImage(img) {
        if (this.loadedImages.has(img)) return;
        
        const src = img.dataset.src;
        if (!src) return;

        // Add loading state
        img.classList.add('loading');
        
        // Create new image to preload
        const newImg = new Image();
        newImg.onload = () => {
            img.src = src;
            img.classList.remove('loading');
            img.classList.add('loaded');
            this.loadedImages.add(img);
            
            // Remove from observer
            if (this.imageObserver) {
                this.imageObserver.unobserve(img);
            }
            
            // Trigger custom event
            img.dispatchEvent(new CustomEvent('imageloaded', { detail: { img, src } }));
        };

        newImg.onerror = () => {
            this.handleImageError(img, src);
        };

        newImg.src = src;
    }

    handleImageError(img, src) {
        const retryCount = parseInt(img.dataset.retryCount || '0');
        
        if (retryCount < this.config.retryCount) {
            img.dataset.retryCount = retryCount + 1;
            setTimeout(() => {
                this.loadImage(img);
            }, this.config.retryDelay * (retryCount + 1));
        } else {
            // Show error state
            img.classList.add('error');
            img.src = this.config.placeholderImage;
        }
    }

    setupLegacyImageLoading() {
        const scrollHandler = () => {
            const images = document.querySelectorAll('img[data-src]:not(.loaded)');
            images.forEach(img => {
                const rect = img.getBoundingClientRect();
                if (rect.top < window.innerHeight + this.config.imageThreshold) {
                    this.loadImage(img);
                }
            });
        };

        window.addEventListener('scroll', scrollHandler, { passive: true });
        window.addEventListener('resize', scrollHandler, { passive: true });
        scrollHandler(); // Initial check
    }

    // Content Lazy Loading (for heavy content sections)
    setupContentLazyLoading() {
        if ('IntersectionObserver' in window) {
            this.contentObserver = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        this.loadContent(entry.target);
                    }
                });
            }, {
                rootMargin: `${this.config.contentThreshold}px`,
                threshold: 0.1
            });
        }

        // Observe all elements with data-load-content attribute
        document.querySelectorAll('[data-load-content]').forEach(element => {
            this.contentObserver ? this.contentObserver.observe(element) : this.loadContent(element);
        });
    }

    loadContent(element) {
        const contentType = element.dataset.loadContent;
        const url = element.dataset.loadUrl;
        
        if (!url || element.dataset.loaded === 'true') return;

        element.classList.add('loading');
        
        fetch(url)
            .then(response => response.text())
            .then(html => {
                element.innerHTML = html;
                element.classList.remove('loading');
                element.classList.add('loaded');
                element.dataset.loaded = 'true';
                
                // Remove from observer
                if (this.contentObserver) {
                    this.contentObserver.unobserve(element);
                }
                
                // Trigger custom event
                element.dispatchEvent(new CustomEvent('contentloaded', { detail: { element, url } }));
                
                // Initialize any scripts in the loaded content
                this.initializeLoadedContent(element);
            })
            .catch(error => {
                console.error('Error loading content:', error);
                element.classList.add('error');
            });
    }

    initializeLoadedContent(element) {
        // Re-initialize components in loaded content
        element.querySelectorAll('img[data-src]').forEach(img => {
            this.imageObserver ? this.imageObserver.observe(img) : this.loadImage(img);
        });
        
        // Initialize any JavaScript components
        if (window.initializeComponents) {
            window.initializeComponents(element);
        }
    }

    // Infinite Scroll Implementation
    setupInfiniteScroll() {
        const infiniteScrollElements = document.querySelectorAll('[data-infinite-scroll]');
        
        infiniteScrollElements.forEach(element => {
            const url = element.dataset.infiniteScroll;
            const container = element.dataset.container ? 
                document.querySelector(element.dataset.container) : element;
            
            if (!url || !container) return;

            const observer = new IntersectionObserver((entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting && !container.dataset.loading) {
                        this.loadMoreContent(container, url);
                    }
                });
            }, {
                rootMargin: '200px',
                threshold: 0.1
            });

            observer.observe(element);
        });
    }

    loadMoreContent(container, url) {
        if (container.dataset.loading === 'true') return;
        
        container.dataset.loading = 'true';
        const page = parseInt(container.dataset.page || '1') + 1;
        
        const loader = document.createElement('div');
        loader.className = 'infinite-scroll-loader';
        loader.innerHTML = '<div class="spinner"></div>';
        container.appendChild(loader);

        fetch(`${url}?page=${page}`)
            .then(response => response.text())
            .then(html => {
                if (html.trim()) {
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = html;
                    
                    // Insert new content before loader
                    container.insertBefore(tempDiv, loader);
                    
                    // Initialize new content
                    this.initializeLoadedContent(tempDiv);
                    
                    container.dataset.page = page;
                    
                    // Check if there's more content
                    if (tempDiv.children.length === 0) {
                        // No more content, hide loader
                        loader.style.display = 'none';
                    }
                } else {
                    // No more content
                    loader.style.display = 'none';
                }
            })
            .catch(error => {
                console.error('Error loading more content:', error);
                loader.innerHTML = '<p>Error loading more content</p>';
            })
            .finally(() => {
                container.dataset.loading = 'false';
                setTimeout(() => {
                    if (loader.parentNode) {
                        loader.remove();
                    }
                }, 500);
            });
    }

    // Asset Optimization
    setupAssetOptimization() {
        this.optimizeImages();
        this.setupCDNFallback();
        this.setupResourceHints();
        this.setupServiceWorker();
    }

    optimizeImages() {
        // Convert images to WebP if supported
        if (this.supportsWebP()) {
            document.querySelectorAll('img[data-webp]').forEach(img => {
                const webpSrc = img.dataset.webp;
                if (webpSrc) {
                    img.src = webpSrc;
                }
            });
        }

        // Add responsive image support
        document.querySelectorAll('img[data-srcset]').forEach(img => {
            const srcset = img.dataset.srcset;
            if (srcset) {
                img.srcset = srcset;
            }
        });
    }

    setupCDNFallback() {
        // CDN fallback for static assets
        const cdnBase = 'https://cdn.fashionstore.com';
        const localBase = '/assets';
        
        document.querySelectorAll('[data-cdn-src]').forEach(element => {
            const cdnSrc = element.dataset.cdnSrc;
            const localSrc = element.dataset.src || cdnSrc.replace(cdnBase, localBase);
            
            // Try CDN first, fallback to local
            this.testCDNAvailability(cdnSrc)
                .then(available => {
                    if (available) {
                        element.src = element.tagName === 'IMG' ? cdnSrc : cdnSrc;
                        if (element.tagName === 'LINK') {
                            element.href = cdnSrc;
                        }
                    } else {
                        element.src = element.tagName === 'IMG' ? localSrc : localSrc;
                        if (element.tagName === 'LINK') {
                            element.href = localSrc;
                        }
                    }
                });
        });
    }

    testCDNAvailability(url) {
        return fetch(url, { method: 'HEAD', mode: 'no-cors' })
            .then(() => true)
            .catch(() => false);
    }

    setupResourceHints() {
        // Critical CSS / JS / fonts are already inlined or loaded synchronously
        // via head.jsp; we no longer need preload hints for phantom files like
        // /assets/css/critical.css, /assets/fonts/main.woff2, /assets/js/critical.js
        // (those caused 404 spam in the console).

        // DNS prefetch for external domains we actually use.
        const externalDomains = [
            'https://fonts.googleapis.com',
            'https://fonts.gstatic.com',
            'https://images.unsplash.com'
        ];

        externalDomains.forEach(domain => {
            const link = document.createElement('link');
            link.rel = 'dns-prefetch';
            link.href = domain;
            document.head.appendChild(link);
        });
    }

    setupServiceWorker() {
        // Service worker file (/sw.js) is not shipped in this build.
        // Attempting to register a non-existent worker spams a console
        // warning even with a silent .catch, so we early-return.
        // To enable offline support: drop a sw.js into webapp root and
        // flip the flag below to true.
        const SERVICE_WORKER_ENABLED = false;
        if (!SERVICE_WORKER_ENABLED) return;
        if ('serviceWorker' in navigator) {
            navigator.serviceWorker.register('/sw.js').catch(() => { /* ignore */ });
        }
    }

    // Performance Monitoring
    setupPerformanceMonitoring() {
        // Monitor Core Web Vitals
        this.monitorLCP();
        this.monitorFID();
        this.monitorCLS();
        
        // Monitor custom metrics
        this.monitorImageLoadTimes();
        this.monitorContentLoadTimes();
    }

    monitorLCP() {
        if ('PerformanceObserver' in window) {
            const observer = new PerformanceObserver((list) => {
                const entries = list.getEntries();
                const lastEntry = entries[entries.length - 1];
                
                // Send to analytics
                this.sendMetric('LCP', lastEntry.startTime);
            });
            
            observer.observe({ entryTypes: ['largest-contentful-paint'] });
        }
    }

    monitorFID() {
        if ('PerformanceObserver' in window) {
            const observer = new PerformanceObserver((list) => {
                const entries = list.getEntries();
                entries.forEach(entry => {
                    if (entry.name === 'first-input') {
                        this.sendMetric('FID', entry.processingStart - entry.startTime);
                    }
                });
            });
            
            observer.observe({ entryTypes: ['first-input'] });
        }
    }

    monitorCLS() {
        if ('PerformanceObserver' in window) {
            let clsValue = 0;
            
            const observer = new PerformanceObserver((list) => {
                const entries = list.getEntries();
                entries.forEach(entry => {
                    if (!entry.hadRecentInput) {
                        clsValue += entry.value;
                    }
                });
                
                this.sendMetric('CLS', clsValue);
            });
            
            observer.observe({ entryTypes: ['layout-shift'] });
        }
    }

    monitorImageLoadTimes() {
        document.addEventListener('imageloaded', (event) => {
            const { img, src } = event.detail;
            const loadTime = performance.now();
            this.sendMetric('image_load_time', loadTime, { src });
        });
    }

    monitorContentLoadTimes() {
        document.addEventListener('contentloaded', (event) => {
            const { element, url } = event.detail;
            const loadTime = performance.now();
            this.sendMetric('content_load_time', loadTime, { url });
        });
    }

    sendMetric(name, value, metadata = {}) {
        // Send metrics to analytics service (only if gtag is loaded).
        if (window.gtag) {
            window.gtag('event', name, {
                value: Math.round(value),
                ...metadata
            });
        }

        // Custom analytics endpoint is not implemented server-side yet.
        // Posting to /api/analytics/metrics produced 404 spam on every page,
        // so the call is intentionally disabled. To enable, implement a servlet
        // mapped to /api/analytics/metrics and flip the flag below to true.
        const ANALYTICS_ENDPOINT_ENABLED = false;
        if (ANALYTICS_ENDPOINT_ENABLED) {
            fetch('/api/analytics/metrics', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name, value, metadata,
                    timestamp: Date.now(),
                    url: window.location.href
                })
            }).catch(() => { /* silently fail */ });
        }
    }

    // Utility methods
    supportsWebP() {
        const canvas = document.createElement('canvas');
        canvas.width = 1;
        canvas.height = 1;
        return canvas.toDataURL('image/webp').indexOf('data:image/webp') === 0;
    }

    // Public API
    preloadImage(src) {
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => resolve(img);
            img.onerror = reject;
            img.src = src;
        });
    }

    preloadContent(url) {
        return fetch(url)
            .then(response => response.text())
            .then(html => {
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = html;
                return tempDiv;
            });
    }

    getPerformanceMetrics() {
        const navigation = performance.getEntriesByType('navigation')[0];
        const paint = performance.getEntriesByType('paint');
        
        return {
            domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
            loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
            firstPaint: paint.find(p => p.name === 'first-paint')?.startTime,
            firstContentfulPaint: paint.find(p => p.name === 'first-contentful-paint')?.startTime,
            totalImages: document.images.length,
            loadedImages: this.loadedImages.size
        };
    }
}

// Initialize performance optimization
document.addEventListener('DOMContentLoaded', () => {
    window.fashionStorePerformance = new FashionStorePerformance();
});

// Global functions for manual control
window.preloadImage = (src) => {
    if (window.fashionStorePerformance) {
        return window.fashionStorePerformance.preloadImage(src);
    }
};

window.preloadContent = (url) => {
    if (window.fashionStorePerformance) {
        return window.fashionStorePerformance.preloadContent(url);
    }
};

window.getPerformanceMetrics = () => {
    if (window.fashionStorePerformance) {
        return window.fashionStorePerformance.getPerformanceMetrics();
    }
};
