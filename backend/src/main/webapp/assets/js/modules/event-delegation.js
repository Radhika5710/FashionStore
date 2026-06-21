/**
 * Centralized Event Delegation System
 * 
 * SINGLE POINT OF EVENT MANAGEMENT
 * - One event listener per event type at document level
 * - Prevents duplicate listeners
 * - Automatic cleanup on page navigation
 * - Supports dynamic content
 * - Debug mode for tracking
 */

(function(window) {
    'use strict';

    const EventDelegation = {
        listeners: {},
        debug: false,

        /**
         * Register an event handler via delegation
         * @param {string} eventType - Event type (click, submit, etc.)
         * @param {string} selector - CSS selector for target elements
         * @param {Function} handler - Event handler function
         * @param {Object} options - Event listener options
         */
        on: function(eventType, selector, handler, options = {}) {
            const key = `${eventType}:${selector}`;
            
            // Check if already registered
            if (this.listeners[key]) {
                if (this.debug) {
                    console.warn(`EventDelegation: Handler already registered for ${key}`);
                }
                return;
            }

            // Store handler reference
            this.listeners[key] = {
                eventType,
                selector,
                handler,
                options
            };

            if (this.debug) {
                console.log(`EventDelegation: Registered ${key}`);
            }
        },

        /**
         * Remove an event handler
         * @param {string} eventType - Event type
         * @param {string} selector - CSS selector
         */
        off: function(eventType, selector) {
            const key = `${eventType}:${selector}`;
            delete this.listeners[key];

            if (this.debug) {
                console.log(`EventDelegation: Removed ${key}`);
            }
        },

        /**
         * Initialize the delegation system
         */
        init: function() {
            // Group listeners by event type
            const groupedListeners = {};
            
            Object.entries(this.listeners).forEach(([key, listener]) => {
                if (!groupedListeners[listener.eventType]) {
                    groupedListeners[listener.eventType] = [];
                }
                groupedListeners[listener.eventType].push(listener);
            });

            // Set up single listener per event type at document level
            Object.entries(groupedListeners).forEach(([eventType, listeners]) => {
                document.addEventListener(eventType, (event) => {
                    listeners.forEach(listener => {
                        const target = event.target.closest(listener.selector);
                        if (target) {
                            // Call handler with context
                            listener.handler.call(target, event, target);
                        }
                    });
                }, { capture: false, passive: false });

                if (this.debug) {
                    console.log(`EventDelegation: Set up ${eventType} listener for ${listeners.length} selectors`);
                }
            });

            if (this.debug) {
                console.log(`EventDelegation: Initialized with ${Object.keys(this.listeners).length} handlers`);
            }
        },

        /**
         * Cleanup all listeners
         */
        cleanup: function() {
            this.listeners = {};
            
            if (this.debug) {
                console.log('EventDelegation: Cleaned up all listeners');
            }
        },

        /**
         * Enable debug mode
         */
        enableDebug: function() {
            this.debug = true;
        },

        /**
         * Disable debug mode
         */
        disableDebug: function() {
            this.debug = false;
        }
    };

    // Cleanup on page navigation
    window.addEventListener('beforeunload', () => {
        EventDelegation.cleanup();
    });

    // Also cleanup on pagehide for SPA-like behavior
    window.addEventListener('pagehide', () => {
        EventDelegation.cleanup();
    });

    // Export
    window.EventDelegation = EventDelegation;

    // Register with FashionStoreApp
    if (typeof window.FashionStoreApp !== 'undefined') {
        window.FashionStoreApp.registerModule('eventDelegation', () => {
            EventDelegation.init();
        }, 0); // Initialize first (priority 0)
    }

})(window);
