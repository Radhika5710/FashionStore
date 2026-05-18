import { useState } from 'react';
import { Plus, Pencil, Trash2, Save, X, Percent } from 'lucide-react';
import { CouponsApi } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { useDataTable } from '../../hooks/useDataTable.js';

export default function Coupons() {
  const { addToast } = useToast();
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({
    code: '',
    discountType: 'percentage',
    discountValue: '',
    minOrder: '',
    maxUses: '',
    expiresAt: '',
  });
  const [saving, setSaving] = useState(false);

  const {
    items: coupons,
    setItems: setCoupons,
    loading,
    handleDelete,
  } = useDataTable(CouponsApi.list, CouponsApi.delete, {
    deleteConfirmMessage: 'Delete this coupon?',
    deleteSuccessMessage: 'Coupon deleted',
    deleteErrorMessage: 'Delete failed',
    loadErrorMessage: 'Failed to load coupons',
  });

  const handleSave = async () => {
    if (!form.code.trim() || !form.discountValue) return;
    setSaving(true);
    try {
      const payload = {
        ...form,
        discountValue: Number(form.discountValue),
        minOrder: Number(form.minOrder) || 0,
        maxUses: Number(form.maxUses) || 0,
      };
      if (editingId) {
        await CouponsApi.update(editingId, payload);
        addToast('Coupon updated', 'success');
      } else {
        await CouponsApi.create(payload);
        addToast('Coupon created', 'success');
      }
      setForm({ code: '', discountType: 'percentage', discountValue: '', minOrder: '', maxUses: '', expiresAt: '' });
      setEditingId(null);
      // Refresh data
      const data = await CouponsApi.list();
      setCoupons(Array.isArray(data) ? data : []);
    } catch {
      addToast('Save failed', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (c) => {
    setEditingId(c.id);
    setForm({
      code: c.code || '',
      discountType: c.discountType || 'percentage',
      discountValue: c.discountValue ?? '',
      minOrder: c.minOrder ?? '',
      maxUses: c.maxUses ?? '',
      expiresAt: c.expiresAt ? c.expiresAt.slice(0, 10) : '',
    });
  };

  return (
    <div className="space-y-8 section-lg">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <p className="text-overline mb-2">Marketing</p>
          <h1 className="text-h1">Coupons</h1>
        </div>
        <div className="badge badge-primary">Create and manage incentives</div>
      </div>

      <div className="card section-md">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-lg bg-[var(--color-bg-secondary)] flex items-center justify-center">
            <Percent size={18} className="text-[var(--color-text-muted)]" />
          </div>
          <h2 className="text-h4">
            {editingId ? 'Edit Coupon' : 'New Coupon'}
          </h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <input placeholder="Code (e.g. SAVE20)" value={form.code} onChange={(e) => setForm((f) => ({ ...f, code: e.target.value.toUpperCase() }))} className="input" />
          <select value={form.discountType} onChange={(e) => setForm((f) => ({ ...f, discountType: e.target.value }))} className="input">
            <option value="percentage">Percentage %</option>
            <option value="fixed">Fixed Amount</option>
          </select>
          <input type="number" min="0" step="0.01" placeholder="Discount value" value={form.discountValue} onChange={(e) => setForm((f) => ({ ...f, discountValue: e.target.value }))} className="input" />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-4">
          <input type="number" min="0" step="0.01" placeholder="Min order ($)" value={form.minOrder} onChange={(e) => setForm((f) => ({ ...f, minOrder: e.target.value }))} className="input" />
          <input type="number" min="0" placeholder="Max uses" value={form.maxUses} onChange={(e) => setForm((f) => ({ ...f, maxUses: e.target.value }))} className="input" />
          <input type="date" value={form.expiresAt} onChange={(e) => setForm((f) => ({ ...f, expiresAt: e.target.value }))} className="input" />
        </div>
        <div className="flex items-center gap-3 mt-6">
          <button onClick={handleSave} disabled={saving} className="btn btn-primary">
            <Save size={14} /> {saving ? 'Saving…' : editingId ? 'Update' : 'Create'}
          </button>
          {editingId && (
            <button onClick={() => { setEditingId(null); setForm({ code: '', discountType: 'percentage', discountValue: '', minOrder: '', maxUses: '', expiresAt: '' }); }} className="btn btn-secondary">
              <X size={14} /> Cancel
            </button>
          )}
        </div>
      </div>

      <div className="card">
        {loading ? (
          <div className="card-body space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-12 rounded-lg bg-[var(--color-bg-secondary)] animate-pulse" />
            ))}
          </div>
        ) : (
          <table className="w-full text-sm text-left">
            <thead>
              <tr className="border-b border-[var(--color-border)] bg-[var(--color-bg-secondary)]">
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)]">Code</th>
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)]">Type</th>
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)]">Value</th>
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)]">Expires</th>
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)] w-24"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--color-border-light)]">
              {coupons.length === 0 ? (
                <tr><td colSpan={5} className="px-5 py-8 text-center text-[var(--color-text-muted)]">No coupons</td></tr>
              ) : (
                coupons.map((c) => (
                  <tr key={c.id} className="hover:bg-[var(--color-bg-secondary)] transition">
                    <td className="px-5 py-3 font-mono font-semibold text-[var(--color-text-primary)]">{c.code}</td>
                    <td className="px-5 py-3 text-[var(--color-text-secondary)] capitalize">{c.discountType}</td>
                    <td className="px-5 py-3 text-[var(--color-text-secondary)]">{c.discountType === 'percentage' ? `${c.discountValue}%` : `$${c.discountValue}`}</td>
                    <td className="px-5 py-3 text-[var(--color-text-secondary)]">{c.expiresAt ? new Date(c.expiresAt).toLocaleDateString() : '—'}</td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1">
                        <button onClick={() => handleEdit(c)} className="p-2 rounded-lg border border-[var(--color-border)] bg-white hover:bg-[var(--color-bg-secondary)] text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition"><Pencil size={14} /></button>
                        <button onClick={() => handleDelete(c.id)} className="p-2 rounded-lg border border-[var(--color-error)]/20 bg-white hover:bg-[var(--color-error)]/10 text-[var(--color-error)] transition"><Trash2 size={14} /></button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
