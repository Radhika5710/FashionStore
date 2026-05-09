/**
 * SPLASH SCREEN CONTROLLER (DEPRECATED)
 *
 * The luxury splash experience now lives in `luxury-motion.js`
 * (cinematic letter-rise reveal + champagne progress bar).
 *
 * This file is intentionally a no-op to avoid:
 *   - duplicate splash overlays firing simultaneously
 *   - broken logo path (pathname concatenation produced 404s)
 *   - 3.5s blocking overlay that conflicted with the cinematic 2.2s reveal
 *
 * Compatibility shim: keeps `window.FashionStoreSplash.reset()` available
 * for QA tooling that may already reference it.
 */
(function () {
    'use strict';

    window.FashionStoreSplash = window.FashionStoreSplash || {
        reset: function () {
            try {
                localStorage.removeItem('fashionstore_splash_shown');
                sessionStorage.removeItem('fashionstore_splash_session');
                sessionStorage.removeItem('lux_splash_shown');
            } catch (e) { /* storage unavailable */ }
            location.reload();
        }
    };
})();
