import { Page, expect } from '@playwright/test';
export async function selectEntity(page: Page, label: string, query: string) {
  const trigger = page.getByRole('button', {
    name: new RegExp('^' + label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ':'),
  });
  await trigger.click();
  const picker = page.locator('app-entity-picker').filter({ has: trigger });
  await picker.locator('input[type=search]').fill(query);
  const option = picker.locator('.entity-picker-option').filter({ hasText: query });
  await expect(option).toHaveCount(1);
  await option.click();
}
