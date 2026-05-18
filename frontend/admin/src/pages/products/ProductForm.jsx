import { useEffect, useState, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Save, Upload } from 'lucide-react';
import OptimizedImage from '../../components/OptimizedImage.jsx';
import { ProductsApi, CategoriesApi } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';

export default function ProductForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEdit = Boolean(id);
  const { addToast } = useToast();

  const [form, setForm] = useState({
    name: '',
    description: '',
    category: '',
    price: '',
    stock: '',
    discount: '',
    status: 'active',
    sku: '',
    tags: '',
    sizes: '',
  });
  const [imagePreview, setImagePreview] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [categories, setCategories] = useState([]);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(isEdit);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    (async () => {
      try {
        const data = await CategoriesApi.list();
        if (mountedRef.current) {
          setCategories(Array.isArray(data) ? data : []);
        }
      } catch {
        // ignore
      }
    })();
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!isEdit) return;
    mountedRef.current = true;
    (async () => {
      try {
        const data = await ProductsApi.get?.(id);
        if (data && mountedRef.current) {
          setForm({
            name: data.name || '',
            description: data.description || '',
            category: data.category || data.categoryId || '',
            price: data.price ?? '',
            stock: data.stock ?? '',
            discount: data.discount ?? '',
            status: data.status || 'active',
            sku: data.sku || '',
            tags: Array.isArray(data.tags) ? data.tags.join(', ') : data.tags || '',
            sizes: Array.isArray(data.sizes) ? data.sizes.join(', ') : data.sizes || '',
          });
          setImagePreview(data.imageUrl || '');
        }
      } catch {
        if (mountedRef.current) {
          addToast('Failed to load product', 'error');
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
  }, [id, isEdit, addToast]);

  // Cleanup blob URL on unmount to prevent memory leak
  useEffect(() => {
    return () => {
      if (imagePreview && imagePreview.startsWith('blob:')) {
        URL.revokeObjectURL(imagePreview);
      }
    };
  }, [imagePreview]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Revoke previous blob URL if it exists
      if (imagePreview && imagePreview.startsWith('blob:')) {
        URL.revokeObjectURL(imagePreview);
      }
      const url = URL.createObjectURL(file);
      setImagePreview(url);
      setImageFile(file);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const price = Number(form.price);
      const stock = Number(form.stock);
      const discount = Number(form.discount) || 0;

      // Validate numeric fields
      if (!Number.isFinite(price) || price <= 0) {
        addToast('Price must be a positive number', 'error');
        setSaving(false);
        return;
      }
      if (!Number.isFinite(stock) || stock < 0) {
        addToast('Stock must be a non-negative number', 'error');
        setSaving(false);
        return;
      }
      if (!Number.isFinite(discount) || discount < 0 || discount > 100) {
        addToast('Discount must be between 0 and 100', 'error');
        setSaving(false);
        return;
      }

      const payload = {
        ...form,
        price,
        stock,
        discount,
        tags: (form.tags || '').split(',').map((t) => t.trim()).filter(Boolean),
        sizes: (form.sizes || '').split(',').map((t) => t.trim()).filter(Boolean),
      };

      // Send multipart if image file is present
      if (imageFile) {
        const formData = new FormData();
        formData.append('payload', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
        formData.append('image', imageFile);
        if (isEdit) {
          await ProductsApi.update?.(id, formData);
          addToast('Product updated', 'success');
        } else {
          await ProductsApi.create?.(formData);
          addToast('Product created', 'success');
        }
      } else {
        if (isEdit) {
          await ProductsApi.update?.(id, payload);
          addToast('Product updated', 'success');
        } else {
          await ProductsApi.create?.(payload);
          addToast('Product created', 'success');
        }
      }
      navigate('/products');
    } catch {
      addToast(isEdit ? 'Update failed' : 'Create failed', 'error');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-4">
        <div className="h-8 w-40 bg-ink-100 dark:bg-ink-800 animate-pulse rounded" />
        <div className="card p-6 space-y-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-10 bg-ink-100 dark:bg-ink-800 animate-pulse rounded" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center gap-3">
        <button onClick={() => navigate('/products')} className="btn-ghost px-2">
          <ArrowLeft size={16} />
        </button>
        <h1 className="text-2xl font-bold text-ink-900 dark:text-white">
          {isEdit ? 'Edit Product' : 'Add Product'}
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="card p-6 space-y-5 max-w-3xl">
        {/* Image Upload */}
        <div>
          <label htmlFor="product-image-upload" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-2">Product Image</label>
          <div className="flex items-center gap-4">
            <div className="w-24 h-24 rounded-xl border border-dashed border-ink-300 dark:border-ink-600 bg-ink-50 dark:bg-ink-900 flex items-center justify-center overflow-hidden">
              {imagePreview ? (
                <OptimizedImage src={imagePreview} alt="Preview" width={400} height={400} loading="eager" />
              ) : (
                <Upload size={20} className="text-ink-400" />
              )}
            </div>
            <label className="btn-ghost cursor-pointer">
              <Upload size={14} /> Upload Image
              <input id="product-image-upload" type="file" accept="image/*" className="hidden" onChange={handleImageChange} />
            </label>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="product-name" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Name *</label>
            <input id="product-name" name="name" value={form.name} onChange={handleChange} required className="input w-full" />
          </div>
          <div>
            <label htmlFor="product-sku" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">SKU</label>
            <input id="product-sku" name="sku" value={form.sku} onChange={handleChange} className="input w-full" />
          </div>
        </div>

        <div>
          <label htmlFor="product-description" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Description</label>
          <textarea id="product-description" name="description" value={form.description} onChange={handleChange} rows={3} className="input w-full" />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="product-category" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Category</label>
            <select id="product-category" name="category" value={form.category} onChange={handleChange} className="input w-full">
              <option value="">Select category</option>
              {categories.map((c) => (
                <option key={c.id || c.name} value={c.name || c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="product-status" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Status</label>
            <select id="product-status" name="status" value={form.status} onChange={handleChange} className="input w-full">
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
              <option value="out_of_stock">Out of Stock</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label htmlFor="product-price" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Price ($)</label>
            <input id="product-price" name="price" type="number" min="0" step="0.01" value={form.price} onChange={handleChange} required className="input w-full" />
          </div>
          <div>
            <label htmlFor="product-stock" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Stock</label>
            <input id="product-stock" name="stock" type="number" min="0" value={form.stock} onChange={handleChange} required className="input w-full" />
          </div>
          <div>
            <label htmlFor="product-discount" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Discount (%)</label>
            <input id="product-discount" name="discount" type="number" min="0" max="100" value={form.discount} onChange={handleChange} className="input w-full" />
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="product-sizes" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Sizes (comma separated)</label>
            <input id="product-sizes" name="sizes" value={form.sizes} onChange={handleChange} placeholder="S, M, L, XL" className="input w-full" />
          </div>
          <div>
            <label htmlFor="product-tags" className="block text-sm font-medium text-ink-700 dark:text-ink-300 mb-1">Tags (comma separated)</label>
            <input id="product-tags" name="tags" value={form.tags} onChange={handleChange} placeholder="summer, new, sale" className="input w-full" />
          </div>
        </div>

        <div className="flex items-center gap-3 pt-2">
          <button type="submit" disabled={saving} className="btn-primary">
            <Save size={16} /> {saving ? 'Saving…' : isEdit ? 'Update Product' : 'Create Product'}
          </button>
          <button type="button" onClick={() => navigate('/products')} className="btn-ghost">
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
