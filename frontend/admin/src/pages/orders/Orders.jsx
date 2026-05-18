import { useState, useRef, useEffect, useMemo } from 'react';
import { Eye, PackageCheck, PackageX, Truck, CheckCircle, RotateCcw } from 'lucide-react';
import DataTable from '../../components/DataTable.jsx';
import { OrdersApi } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import { useDataTableWithFilter } from '../../hooks/useDataTable.js';

const STATUS_TABS = ['all', 'pending', 'processing', 'shipped', 'delivered', 'cancelled'];

export default function Orders() {
  const { addToast } = useToast();
  const [actionId, setActionId] = useState(null);
  const [detailOrder, setDetailOrder] = useState(null);
  const modalRef = useRef(null);

  // Lock body scroll when modal is open
  useEffect(() => {
    if (detailOrder) {
      document.body.style.overflow = 'hidden';
      const handleEscape = (e) => {
        if (e.key === 'Escape') setDetailOrder(null);
      };
      document.addEventListener('keydown', handleEscape);
      // Focus the modal when it opens
      if (modalRef.current) {
        modalRef.current.focus();
      }
      return () => {
        document.body.style.overflow = '';
        document.removeEventListener('keydown', handleEscape);
      };
    }
  }, [detailOrder]);

  const {
    items: orders,
    setItems: setOrders,
    loading,
    statusFilter: activeTab,
    setStatusFilter: setActiveTab,
  } = useDataTableWithFilter(() => OrdersApi.list(100), null, {
    statusOptions: STATUS_TABS,
    filterKey: 'status',
    loadErrorMessage: 'Failed to load orders',
  });

  const filtered = useMemo(() => {
    if (activeTab === 'all') return orders;
    return orders.filter((o) => (o.status?.toLowerCase() || 'pending') === activeTab);
  }, [orders, activeTab]);

  const doAction = async (apiFn, id, successMsg) => {
    setActionId(id);
    try {
      await apiFn(id);
      setOrders((prev) => prev.map((o) => (o.id === id ? { ...o, status: inferStatus(apiFn) } : o)));
      addToast(successMsg, 'success');
    } catch {
      addToast('Action failed', 'error');
    } finally {
      setActionId(null);
    }
  };

  const inferStatus = (fn) => {
    if (fn === OrdersApi.approve) return 'processing';
    if (fn === OrdersApi.cancel) return 'cancelled';
    if (fn === OrdersApi.ship) return 'shipped';
    if (fn === OrdersApi.deliver) return 'delivered';
    if (fn === OrdersApi.refund) return 'cancelled';
    return 'pending';
  };

  const columns = [
    { key: 'id', header: 'Order ID', width: '100px' },
    { key: 'customer', header: 'Customer', render: (r) => r.customerName || r.customer?.name || r.user?.email || '—' },
    { key: 'date', header: 'Date', width: '120px', render: (r) => r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—' },
    { key: 'total', header: 'Total', width: '90px', render: (r) => `$${(r.total || 0).toFixed(2)}` },
    { key: 'status', header: 'Status', width: '110px', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'payment', header: 'Payment', width: '110px', render: (r) => <span className="pill pill-primary">{(r.paymentStatus || 'pending').replace('_', ' ')}</span> },
    {
      key: 'actions',
      header: '',
      width: '180px',
      render: (r) => {
        const status = r.status?.toLowerCase() || 'pending';
        return (
          <div className="flex items-center gap-1">
            <ActionBtn icon={Eye} title="View" onClick={() => setDetailOrder(r)} />
            {status === 'pending' && <ActionBtn icon={PackageCheck} title="Approve" onClick={() => doAction(OrdersApi.approve, r.id, 'Order approved')} disabled={actionId === r.id} />}
            {status === 'pending' && <ActionBtn icon={PackageX} title="Cancel" onClick={() => doAction(OrdersApi.cancel, r.id, 'Order cancelled')} disabled={actionId === r.id} danger />}
            {(status === 'processing' || status === 'packing') && <ActionBtn icon={Truck} title="Ship" onClick={() => doAction(OrdersApi.ship, r.id, 'Order shipped')} disabled={actionId === r.id} />}
            {(status === 'shipped' || status === 'out for delivery') && <ActionBtn icon={CheckCircle} title="Deliver" onClick={() => doAction(OrdersApi.deliver, r.id, 'Order delivered')} disabled={actionId === r.id} />}
            {['delivered', 'completed'].includes(status) && <ActionBtn icon={RotateCcw} title="Refund" onClick={() => doAction(OrdersApi.refund, r.id, 'Order refunded')} disabled={actionId === r.id} danger />}
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-8 section-lg">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <p className="text-overline mb-2">Operations</p>
          <h1 className="text-h1">Orders</h1>
        </div>
      </div>

      <div className="flex flex-wrap gap-3">
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={[
              'px-4 py-2 rounded-lg text-sm font-semibold transition shadow-sm border',
              activeTab === tab
                ? 'bg-[var(--color-primary)] text-white border-[var(--color-primary)] shadow-md'
                : 'bg-white border-[var(--color-border)] text-[var(--color-text-secondary)] hover:bg-[var(--color-bg-secondary)] hover:text-[var(--color-text-primary)]',
            ].join(' ')}
          >
            {tab === 'all' ? 'All' : tab.charAt(0).toUpperCase() + tab.slice(1)}
            {tab !== 'all' && (
              <span className="ml-2 text-xs bg-[var(--color-bg-secondary)] px-2 py-0.5 rounded-full">
                {orders.filter((o) => (o.status?.toLowerCase() || 'pending') === tab).length}
              </span>
            )}
          </button>
        ))}
      </div>

      <div className="card">
        {loading ? (
          <div className="card-body space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-12 rounded-lg bg-[var(--color-bg-secondary)] animate-pulse" />
            ))}
          </div>
        ) : (
          <DataTable columns={columns} rows={filtered} empty="No orders found" getRowKey={(r) => r.id} />
        )}
      </div>

      {/* Order Detail Modal */}
      {detailOrder && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4" role="dialog" aria-modal="true" aria-labelledby="modal-title">
          <div ref={modalRef} className="card container-lg w-full max-h-[80vh] overflow-auto p-6 animate-scale-in">
            <div className="flex items-center justify-between mb-6">
              <h2 id="modal-title" className="text-h2">Order #{detailOrder.id}</h2>
              <button onClick={() => setDetailOrder(null)} className="text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition-colors" aria-label="Close modal">
                ✕
              </button>
            </div>
            <div className="space-y-4 text-body">
              <div className="flex justify-between"><span className="text-[var(--color-text-secondary)]">Customer</span><span className="font-medium text-[var(--color-text-primary)]">{detailOrder.customerName || detailOrder.customer?.name || '—'}</span></div>
              <div className="flex justify-between"><span className="text-[var(--color-text-secondary)]">Email</span><span className="font-medium text-[var(--color-text-primary)]">{detailOrder.customerEmail || detailOrder.customer?.email || '—'}</span></div>
              <div className="flex justify-between items-center"><span className="text-[var(--color-text-secondary)]">Status</span><StatusBadge status={detailOrder.status} /></div>
              <div className="flex justify-between items-center"><span className="text-[var(--color-text-secondary)]">Payment</span><span className="badge badge-primary">{detailOrder.paymentStatus || 'pending'}</span></div>
              <div className="flex justify-between"><span className="text-[var(--color-text-secondary)]">Total</span><span className="font-medium text-[var(--color-text-primary)]">${(detailOrder.total || 0).toFixed(2)}</span></div>
              <div className="flex justify-between"><span className="text-[var(--color-text-secondary)]">Date</span><span className="font-medium text-[var(--color-text-primary)]">{detailOrder.createdAt ? new Date(detailOrder.createdAt).toLocaleString() : '—'}</span></div>
              <div className="pt-4">
                <p className="text-overline mb-3">Status Timeline</p>
                <div className="flex flex-wrap gap-2">
                  {['pending','processing','shipped','delivered'].map((step) => {
                    const current = (detailOrder.status || '').toLowerCase();
                    const active = current === step || ['delivered','completed'].includes(current) && step === 'delivered' || (current === 'shipped' && step === 'shipped') || (current === 'processing' && ['pending','processing'].includes(step)) || (current === 'pending' && step === 'pending');
                    return (
                      <span key={step} className={`badge ${active ? 'badge-primary' : ''}`}>
                        {step}
                      </span>
                    );
                  })}
                </div>
              </div>
              {detailOrder.items?.length > 0 && (
                <div className="pt-4 border-t border-[var(--color-border)]">
                  <p className="text-body-sm text-[var(--color-text-secondary)] mb-3">Items</p>
                  <ul className="space-y-2">
                    {detailOrder.items.map((item, i) => (
                      <li key={i} className="flex justify-between text-[var(--color-text-primary)]">
                        <span>{item.name || item.productName} x{item.quantity}</span>
                        <span>${((item.price || 0) * (item.quantity || 1)).toFixed(2)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
            <div className="mt-6 flex justify-end">
              <button onClick={() => setDetailOrder(null)} className="btn btn-secondary">Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ActionBtn({ icon: Icon, onClick, disabled, danger, title }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      title={title}
      className={[
        'p-1.5 rounded-md transition disabled:opacity-40',
        danger
          ? 'hover:bg-rose-50 dark:hover:bg-rose-900/30 text-rose-500'
          : 'hover:bg-ink-100 dark:hover:bg-ink-700 text-ink-500 dark:text-ink-300',
      ].join(' ')}
    >
      <Icon size={14} />
    </button>
  );
}
