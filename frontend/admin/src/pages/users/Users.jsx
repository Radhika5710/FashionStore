import { useState } from 'react';
import { Search, Shield, Ban, RotateCcw, Trash2 } from 'lucide-react';
import DataTable from '../../components/DataTable.jsx';
import { UsersApi } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';
import { useDataTableWithFilter } from '../../hooks/useDataTable.js';

const ROLE_OPTIONS = ['all', 'admin', 'user', 'manager'];

export default function Users() {
  const { addToast } = useToast();
  const [actionId, setActionId] = useState(null);

  const {
    items: users,
    setItems: setUsers,
    loading,
    search,
    setSearch,
    statusFilter: roleFilter,
    setStatusFilter: setRoleFilter,
    filtered,
  } = useDataTableWithFilter(UsersApi.list, null, {
    statusOptions: ROLE_OPTIONS,
    filterKey: 'role',
    loadErrorMessage: 'Failed to load users',
    filterFn: (rows, search) => {
      if (search.trim()) {
        const q = search.toLowerCase();
        return rows.filter((u) =>
          (u.fullName || u.name || '').toLowerCase().includes(q) ||
          (u.email || '').toLowerCase().includes(q)
        );
      }
      return rows;
    },
  });

  const updateUser = async (id, patch, msg) => {
    setActionId(id);
    try {
      await UsersApi.update(id, patch);
      setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, ...patch } : u)));
      addToast(msg, 'success');
    } catch {
      addToast('Action failed', 'error');
    } finally {
      setActionId(null);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this user?')) return;
    setActionId(id);
    try {
      await UsersApi.delete(id);
      setUsers((prev) => prev.filter((u) => u.id !== id));
      addToast('User deleted', 'success');
    } catch {
      addToast('Delete failed', 'error');
    } finally {
      setActionId(null);
    }
  };

  const columns = [
    {
      key: 'avatar',
      header: '',
      width: '48px',
      render: (r) => (
        <div className="w-8 h-8 rounded-full bg-ink-900 text-white dark:bg-white dark:text-ink-900 flex items-center justify-center text-xs font-bold">
          {(r.fullName || r.name || r.email || '?').split(' ').map((p) => p[0]).join('').slice(0, 2).toUpperCase()}
        </div>
      ),
    },
    { key: 'name', header: 'Name', render: (r) => r.fullName || r.name || r.email },
    { key: 'email', header: 'Email' },
    {
      key: 'role',
      header: 'Role',
      width: '90px',
      render: (r) => (
        <span className={`pill ${(r.role || 'user') === 'admin' ? 'pill-primary' : 'pill'}`}>{r.role || 'user'}</span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      width: '90px',
      render: (r) => (
        <span className={`pill ${r.blocked ? 'pill-danger' : 'pill-success'}`}>{r.blocked ? 'Blocked' : 'Active'}</span>
      ),
    },
    { key: 'orders', header: 'Orders', width: '70px', render: (r) => r.orderCount || 0 },
    {
      key: 'actions',
      header: '',
      width: '160px',
      render: (r) => (
        <div className="flex items-center gap-1">
          <button
            onClick={() => updateUser(r.id, { role: r.role === 'admin' ? 'user' : 'admin' }, `Role changed to ${r.role === 'admin' ? 'user' : 'admin'}`)}
            disabled={actionId === r.id}
            title="Toggle Role"
            className="p-1.5 rounded-md hover:bg-ink-100 dark:hover:bg-ink-700 text-ink-500 dark:text-ink-300 disabled:opacity-40"
          >
            <Shield size={14} />
          </button>
          <button
            onClick={() => updateUser(r.id, { blocked: !r.blocked }, r.blocked ? 'User unblocked' : 'User blocked')}
            disabled={actionId === r.id}
            title={r.blocked ? 'Unblock' : 'Block'}
            className="p-1.5 rounded-md hover:bg-amber-50 dark:hover:bg-amber-900/30 text-amber-500 disabled:opacity-40"
          >
            {r.blocked ? <RotateCcw size={14} /> : <Ban size={14} />}
          </button>
          <button
            onClick={() => updateUser(r.id, { passwordReset: true }, 'Password reset sent')}
            disabled={actionId === r.id}
            title="Reset Password"
            className="p-1.5 rounded-md hover:bg-blue-50 dark:hover:bg-blue-900/30 text-blue-500 disabled:opacity-40"
          >
            <RotateCcw size={14} />
          </button>
          <button
            onClick={() => handleDelete(r.id)}
            disabled={actionId === r.id}
            title="Delete"
            className="p-1.5 rounded-md hover:bg-rose-50 dark:hover:bg-rose-900/30 text-rose-500 disabled:opacity-40"
          >
            <Trash2 size={14} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold text-ink-900 dark:text-white">Users</h1>

      <div className="card p-4 flex flex-col sm:flex-row gap-3">
        <div className="flex items-center gap-2 px-3 h-9 rounded-lg border border-ink-200 dark:border-ink-700 bg-ink-50 dark:bg-ink-900 text-ink-400 flex-1">
          <Search size={16} />
          <input
            type="search"
            placeholder="Search users by name or email…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-transparent text-sm text-ink-900 dark:text-ink-100 placeholder:text-ink-400 focus:outline-none"
          />
        </div>
        <select
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
          className="h-9 px-3 rounded-lg border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 text-sm text-ink-900 dark:text-ink-100 focus:outline-none"
        >
          {ROLE_OPTIONS.map((r) => (
            <option key={r} value={r}>{r === 'all' ? 'All Roles' : r}</option>
          ))}
        </select>
      </div>

      <div className="card">
        {loading ? (
          <div className="p-8 space-y-3">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-10 rounded bg-ink-100 dark:bg-ink-800 animate-pulse" />
            ))}
          </div>
        ) : (
          <DataTable columns={columns} rows={filtered} empty="No users found" getRowKey={(r) => r.id} />
        )}
      </div>
    </div>
  );
}
