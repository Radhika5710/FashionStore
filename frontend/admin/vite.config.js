import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// Vite dev server runs on http://localhost:5173
// Backend (Tomcat) runs on configurable backend URL
// All /api requests are transparently proxied so the JSESSIONID cookie is shared.
// 
// Production deployment:
// - Admin frontend served at /admin/ via Nginx reverse proxy
// - Vite base path: /admin/ (for asset resolution)
// - React Router basename: /admin (for client-side routing)
// - Nginx try_files fallback: /index.html (for SPA routing)
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backendTarget = env.VITE_BACKEND_TARGET || 'http://localhost:8080';
  const isProduction = mode === 'production';
  
  return {
    // Base path for production build - must match where admin is served
    // In production: served through Nginx at /admin/
    // In development: served from root /
    base: isProduction ? '/admin/' : '/',
    
    plugins: [react()],
    
    server: {
      port: 5173,
      strictPort: false,
      host: true,
      // Proxy API requests to backend in development
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
          secure: false,
          configure: (proxy, options) => {
            proxy.on('proxyReq', (proxyReq, req, res) => {
              // Log proxy requests for debugging
              console.log(`[Proxy] ${req.method} ${req.url} -> ${options.target}${req.url}`);
            });
          },
        },
      },
    },
    
    build: {
      outDir: 'dist',
      sourcemap: false,
      chunkSizeWarningLimit: 600,
      target: 'es2020',
      cssCodeSplit: true,
      // Ensure assets are generated with correct paths
      assetsDir: 'assets',
      // Optimize chunk splitting for better caching and smaller bundles
      rollupOptions: {
        output: {
          manualChunks: (id) => {
            // Define explicit chunks to avoid circular dependencies
            const chunks = {
              'react-vendor': ['react', 'react-dom', 'react-router-dom'],
              'charts-vendor': ['recharts'],
              'api-vendor': ['axios'],
              'icons-vendor': ['lucide-react'],
            };

            // Check if module belongs to a specific chunk
            for (const [chunkName, keywords] of Object.entries(chunks)) {
              if (keywords.some(keyword => id.includes(keyword))) {
                return chunkName;
              }
            }

            // No specific chunk - return undefined to let Rollup handle it
            return undefined;
          },
          // Ensure proper asset naming for production with content-based hashing
          entryFileNames: 'assets/[name]-[hash].js',
          chunkFileNames: 'assets/[name]-[hash].js',
          assetFileNames: 'assets/[name]-[hash][extname]',
        },
      },
      // Use default minification (esbuild)
      minify: 'esbuild',
      // Enable compression
      reportCompressedSize: true,
      // Enable CSS minification
      cssMinify: true,
    },
  };
});
