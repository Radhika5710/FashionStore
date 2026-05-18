import { useEffect, useState, useRef } from 'react';
import {
  DollarSign, ShoppingBag, Package, Users, Clock, AlertTriangle,
} from 'lucide-react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  BarChart, Bar,
} from 'recharts';
import StatCard from '../../components/StatCard.jsx';
import DataTable from '../../components/DataTable.jsx';
import StatusBadge from '../../components/StatusBadge.jsx';
import ChartErrorBoundary from '../../components/ChartErrorBoundary.jsx';
import { DashboardApi, OrdersApi, UsersApi, ProductsApi } from '../../api/client.js';
import { useToast } from '../../context/ToastContext.jsx';

const SALES_MOCK = [
  { name: 'Mon', revenue: 1200, orders: 12 },
  { name: 'Tue', revenue: 1900, orders: 18 },
  { name: 'Wed', revenue: 1500, orders: 15 },
  { name: 'Thu', revenue: 2200, orders: 22 },
  { name: 'Fri', revenue: 2800, orders: 28 },
  { name: 'Sat', revenue: 3400, orders: 34 },
  { name: 'Sun', revenue: 2900, orders: 29 },
];

export default function Dashboard() {
  const [stats, setStats] = useState({
    revenue: '$0',
    orders: 0,
    products: 0,
    customers: 0,
    pending: 0,
    lowStock: 0,
  });
  const [salesData, setSalesData] = useState(SALES_MOCK);
  const [recentOrders, setRecentOrders] = useState([]);
  const [recentUsers, setRecentUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const { addToast } = useToast();
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    (async () => {
      try {
        const [dash, prodRes, userRes, orderRes] = await Promise.allSettled([
          DashboardApi.fetch(),
          ProductsApi.list(),
          UsersApi.list(),
          OrdersApi.list(5),
        ]);

        const products = prodRes.status === 'fulfilled' ? prodRes.value?.length || 0 : 0;
        const customers = userRes.status === 'fulfilled' ? userRes.value?.length || 0 : 0;
        const orders = orderRes.status === 'fulfilled' ? orderRes.value || [] : [];

        // Use dashboard API data if available
        const dashData = dash.status === 'fulfilled' ? dash.value : {};

        if (mountedRef.current) {
          setStats((s) => ({
            ...s,
            revenue: dashData.revenue ? `$${Number(dashData.revenue).toLocaleString()}` : s.revenue,
            orders: dashData.orders || orders.length,
            products,
            customers,
            pending: orders.filter((o) => (o.status || '').toLowerCase() === 'pending').length,
            lowStock: dashData.lowStock || 0,
          }));
          
          // Use real sales data if available
          if (dashData.salesData && Array.isArray(dashData.salesData)) {
            setSalesData(dashData.salesData);
          }
          
          setRecentOrders(orders.slice(0, 5));
          setRecentUsers(userRes.status === 'fulfilled' ? (userRes.value || []).slice(0, 5) : []);
        }
      } catch (err) {
        console.error('Dashboard data loading error:', err);
        if (mountedRef.current) {
          // Check if it's a network error (backend unavailable)
          const isNetworkError = !err.response && err.message;
          if (isNetworkError) {
            addToast('Backend unavailable - showing cached data', 'warning');
          } else {
            addToast('Failed to load dashboard data', 'error');
          }
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
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="p-5 h-24 rounded-2xl border border-ink-200/70 dark:border-ink-700/70 bg-white/70 dark:bg-ink-800/70 backdrop-blur-xl animate-pulse"
            />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 section-lg">
      {/* Hero Section */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[var(--color-primary)] to-[var(--color-primary-hover)] p-8 md:p-12">
        <div className="relative z-10">
          <p className="text-overline mb-3 text-white/90">Executive Overview</p>
          <h1 className="text-h1 text-white mb-4">Welcome Back</h1>
          <p className="text-body-lg text-white/80 max-w-2xl">
            Monitor your fashion store performance with real-time analytics and insights
          </p>
        </div>
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2" />
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <StatCard icon={DollarSign} label="Total Revenue" value={stats.revenue} delta={12.5} accent="primary" />
        <StatCard icon={ShoppingBag} label="Total Orders" value={stats.orders} delta={8.2} accent="success" />
        <StatCard icon={Package} label="Products" value={stats.products} accent="info" />
        <StatCard icon={Users} label="Customers" value={stats.customers} accent="neutral" />
        <StatCard icon={Clock} label="Pending Orders" value={stats.pending} accent="warning" />
        <StatCard icon={AlertTriangle} label="Low Stock Items" value={stats.lowStock} accent="warning" />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="card">
          <div className="card-head">
            <h3 className="text-overline">Revenue Trend</h3>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-emerald-500 shadow-sm" />
              <span className="text-body-sm text-ink-500 dark:text-ink-400">This week</span>
            </div>
          </div>
          <div className="card-body">
            <div className="h-80">
              <ChartErrorBoundary>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={salesData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="colorRev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="var(--color-primary)" stopOpacity={0.5} />
                      <stop offset="95%" stopColor="var(--color-primary)" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="currentColor" className="stroke-ink-200 dark:stroke-ink-700" opacity={0.5} />
                  <XAxis 
                    dataKey="name" 
                    stroke="currentColor" 
                    className="text-ink-400 dark:text-ink-500"
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis 
                    stroke="currentColor" 
                    className="text-ink-400 dark:text-ink-500"
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(value) => `$${value}`}
                  />
                  <Tooltip 
                    contentStyle={{ 
                      borderRadius: 12, 
                      border: 'none', 
                      boxShadow: 'var(--shadow-xl)',
                      background: 'white',
                      color: 'var(--color-text-primary)',
                      padding: '12px 16px',
                    }}
                    formatter={(value) => [`$${value}`, 'Revenue']}
                  />
                  <Area 
                    type="monotone" 
                    dataKey="revenue" 
                    stroke="var(--color-primary)" 
                    strokeWidth={2.5}
                    fillOpacity={1} 
                    fill="url(#colorRev)" 
                    animationDuration={1000}
                    animationEasing="ease-out"
                  />
                </AreaChart>
              </ResponsiveContainer>
              </ChartErrorBoundary>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-head">
            <h3 className="text-overline">Orders Trend</h3>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-indigo-500 shadow-sm" />
              <span className="text-body-sm text-ink-500 dark:text-ink-400">This week</span>
            </div>
          </div>
          <div className="card-body">
            <div className="h-80">
              <ChartErrorBoundary>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={salesData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="currentColor" className="stroke-ink-200 dark:stroke-ink-700" opacity={0.5} />
                  <XAxis 
                    dataKey="name" 
                    stroke="currentColor" 
                    className="text-ink-400 dark:text-ink-500"
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis 
                    stroke="currentColor" 
                    className="text-ink-400 dark:text-ink-500"
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                  />
                  <Tooltip 
                    contentStyle={{ 
                      borderRadius: 12, 
                      border: 'none', 
                      boxShadow: 'var(--shadow-xl)',
                      background: 'white',
                      color: 'var(--color-text-primary)',
                      padding: '12px 16px',
                    }}
                    formatter={(value) => [value, 'Orders']}
                  />
                  <Bar 
                    dataKey="orders" 
                    fill="var(--color-primary)" 
                    radius={[8, 8, 0, 0]}
                    animationDuration={800}
                    animationEasing="ease-out"
                  />
                </BarChart>
              </ResponsiveContainer>
              </ChartErrorBoundary>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="card">
          <div className="card-head">
            <h2 className="text-h4">Recent Orders</h2>
          </div>
          <div className="card-body">
            <DataTable
              columns={[
                { key: 'id', header: 'Order ID', width: '100px' },
                { key: 'customer', header: 'Customer' },
                { key: 'total', header: 'Total', width: '80px', render: (r) => `$${(r.total || 0).toFixed(2)}` },
                { key: 'status', header: 'Status', width: '100px', render: (r) => <StatusBadge status={r.status} /> },
              ]}
              rows={recentOrders}
              empty="No recent orders"
              getRowKey={(r) => r.id}
            />
          </div>
        </div>

        <div className="card">
          <div className="card-head">
            <h2 className="text-h4">Recent Users</h2>
          </div>
          <div className="card-body">
            <DataTable
              columns={[
                { key: 'name', header: 'Name', render: (r) => r.fullName || r.name || r.email },
                { key: 'email', header: 'Email' },
                { key: 'role', header: 'Role', width: '80px', render: (r) => <span className="badge">{r.role || 'user'}</span> },
              ]}
              rows={recentUsers}
              empty="No recent users"
              getRowKey={(r) => r.id}
            />
          </div>
        </div>
      </div>
    </div>
  );
}
