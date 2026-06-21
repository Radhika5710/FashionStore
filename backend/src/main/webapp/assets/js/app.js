/**
 * FashionStore Frontend Application Bootstrap
 * 
 * SINGLE POINT OF INITIALIZATION
 * - One DOMContentLoaded listener for entire application
 * - Controlled module initialization order
 * - Prevents duplicate initialization
 * - Centralized error handling
 * - Stable runtime architecture
 */

(function(window) {
    'use strict';

    const FashionStoreApp = {
        initialized: false,
        modules: {},

        /**
         * Register a module for initialization
         * @param {string} name - Module name
         * @param {Function} initFn - Initialization function
         * @param {number} priority - Priority (lower = earlier)
         */
        registerModule: function(name, initFn, priority) {
            this.modules[name] = {
                initFn: initFn,
                priority: priority || 100,
                initialized: false
            };
        },

        /**
         * Initialize all registered modules in priority order
         */
        init: function() {
            if (this.initialized) {
                console.warn('FashionStoreApp already initialized');
                return;
            }

            console.log('FashionStoreApp: Initializing application...');

            // Sort modules by priority (lower = earlier)
            const sortedModules = Object.entries(this.modules)
                .sort(([, a], [, b]) => a.priority - b.priority);

            // Initialize each module
            sortedModules.forEach(([name, module]) => {
                try {
                    console.log(`FashionStoreApp: Initializing ${name}...`);
                    module.initFn();
                    module.initialized = true;
                    console.log(`FashionStoreApp: ${name} initialized successfully`);
                } catch (error) {
                    console.error(`FashionStoreApp: Failed to initialize ${name}`, error);
                }
            });

            this.initialized = true;
            console.log('FashionStoreApp: Application initialization complete');
        },

        /**
         * Check if a module is initialized
         */
        isModuleInitialized: function(name) {
            return this.modules[name] && this.modules[name].initialized;
        }
    };

    // Initialize on DOM ready - SINGLE LISTENER
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => FashionStoreApp.init());
    } else {
        // DOM already loaded, initialize immediately
        FashionStoreApp.init();
    }

    // Export to window
    window.FashionStoreApp = FashionStoreApp;

})(window);
