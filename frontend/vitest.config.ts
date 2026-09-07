import { defineConfig } from 'vitest/config'
import * as path from 'path'
import react from '@vitejs/plugin-react'

// Dedicated Vitest config so the test runner gets a jsdom environment and the
// same `@` alias the app uses, without pulling in the dev-server proxy config.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    testTimeout: 15000,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    clearMocks: true,
    restoreMocks: true,
  },
})
