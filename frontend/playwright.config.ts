import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: 'list',
  timeout: 30000,
  use: {
    baseURL: process.env['GYM_E2E_BASE_URL'] || 'http://127.0.0.1:4200',
    trace: 'off',
    screenshot: 'off',
    launchOptions: process.env['CHROMIUM_PATH']
      ? { executablePath: process.env['CHROMIUM_PATH'] }
      : {},
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env['GYM_E2E_BASE_URL']
    ? undefined
    : {
        command:
          'npm start -- --host 127.0.0.1' +
          (process.env['GYM_E2E_PRODUCTION'] ? ' --configuration production' : ''),
        url: 'http://127.0.0.1:4200',
        reuseExistingServer: !process.env['CI'] && !process.env['GYM_E2E_PRODUCTION'],
        timeout: 60000,
      },
});
