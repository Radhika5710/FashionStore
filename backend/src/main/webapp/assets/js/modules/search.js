/**
 * FashionStore - Search Module
 * Search suggestions, trending searches, and keyboard navigation
 * 
 * REFACTORED FOR MVC ARCHITECTURE:
 * - Backend provides all search suggestions (no frontend filtering)
 * - Frontend only handles UI interactions and AJAX triggers
 * - Trending searches loaded from backend session
 * - No client-side result manipulation
 */

const FashionStoreSearch = (function() {
    const contextPath = window.contextPath || '';
    
    let debounceTimer = null;
    let suggestionsContainer = null;
    let selectedIndex = -1;
    let clickHandler = null; // Track click handler for proper cleanup
    
    // Trending searches from backend (loaded via session)
    const trendingSearches = window.trendingSearches || [
        'Summer Collection',
        'Casual Shirts',
        'Denim Jeans',
        'Sneakers',
        'Blazers'
    ];
    
    function init() {
        const searchInput = document.querySelector('.nav-search input, .catalog-search input');
        if (!searchInput) return;
        
        suggestionsContainer = document.createElement('div');
        suggestionsContainer.className = 'search-suggestions';
        suggestionsContainer.style.display = 'none';
        searchInput.parentNode.appendChild(suggestionsContainer);
        
        // Single persistent click handler for all suggestion interactions
        clickHandler = (e) => {
            const item = e.target.closest('.suggestion-item');
            if (item) {
                // Handle remove button
                if (e.target.classList.contains('suggestion-remove')) {
                    e.stopPropagation();
                    removeRecentSearch(e.target.dataset.search);
                    return;
                }
                
                // Handle suggestion click
                const searchInput = document.querySelector('.nav-search input, .catalog-search input');
                if (searchInput) {
                    searchInput.value = item.dataset.value;
                    saveSearch(item.dataset.value);
                    searchInput.closest('form').submit();
                }
            }
        };
        
        // Attach click handler once
        suggestionsContainer.addEventListener('click', clickHandler);
        
        searchInput.addEventListener('input', handleInput);
        searchInput.addEventListener('focus', showTrendingAndRecent);
        searchInput.addEventListener('blur', () => {
            setTimeout(hideSuggestions, 200);
        });
        searchInput.addEventListener('keydown', handleKeyboard);
        
        // Mobile-specific optimizations
        if (window.innerWidth <= 768) {
            searchInput.setAttribute('autocomplete', 'off');
            searchInput.setAttribute('autocorrect', 'off');
            searchInput.setAttribute('autocapitalize', 'off');
        }
    }
    
    function handleInput(e) {
        const query = e.target.value.trim();
        clearTimeout(debounceTimer);
        selectedIndex = -1;
        
        if (query.length < 2) {
            showTrendingAndRecent();
            return;
        }
        
        // Optimized debounce: faster for short queries, slower for long queries
        const debounceTime = query.length < 4 ? 150 : 300;
        debounceTimer = setTimeout(() => fetchSuggestions(query), debounceTime);
    }
    
    function fetchSuggestions(query) {
        // Search suggestions endpoint doesn't exist in backend
        // Fall back to showing trending and recent searches
        showTrendingAndRecent();
    }
    
    function displaySuggestions(suggestions, query) {
        if (!suggestionsContainer) return;
        
        let html = '';
        
        if (suggestions.length === 0) {
            displayEmptyState(query);
            return;
        }
        
        // Add "See all results" option
        html += `
            <div class="suggestion-item search-all" data-value="${escapeHtml(query)}" data-type="search">
                <span class="suggestion-icon">🔍</span>
                <span class="suggestion-text">Search for "${escapeHtml(query)}"</span>
            </div>
        `;
        
        suggestions.forEach((s, index) => {
            const icon = s.type === 'brand' ? '🏷️' : '👕';
            html += `
                <div class="suggestion-item ${index === 0 ? 'selected' : ''}" 
                     data-value="${escapeHtml(s.value)}" 
                     data-type="${s.type}"
                     data-index="${index}">
                    <span class="suggestion-icon">${icon}</span>
                    <span class="suggestion-text">${highlightMatch(s.value, query)}</span>
                </div>
            `;
        });
        
        suggestionsContainer.innerHTML = html;
        suggestionsContainer.style.display = 'block';
        selectedIndex = suggestions.length > 0 ? 0 : -1;
    }
    
    function showTrendingAndRecent() {
        if (!suggestionsContainer) return;
        
        const recentSearches = JSON.parse(sessionStorage.getItem('recentSearches') || '[]');
        let html = '';
        
        // Show trending searches
        if (trendingSearches.length > 0) {
            html += '<div class="suggestion-section">';
            html += '<div class="suggestion-section-title">Trending</div>';
            trendingSearches.forEach(search => {
                html += `
                    <div class="suggestion-item suggestion-chip" data-value="${escapeHtml(search)}">
                        <span class="suggestion-icon">🔥</span>
                        <span class="suggestion-text">${escapeHtml(search)}</span>
                    </div>
                `;
            });
            html += '</div>';
        }
        
        // Show recent searches
        if (recentSearches.length > 0) {
            html += '<div class="suggestion-section">';
            html += '<div class="suggestion-section-title">Recent Searches</div>';
            recentSearches.forEach(search => {
                html += `
                    <div class="suggestion-item" data-value="${escapeHtml(search)}" data-type="recent">
                        <span class="suggestion-icon">🕐</span>
                        <span class="suggestion-text">${escapeHtml(search)}</span>
                        <button class="suggestion-remove" data-search="${escapeHtml(search)}" aria-label="Remove">✕</button>
                    </div>
                `;
            });
            html += '</div>';
        }
        
        if (html === '') {
            suggestionsContainer.style.display = 'none';
            return;
        }
        
        suggestionsContainer.innerHTML = html;
        suggestionsContainer.style.display = 'block';
    }
    
    function displayEmptyState(query) {
        if (!suggestionsContainer) return;
        
        let html = `
            <div class="suggestion-empty">
                <div class="empty-icon">🔍</div>
                <div class="empty-text">No results found for "${escapeHtml(query)}"</div>
                <div class="empty-hint">Try different keywords or browse our categories</div>
            </div>
            <div class="suggestion-item search-all" data-value="${escapeHtml(query)}" data-type="search">
                <span class="suggestion-icon">🔍</span>
                <span class="suggestion-text">See all products matching "${escapeHtml(query)}"</span>
            </div>
        `;
        
        suggestionsContainer.innerHTML = html;
        suggestionsContainer.style.display = 'block';
    }
    
    function handleKeyboard(e) {
        const items = suggestionsContainer?.querySelectorAll('.suggestion-item') || [];
        
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            selectedIndex = Math.min(selectedIndex + 1, items.length - 1);
            updateSelection(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            selectedIndex = Math.max(selectedIndex - 1, 0);
            updateSelection(items);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (selectedIndex >= 0 && items[selectedIndex]) {
                items[selectedIndex].click();
            }
        } else if (e.key === 'Escape') {
            hideSuggestions();
        }
    }
    
    function updateSelection(items) {
        items.forEach((item, index) => {
            item.classList.toggle('selected', index === selectedIndex);
        });
    }
    
    function hideSuggestions() {
        if (suggestionsContainer) {
            suggestionsContainer.style.display = 'none';
        }
    }
    
    function saveSearch(search) {
        let recentSearches = JSON.parse(sessionStorage.getItem('recentSearches') || '[]');
        
        // Remove if already exists (to move to top)
        recentSearches = recentSearches.filter(s => s !== search);
        
        // Add to front
        recentSearches.unshift(search);
        
        // Keep only last 10
        recentSearches = recentSearches.slice(0, 10);
        
        sessionStorage.setItem('recentSearches', JSON.stringify(recentSearches));
    }
    
    function removeRecentSearch(search) {
        let recentSearches = JSON.parse(sessionStorage.getItem('recentSearches') || '[]');
        recentSearches = recentSearches.filter(s => s !== search);
        sessionStorage.setItem('recentSearches', JSON.stringify(recentSearches));
        showTrendingAndRecent();
    }
    
    function highlightMatch(text, query) {
        if (!query) return escapeHtml(text);
        const regex = new RegExp(`(${escapeRegex(query)})`, 'gi');
        return escapeHtml(text).replace(regex, '<strong>$1</strong>');
    }
    
    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    function escapeRegex(string) {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }
    
    function cleanup() {
        clearTimeout(debounceTimer);
        
        // Remove click handler if it exists
        if (suggestionsContainer && clickHandler) {
            suggestionsContainer.removeEventListener('click', clickHandler);
        }
        
        // Remove suggestions container from DOM
        if (suggestionsContainer && suggestionsContainer.parentNode) {
            suggestionsContainer.parentNode.removeChild(suggestionsContainer);
        }
        
        // Clear references
        suggestionsContainer = null;
        clickHandler = null;
    }
    
    // Public API
    return {
        init,
        cleanup,
        saveSearch,
        removeRecentSearch
    };
})();

// Make search available globally for backward compatibility
if (typeof window.FashionStore === 'undefined') {
    window.FashionStore = {};
}
window.FashionStore.search = FashionStoreSearch;

// Register with FashionStoreApp for centralized initialization
if (typeof window.FashionStoreApp !== 'undefined') {
    window.FashionStoreApp.registerModule('search', FashionStoreSearch.init, 20);
} else {
    // Fallback: Initialize on DOM ready if FashionStoreApp not available
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', FashionStoreSearch.init);
    } else {
        FashionStoreSearch.init();
    }
}

// Export for ES6 modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = FashionStoreSearch;
}
