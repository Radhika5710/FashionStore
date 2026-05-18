import { useState } from 'react';
import { Plus, Pencil, Trash2, Save, X, ImageIcon } from 'lucide-react';
import { CategoriesApi } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { useDataTable } from '../../hooks/useDataTable.js';

export default function Categories() {
  const { addToast } = useToast();
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ name: '', description: '' });
  const [saving, setSaving] = useState(false);

  const {
    items: categories,
    setItems: setCategories,
    loading,
    handleDelete,
  } = useDataTable(CategoriesApi.list, CategoriesApi.delete, {
    deleteConfirmMessage: 'Delete this category?',
    deleteSuccessMessage: 'Category deleted',
    deleteErrorMessage: 'Delete failed',
    loadErrorMessage: 'Failed to load categories',
  });

  const handleSave = async () => {
    if (!form.name.trim()) return;
    setSaving(true);
    try {
      if (editingId) {
        await CategoriesApi.update(editingId, form);
        addToast('Category updated', 'success');
      } else {
        await CategoriesApi.create(form);
        addToast('Category created', 'success');
      }
      setForm({ name: '', description: '' });
      setEditingId(null);
      // Refresh data
      const data = await CategoriesApi.list();
      setCategories(Array.isArray(data) ? data : []);
    } catch {
      addToast('Save failed', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (cat) => {
    setEditingId(cat.id);
    setForm({ name: cat.name || '', description: cat.description || '' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm({ name: '', description: '' });
  };

  return (
    <div className="space-y-8 section-lg">
      <div>
        <p className="text-overline mb-2">Catalog Management</p>
        <h1 className="text-h1">Categories</h1>
      </div>

      <div className="card section-md">
        <div className="flex items-center gap-3 mb-6">
          <div className="w-10 h-10 rounded-lg bg-[var(--color-bg-secondary)] flex items-center justify-center">
            <ImageIcon size={18} className="text-[var(--color-text-muted)]" />
          </div>
          <h2 className="text-h4">
            {editingId ? 'Edit Category' : 'New Category'}
          </h2>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <input
            placeholder="Category name"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            className="input"
          />
          <input
            placeholder="Description"
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            className="input"
          />
        </div>
        <div className="flex items-center gap-3 mt-6">
          <button onClick={handleSave} disabled={saving} className="btn btn-primary">
            <Save size={14} /> {saving ? 'Saving…' : editingId ? 'Update' : 'Create'}
          </button>
          {editingId && (
            <button onClick={cancelEdit} className="btn btn-secondary">
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
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)]">Name</th>
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)]">Description</th>
                <th className="px-5 py-3 font-semibold text-xs uppercase tracking-wider text-[var(--color-text-muted)] w-24"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--color-border-light)]">
              {categories.length === 0 ? (
                <tr><td colSpan={3} className="px-5 py-8 text-center text-[var(--color-text-muted)]">No categories</td></tr>
              ) : (
                categories.map((c) => (
                  <tr key={c.id} className="hover:bg-[var(--color-bg-secondary)] transition">
                    <td className="px-5 py-3 text-[var(--color-text-primary)] font-medium">{c.name}</td>
                    <td className="px-5 py-3 text-[var(--color-text-secondary)]">{c.description || '—'}</td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-1">
                        <button onClick={() => handleEdit(c)} className="p-1.5 rounded hover:bg-[var(--color-bg-secondary)] text-[var(--color-text-muted)] hover:text-[var(--color-text-primary)] transition-colors"><Pencil size={14} /></button>
                        <button onClick={() => handleDelete(c.id)} className="p-1.5 rounded hover:bg-[var(--color-error)]/10 text-[var(--color-error)] transition-colors"><Trash2 size={14} /></button>
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
