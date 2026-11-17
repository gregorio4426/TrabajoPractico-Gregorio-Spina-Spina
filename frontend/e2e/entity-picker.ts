import { Page, expect } from '@playwright/test';
export async function selectEntity(page: Page, label: string, query: string) {
  const exercise = /^Ejercicio del día (\d+), fila (\d+)$/.exec(label);
  const scope = exercise
    ? page
        .getByRole('group', { name: 'Día ' + exercise[1], exact: true })
        .getByRole('group', { name: 'Ejercicio ' + exercise[2], exact: true })
    : page;
  if (exercise) label = 'Ejercicio';
  const trigger = scope.getByRole('button', {
    name: new RegExp('^' + label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ':'),
  });
  await trigger.click();
  const picker = page.locator('app-entity-picker').filter({ has: trigger });
  await picker.locator('input[type=search]').fill(query);
  const option = picker.locator('.entity-picker-option').filter({ hasText: query });
  await expect(option).toHaveCount(1);
  await option.click();
}

export function routineField(page: Page, label: string) {
  const match = /^(Series|Vueltas|Repeticiones) día (\d+), fila (\d+)$/.exec(label);
  if (match)
    return page
      .getByRole('group', { name: 'Día ' + match[2], exact: true })
      .getByRole('group', { name: 'Ejercicio ' + match[3], exact: true })
      .getByLabel(match[1] === 'Vueltas' ? 'Series' : match[1], { exact: true });
  const day = /^(Nombre|Orden) del día (\d+)$/.exec(label);
  if (day)
    return page
      .getByRole('group', { name: 'Día ' + day[2], exact: true })
      .getByLabel(day[1] + ' del día', { exact: true });
  throw new Error('Campo de rutina no reconocido');
}
