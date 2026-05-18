import { Routes, Route, Navigate } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import ProtectedRoute from '../router/ProtectedRoute.jsx';
import MainLayout from '../layouts/MainLayout.jsx';

// Lazy load all pages for better initial bundle size
const Login = lazy(() => import('../pages/Login.jsx'));
const Register = lazy(() => import('../pages/Register.jsx'));
const Dashboard = lazy(() => import('../pages/dashboard/Dashboard.jsx'));
const Products = lazy(() => import('../pages/products/Products.jsx'));
const ProductForm = lazy(() => import('../pages/products/ProductForm.jsx'));
const Inventory = lazy(() => import('../pages/inventory/Inventory.jsx'));
const Orders = lazy(() => import('../pages/orders/Orders.jsx'));
const Users = lazy(() => import('../pages/users/Users.jsx'));
const Categories = lazy(() => import('../pages/categories/Categories.jsx'));
const Coupons = lazy(() => import('../pages/coupons/Coupons.jsx'));
const Settings = lazy(() => import('../pages/settings/Settings.jsx'));

function RouteFallback() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-ink-50 dark:bg-ink-900">
      <div className="text-center">
        <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4" />
        <p className="text-ink-600 dark:text-ink-300 text-sm">Loading...</p>
      </div>
    </div>
  );
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public routes - no layout */}
      <Route path="/login" element={<Suspense fallback={<RouteFallback />}><Login /></Suspense>} />
      <Route path="/register" element={<Suspense fallback={<RouteFallback />}><Register /></Suspense>} />

      {/* Protected routes */}
      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          {/* Root redirect */}
          <Route index element={<Navigate to="dashboard" replace />} />
          
          {/* Dashboard */}
          <Route path="dashboard" element={<Suspense fallback={<RouteFallback />}><Dashboard /></Suspense>} />
          
          {/* Products */}
          <Route path="products" element={<Suspense fallback={<RouteFallback />}><Products /></Suspense>} />
          <Route path="products/new" element={<Suspense fallback={<RouteFallback />}><ProductForm /></Suspense>} />
          <Route path="products/:id/edit" element={<Suspense fallback={<RouteFallback />}><ProductForm /></Suspense>} />
          
          {/* Inventory */}
          <Route path="inventory" element={<Suspense fallback={<RouteFallback />}><Inventory /></Suspense>} />
          
          {/* Orders */}
          <Route path="orders" element={<Suspense fallback={<RouteFallback />}><Orders /></Suspense>} />
          
          {/* Users */}
          <Route path="users" element={<Suspense fallback={<RouteFallback />}><Users /></Suspense>} />
          
          {/* Categories */}
          <Route path="categories" element={<Suspense fallback={<RouteFallback />}><Categories /></Suspense>} />
          
          {/* Coupons */}
          <Route path="coupons" element={<Suspense fallback={<RouteFallback />}><Coupons /></Suspense>} />
          
          {/* Settings */}
          <Route path="settings" element={<Suspense fallback={<RouteFallback />}><Settings /></Suspense>} />
        </Route>
      </Route>

      {/* Catch-all - redirect to dashboard */}
      <Route path="*" element={<Navigate to="dashboard" replace />} />
    </Routes>
  );
}
