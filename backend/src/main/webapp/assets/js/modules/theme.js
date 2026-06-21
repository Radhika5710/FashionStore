/**
 * FashionStore - Theme Module
 * Dark mode, theme switching, and theme persistence
 */

const FashionStoreTheme = (function() {
    const THEME_KEY = 'fashionstore-theme';
    const THEMES = {
        LIGHT: 'light',
        DARK: 'dark'
    };
    
    let currentTheme = THEMES.LIGHT;
    
    function init() {
        // Load saved theme or system preference
        const savedTheme = localStorage.getItem(THEME_KEY);
        const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        
        currentTheme = savedTheme || (systemPrefersDark ? THEMES.DARK : THEMES.LIGHT);
        applyTheme(currentTheme);
        
        // Setup theme toggle button
        setupThemeToggle();
        
        // Listen for system theme changes
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
            if (!localStorage.getItem(THEME_KEY)) {
                currentTheme = e.matches ? THEMES.DARK : THEMES.LIGHT;
                applyTheme(currentTheme);
            }
        });
    }
    
    function setupThemeToggle() {
        const themeToggle = document.getElementById('dark-mode-toggle');
        if (!themeToggle) return;
        
        themeToggle.addEventListener('click', () => {
            toggleTheme();
        });
        
        // Update button state based on current theme
        updateThemeToggleState(themeToggle);
    }
    
    function toggleTheme() {
        currentTheme = currentTheme === THEMES.LIGHT ? THEMES.DARK : THEMES.LIGHT;
        applyTheme(currentTheme);
        localStorage.setItem(THEME_KEY, currentTheme);
        
        const themeToggle = document.getElementById('dark-mode-toggle');
        if (themeToggle) {
            updateThemeToggleState(themeToggle);
        }
    }
    
    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        document.body.classList.remove('theme-light', 'theme-dark');
        document.body.classList.add(`theme-${theme}`);
    }
    
    function updateThemeToggleState(button) {
        if (!button) return;
        
        const isDark = currentTheme === THEMES.DARK;
        button.setAttribute('aria-pressed', isDark ? 'true' : 'false');
        
        // Update icon if present
        const icon = button.querySelector('svg');
        if (icon) {
            if (isDark) {
                // Sun icon for dark mode (to switch to light)
                icon.innerHTML = '<circle cx="12" cy="12" r="5" stroke="currentColor" stroke-width="2" fill="none"></circle><path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72l1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" stroke="currentColor" stroke-width="2" stroke-linecap="round"></path>';
            } else {
                // Moon icon for light mode (to switch to dark)
                icon.innerHTML = '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"></path>';
            }
        }
    }
    
    function getTheme() {
        return currentTheme;
    }
    
    function setTheme(theme) {
        if (theme === THEMES.LIGHT || theme === THEMES.DARK) {
            currentTheme = theme;
            applyTheme(theme);
            localStorage.setItem(THEME_KEY, theme);
            
            const themeToggle = document.getElementById('dark-mode-toggle');
            if (themeToggle) {
                updateThemeToggleState(themeToggle);
            }
        }
    }
    
    function cleanup() {
        // Remove event listeners if needed
        const themeToggle = document.getElementById('dark-mode-toggle');
        if (themeToggle) {
            themeToggle.replaceWith(themeToggle.cloneNode(true));
        }
    }
    
    // Public API
    return {
        THEMES,
        init,
        toggleTheme,
        getTheme,
        setTheme,
        cleanup
    };
})();

// Make theme available globally
if (typeof window.FashionStore === 'undefined') {
    window.FashionStore = {};
}
window.FashionStore.theme = FashionStoreTheme;
window.FashionStore.darkMode = FashionStoreTheme; // For backward compatibility

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('theme', FashionStoreTheme.init, 1);
} else {
    // Fallback: Initialize on DOM ready if FashionStoreApp not available
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', FashionStoreTheme.init);
    } else {
        FashionStoreTheme.init();
    }
}

// Export for ES6 modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FashionStoreTheme;
}
