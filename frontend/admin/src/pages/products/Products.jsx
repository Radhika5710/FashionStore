import { useNavigate } from 'react-router-dom';
import { Plus, Search, Pencil, Trash2, Filter } from 'lucide-react';
import DataTable from '../../components/DataTable.jsx';
import OptimizedImage from '../../components/OptimizedImage.jsx';
import { ProductsApi } from '../../api/client.js';
import { useDataTableWithFilter } from '../../hooks/useDataTable.js';

const STATUS_OPTIONS = ['all', 'active', 'inactive', 'out_of_stock'];

export default function Products() {
  const navigate = useNavigate();

  const {
    items: products,
    loading,
    search,
    setSearch,
    statusFilter,
    setStatusFilter,
    filtered,
    deletingId,
    handleDelete,
  } = useDataTableWithFilter(ProductsApi.list, ProductsApi.delete, {
    statusOptions: STATUS_OPTIONS,
    filterKey: 'status',
    deleteConfirmMessage: 'Are you sure you want to delete this product?',
    deleteSuccessMessage: 'Product deleted',
    deleteErrorMessage: 'Delete failed',
    loadErrorMessage: 'Failed to load products',
    filterFn: (rows, search) => {
      if (search.trim()) {
        const q = search.toLowerCase();
        return rows.filter((r) =>
          (r.name || '').toLowerCase().includes(q) ||
          (r.sku || '').toLowerCase().includes(q) ||
          (r.category || '').toLowerCase().includes(q)
        );
      }
      return rows;
    },
  });

  const columns = [
    {
      key: 'image',
      header: 'Image',
      width: '64px',
      render: (r) => (
        <div className="w-10 h-10 rounded-lg bg-ink-100 dark:bg-ink-700 flex items-center justify-center overflow-hidden">
          {r.imageUrl ? (
            <OptimizedImage src={r.imageUrl} alt="" width={40} height={40} />
          ) : (
            <span className="text-xs text-ink-400">—</span>
          )}
        </div>
      ),
    },
    { key: 'name', header: 'Product Name' },
    { key: 'category', header: 'Category', width: '140px' },
    { key: 'price', header: 'Price', width: '100px', render: (r) => `$${(r.price || 0).toFixed(2)}` },
    {
      key: 'stock',
      header: 'Stock',
      width: '90px',
      render: (r) => (
        <span className={`text-xs font-semibold ${(r.stock || 0) <= 5 ? 'text-rose-600' : 'text-emerald-600'}`}>
          {r.stock || 0}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      width: '100px',
      render: (r) => {
        const s = r.status || 'active';
        const cls = s === 'active'
          ? 'pill-success'
          : s === 'inactive'
          ? 'pill-warning'
          : 'pill-pending';
        return <span className={`pill ${cls}`}>{s.replace('_', ' ')}</span>;
      },
    },
    {
      key: 'actions',
      header: '',
      width: '100px',
      render: (r) => (
        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate(`/products/${r.id}/edit`)}
            className="p-2 rounded-lg border border-ink-200/70 dark:border-ink-700/70 bg-white/80 dark:bg-ink-900/80 hover:shadow-md text-ink-600 dark:text-ink-200 transition"
            title="Edit"
          >
            <Pencil size={14} />
          </button>
          <button
            onClick={() => handleDelete(r.id)}
            disabled={deletingId === r.id}
            className="p-2 rounded-lg border border-rose-200/70 dark:border-rose-800/70 bg-white/80 dark:bg-ink-900/80 hover:bg-rose-50 dark:hover:bg-rose-900/30 text-rose-500 disabled:opacity-50 transition"
            title="Delete"
          >
            <Trash2 size={14} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-ink-400 dark:text-ink-500 mb-1">Catalog</p>
          <h1 className="text-2xl font-bold text-ink-900 dark:text-white">Products</h1>
        </div>
        <button
          onClick={() => navigate('/products/new')}
          className="btn-primary self-start"
        >
          <Plus size={16} /> Add Product
        </button>
      </div>

      <div className="rounded-2xl border border-ink-200/80 dark:border-ink-700/80 bg-white/90 dark:bg-ink-800/90 shadow-card backdrop-blur-xl p-4 flex flex-col sm:flex-row gap-3">
        <div className="flex items-center gap-2 px-3.5 h-10 rounded-xl border border-ink-200/80 dark:border-ink-700/80 bg-white/90 dark:bg-ink-900/90 text-ink-400 flex-1 shadow-sm">
          <Search size={16} />
          <input
            type="search"
            placeholder="Search products by name, SKU, category…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-transparent text-sm text-ink-900 dark:text-ink-100 placeholder:text-ink-400 focus:outline-none"
          />
        </div>
        <div className="flex items-center gap-2">
          <div className="w-10 h-10 rounded-full bg-white/90 dark:bg-ink-900/90 border border-ink-200/80 dark:border-ink-700/80 flex items-center justify-center text-ink-500 shadow-sm">
            <Filter size={16} />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-10 px-3 rounded-xl border border-ink-200/80 dark:border-ink-700/80 bg-white/90 dark:bg-ink-900/90 text-sm text-ink-900 dark:text-ink-100 focus:outline-none shadow-sm"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s === 'all' ? 'All Status' : s.replace('_', ' ')}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="card">
        {loading ? (
          <div className="p-8 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-10 rounded-xl border border-ink-200/70 dark:border-ink-700/70 bg-white/70 dark:bg-ink-800/70 backdrop-blur-xl animate-pulse" />
            ))}
          </div>
        ) : (
          <DataTable
            columns={columns}
            rows={filtered}
            empty="No products found"
            getRowKey={(r) => r.id}
          />
        )}
      </div>
    </div>
  );
}
