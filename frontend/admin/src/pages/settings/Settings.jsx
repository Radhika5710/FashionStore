import { useState, useRef, useEffect } from 'react';
import { Store, CreditCard, Truck, Receipt, Save } from 'lucide-react';
import { useToast } from '../../context/ToastContext.jsx';

const TABS = [
  { key: 'store', label: 'Store', Icon: Store },
  { key: 'payment', label: 'Payment', Icon: CreditCard },
  { key: 'shipping', label: 'Shipping', Icon: Truck },
  { key: 'taxes', label: 'Taxes', Icon: Receipt },
];

export default function Settings() {
  const { addToast } = useToast();
  const [activeTab, setActiveTab] = useState('store');
  const [saving, setSaving] = useState(false);
  const saveTimeoutRef = useRef(null);

  // Cleanup timeout on unmount
  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, []);

  const [store, setStore] = useState({ name: 'FashionStore', email: 'support@fashionstore.com', currency: 'USD', timezone: 'UTC' });
  const [payment, setPayment] = useState({ gateway: 'stripe', publicKey: '', secretKey: '', enabled: true });
  const [shipping, setShipping] = useState({ flatRate: '5.00', freeThreshold: '50.00', enabled: true });
  const [taxes, setTaxes] = useState({ rate: '8.00', inclusive: false });

  const handleSave = () => {
    setSaving(true);
    // Clear any existing timeout
    if (saveTimeoutRef.current) {
      clearTimeout(saveTimeoutRef.current);
    }
    saveTimeoutRef.current = setTimeout(() => {
      setSaving(false);
      addToast('Settings saved', 'success');
      saveTimeoutRef.current = null;
    }, 600);
  };

  const renderTab = () => {
    switch (activeTab) {
      case 'store':
        return (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Store Name</label>
              <input value={store.name} onChange={(e) => setStore((s) => ({ ...s, name: e.target.value }))} className="input w-full max-w-md" />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Support Email</label>
              <input type="email" value={store.email} onChange={(e) => setStore((s) => ({ ...s, email: e.target.value }))} className="input w-full max-w-md" />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-md">
              <div>
                <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Currency</label>
                <select value={store.currency} onChange={(e) => setStore((s) => ({ ...s, currency: e.target.value }))} className="input w-full">
                  <option value="USD">USD</option>
                  <option value="EUR">EUR</option>
                  <option value="GBP">GBP</option>
                  <option value="INR">INR</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Timezone</label>
                <select value={store.timezone} onChange={(e) => setStore((s) => ({ ...s, timezone: e.target.value }))} className="input w-full">
                  <option value="UTC">UTC</option>
                  <option value="America/New_York">Eastern</option>
                  <option value="Europe/London">London</option>
                  <option value="Asia/Kolkata">India</option>
                </select>
              </div>
            </div>
          </div>
        );
      case 'payment':
        return (
          <div className="space-y-4 max-w-md">
            <div className="flex items-center gap-2">
              <input id="pay-enabled" type="checkbox" checked={payment.enabled} onChange={(e) => setPayment((p) => ({ ...p, enabled: e.target.checked }))} className="w-4 h-4 rounded border-ink-300" />
              <label htmlFor="pay-enabled" className="text-sm font-medium text-ink-700 dark:text-ink-300">Enable payments</label>
            </div>
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Gateway</label>
              <select value={payment.gateway} onChange={(e) => setPayment((p) => ({ ...p, gateway: e.target.value }))} className="input w-full">
                <option value="stripe">Stripe</option>
                <option value="paypal">PayPal</option>
                <option value="razorpay">Razorpay</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Public Key</label>
              <input value={payment.publicKey} onChange={(e) => setPayment((p) => ({ ...p, publicKey: e.target.value }))} className="input w-full" placeholder="pk_live_..." />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Secret Key</label>
              <input type="password" value={payment.secretKey} onChange={(e) => setPayment((p) => ({ ...p, secretKey: e.target.value }))} className="input w-full" placeholder="sk_live_..." />
            </div>
          </div>
        );
      case 'shipping':
        return (
          <div className="space-y-4 max-w-md">
            <div className="flex items-center gap-2">
              <input id="ship-enabled" type="checkbox" checked={shipping.enabled} onChange={(e) => setShipping((s) => ({ ...s, enabled: e.target.checked }))} className="w-4 h-4 rounded border-ink-300" />
              <label htmlFor="ship-enabled" className="text-sm font-medium text-ink-700 dark:text-ink-300">Enable shipping</label>
            </div>
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Flat Rate ($)</label>
              <input type="number" min="0" step="0.01" value={shipping.flatRate} onChange={(e) => setShipping((s) => ({ ...s, flatRate: e.target.value }))} className="input w-full" />
            </div>
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Free Shipping Threshold ($)</label>
              <input type="number" min="0" step="0.01" value={shipping.freeThreshold} onChange={(e) => setShipping((s) => ({ ...s, freeThreshold: e.target.value }))} className="input w-full" />
            </div>
          </div>
        );
      case 'taxes':
        return (
          <div className="space-y-4 max-w-md">
            <div>
              <label className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Tax Rate (%)</label>
              <input type="number" min="0" max="100" step="0.01" value={taxes.rate} onChange={(e) => setTaxes((t) => ({ ...t, rate: e.target.value }))} className="input w-full" />
            </div>
            <div className="flex items-center gap-2">
              <input id="tax-inclusive" type="checkbox" checked={taxes.inclusive} onChange={(e) => setTaxes((t) => ({ ...t, inclusive: e.target.checked }))} className="w-4 h-4 rounded border-ink-300" />
              <label htmlFor="tax-inclusive" className="text-sm font-medium text-ink-700 dark:text-ink-300">Prices include tax</label>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold text-ink-900 dark:text-white">Settings</h1>

      <div className="flex flex-wrap gap-2">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={[
              'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition',
              activeTab === tab.key
                ? 'bg-ink-900 text-white dark:bg-white dark:text-ink-900'
                : 'bg-white dark:bg-ink-800 border border-ink-200 dark:border-ink-700 text-ink-600 dark:text-ink-300 hover:bg-ink-50 dark:hover:bg-ink-700',
            ].join(' ')}
          >
            <tab.Icon size={16} /> {tab.label}
          </button>
        ))}
      </div>

      <div className="card p-6">
        {renderTab()}
        <div className="mt-6 pt-4 border-t border-ink-200 dark:border-ink-700">
          <button onClick={handleSave} disabled={saving} className="btn-primary">
            <Save size={16} /> {saving ? 'Saving…' : 'Save Settings'}
          </button>
        </div>
      </div>
    </div>
  );
}
