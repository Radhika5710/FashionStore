import { useEffect, useMemo, useState, useRef } from 'react';
import { useToast } from '../context/ToastContext.jsx';

/**
 * useDataTable - Reusable hook for data table pages
 * 
 * Eliminates repetitive boilerplate across admin pages:
 * - Products, Categories, Coupons, Users, Orders, Inventory
 * 
 * @param {Function} apiCall - API function to fetch data (e.g., ProductsApi.list)
 * @param {Function} deleteApiCall - Optional API function to delete items (e.g., ProductsApi.delete)
 * @param {Object} options - Configuration options
 * @param {Function} options.filterFn - Custom filter function for search/filter
 * @param {string} options.deleteConfirmMessage - Custom delete confirmation message
 * @param {string} options.deleteSuccessMessage - Custom delete success message
 * @param {string} options.deleteErrorMessage - Custom delete error message
 * @param {string} options.loadErrorMessage - Custom load error message
 * 
 * @returns {Object} - { items, setItems, loading, search, setSearch, filters, setFilters, filtered, deletingId, handleDelete }
 */
export function useDataTable(apiCall, deleteApiCall = null, options = {}) {
  const {
    filterFn = null,
    deleteConfirmMessage = 'Are you sure you want to delete this item?',
    deleteSuccessMessage = 'Item deleted',
    deleteErrorMessage = 'Delete failed',
    loadErrorMessage = 'Failed to load data',
  } = options;

  const { addToast } = useToast();

  // State
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({});
  const [deletingId, setDeletingId] = useState(null);
  
  // Ref for cleanup
  const mountedRef = useRef(true);

  // Fetch data on mount
  useEffect(() => {
    mountedRef.current = true;
    (async () => {
      try {
        const data = await apiCall();
        if (mountedRef.current) {
          setItems(Array.isArray(data) ? data : []);
        }
      } catch {
        if (mountedRef.current) {
          addToast(loadErrorMessage, 'error');
        }
      } finally {
        if (mountedRef.current) {
          setLoading(false);
        }
      }
    })();
    return () => {
      mountedRef.current = false;
    };
  }, [apiCall, addToast, loadErrorMessage]);

  // Filter items based on search and filters
  const filtered = useMemo(() => {
    let rows = items;
    
    // Apply custom filter if provided
    if (filterFn) {
      rows = filterFn(rows, search, filters);
    } else {
      // Default filter: search across all string fields
      if (search.trim()) {
        const q = search.toLowerCase();
        rows = rows.filter((r) => {
          // Search across all string properties
          return Object.values(r).some((val) => 
            typeof val === 'string' && val.toLowerCase().includes(q)
          );
        });
      }
      
      // Apply filters
      Object.entries(filters).forEach(([key, value]) => {
        if (value && value !== 'all') {
          rows = rows.filter((r) => r[key] === value);
        }
      });
    }
    
    return rows;
  }, [items, search, filters, filterFn]);

  // Handle delete operation
  const handleDelete = async (id) => {
    if (!deleteApiCall) return;
    if (!window.confirm(deleteConfirmMessage)) return;
    
    setDeletingId(id);
    try {
      await deleteApiCall(id);
      setItems((prev) => prev.filter((item) => item.id !== id));
      addToast(deleteSuccessMessage, 'success');
    } catch {
      addToast(deleteErrorMessage, 'error');
    } finally {
      setDeletingId(null);
    }
  };

  return {
    items,
    setItems,
    loading,
    search,
    setSearch,
    filters,
    setFilters,
    filtered,
    deletingId,
    handleDelete,
  };
}

/**
 * useDataTableWithFilter - Hook with built-in status filter
 * 
 * For pages that need a status filter (Products, Coupons, etc.)
 */
export function useDataTableWithFilter(apiCall, deleteApiCall = null, options = {}) {
  const {
    statusOptions = ['all', 'active', 'inactive'],
    filterKey = 'status',
    ...restOptions
  } = options;

  const {
    items,
    setItems,
    loading,
    search,
    setSearch,
    filters,
    setFilters,
    filtered,
    deletingId,
    handleDelete,
  } = useDataTable(apiCall, deleteApiCall, restOptions);

  // Custom filter that includes status
  const filteredWithStatus = useMemo(() => {
    let rows = items;
    
    // Search filter
    if (search.trim()) {
      const q = search.toLowerCase();
      rows = rows.filter((r) => {
        return Object.values(r).some((val) => 
          typeof val === 'string' && val.toLowerCase().includes(q)
        );
      });
    }
    
    // Status filter
    const statusValue = filters[filterKey];
    if (statusValue && statusValue !== 'all') {
      rows = rows.filter((r) => (r[filterKey] || 'active') === statusValue);
    }
    
    return rows;
  }, [items, search, filters, filterKey]);

  return {
    items,
    setItems,
    loading,
    search,
    setSearch,
    statusFilter: filters[filterKey] || 'all',
    setStatusFilter: (value) => setFilters({ ...filters, [filterKey]: value }),
    statusOptions,
    filtered: filteredWithStatus,
    deletingId,
    handleDelete,
  };
}
