import { useEffect, useState, useRef } from 'react';
import { AlertTriangle, Package, Save } from 'lucide-react';
import DataTable from '../../components/DataTable.jsx';
import OptimizedImage from '../../components/OptimizedImage.jsx';
import { InventoryApi, ProductsApi } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function Inventory() {
  const { addToast } = useToast();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState({});
  const [savingId, setSavingId] = useState(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    (async () => {
      try {
        const data = await ProductsApi.list();
        if (mountedRef.current) {
          setProducts(Array.isArray(data) ? data : []);
        }
      } catch {
        if (mountedRef.current) {
          addToast('Failed to load inventory', 'error');
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
  }, [addToast]);

  const handleStockChange = (id, value) => {
    setEditing((prev) => ({ ...prev, [id]: value }));
  };

  const saveStock = async (id) => {
    const stock = Number(editing[id]);
    if (Number.isNaN(stock)) return;
    setSavingId(id);
    try {
      await InventoryApi.updateStock(id, stock);
      setProducts((prev) => prev.map((p) => (p.id === id ? { ...p, stock } : p)));
      setEditing((prev) => { const n = { ...prev }; delete n[id]; return n; });
      addToast('Stock updated', 'success');
    } catch {
      addToast('Update failed', 'error');
    } finally {
      setSavingId(null);
    }
  };

  const lowStock = products.filter((p) => (p.stock || 0) <= 5);

  const columns = [
    { key: 'image', header: '', width: '48px', render: (r) => (
      <div className="w-10 h-10 rounded-lg bg-ink-100 dark:bg-ink-700 flex items-center justify-center overflow-hidden">
        {r.imageUrl ? <OptimizedImage src={r.imageUrl} alt="" width={40} height={40} /> : <Package size={14} className="text-ink-400" />}
      </div>
    )},
    { key: 'name', header: 'Product' },
    { key: 'sku', header: 'SKU', width: '120px' },
    { key: 'category', header: 'Category', width: '120px' },
    {
      key: 'stock',
      header: 'Stock',
      width: '140px',
      render: (r) => (
        <div className="flex items-center gap-2">
          <input
            type="number"
            min="0"
            defaultValue={r.stock || 0}
            onChange={(e) => handleStockChange(r.id, e.target.value)}
            className="w-20 h-8 px-2 rounded border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 text-sm text-ink-900 dark:text-ink-100 focus:outline-none"
          />
          {editing[r.id] !== undefined && (
            <button
              onClick={() => saveStock(r.id)}
              disabled={savingId === r.id}
              className="p-1 rounded hover:bg-emerald-50 dark:hover:bg-emerald-900/30 text-emerald-600 disabled:opacity-40"
              title="Save"
            >
              <Save size={14} />
            </button>
          )}
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Alert',
      width: '120px',
      render: (r) => {
        const s = r.stock || 0;
        if (s === 0) return <span className="pill pill-danger flex items-center gap-1"><AlertTriangle size={10} /> Out of stock</span>;
        if (s <= 5) return <span className="pill pill-warning flex items-center gap-1"><AlertTriangle size={10} /> Low stock</span>;
        return <span className="pill pill-success">In stock</span>;
      },
    },
  ];

  return (
    <div className="space-y-8 section-lg">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <p className="text-overline mb-2">Operations</p>
          <h1 className="text-h1">Inventory</h1>
        </div>
        {lowStock.length > 0 && (
          <div className="badge badge-warning flex items-center gap-1">
            <AlertTriangle size={12} /> {lowStock.length} low stock items
          </div>
        )}
      </div>

      <div className="card">
        {loading ? (
          <div className="card-body space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-12 rounded-lg bg-[var(--color-bg-secondary)] animate-pulse" />
            ))}
          </div>
        ) : (
          <DataTable columns={columns} rows={products} empty="No products in inventory" getRowKey={(r) => r.id} />
        )}
      </div>
    </div>
  );
}
