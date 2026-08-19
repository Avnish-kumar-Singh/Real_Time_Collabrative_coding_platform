import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/room': 'http://localhost:8080',
      '/rooms': 'http://localhost:8080',
      '/auth': 'http://localhost:8080',
      '/execute': 'http://localhost:8080',
      '/code': {
        target: 'ws://localhost:8080',
        ws: true,
      },
      '/screen': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
});
