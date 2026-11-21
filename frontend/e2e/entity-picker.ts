import { Page, expect } from '@playwright/test';
export async function selectEntity(page: Page, label: string, query: string) {
  const exercise = /^Ejercicio del día (\d+), fila (\d+)$/.exec(label);
  const scope = exercise
    ? page
        .getByRole('group', { name: 'Día ' + exercise[1], exact: true })
        .getByRole('group', { name: 'Ejercicio ' + exercise[2], exact: true })
    : page;
  if (exercise) {
    const day = page.getByRole('group', { name: 'Día ' + exercise[1], exact: true });
    while ((await day.locator('.compact-exercise').count()) < Number(exercise[2])) {
      await day.getByRole('button', { name: '+ Agregar ejercicio', exact: true }).last().click();
    }
    label = 'Ejercicio';
  }
  if (exercise) {
    const input = scope.getByRole('combobox', { name: 'Ejercicio', exact: true });
    await input.fill(query);
    await scope
      .getByRole('option', { name: new RegExp(query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')) })
      .click();
    return;
  }

  const trigger = scope.getByRole('button', {
    name: new RegExp('^' + label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + ':'),
  });
  await trigger.click();
  const picker = exercise
    ? scope.locator('app-entity-picker')
    : page.locator('app-entity-picker').filter({ has: trigger });
  await picker.locator('input[type=search]').fill(query);
  const option = picker.locator('.entity-picker-option').filter({ hasText: query });
  await expect(option).toHaveCount(1);
  await option.click();
}

export function routineField(page: Page, label: string) {
  const match = /^(Series|Vueltas|Repeticiones) día (\d+), fila (\d+)$/.exec(label);
  if (match) {
    const row = page
      .getByRole('group', { name: 'Día ' + match[2], exact: true })
      .getByRole('group', { name: 'Ejercicio ' + match[3], exact: true });
    if (match[1] === 'Repeticiones') return row.getByLabel('Repeticiones', { exact: true });
    return row
      .locator('xpath=ancestor::section[contains(@class,"routine-block")][1]')
      .getByRole('spinbutton', { name: /^(Series|Vueltas)$/ });
  }
  const day = /^(Nombre|Orden) del día (\d+)$/.exec(label);
  if (day)
    return page
      .getByRole('group', { name: 'Día ' + day[2], exact: true })
      .getByLabel(day[1] + ' del día', { exact: true });
  throw new Error('Campo de rutina no reconocido');
}

export async function selectMuscle(page: Page, label: string, name: string) {
  const input = page.getByRole('combobox', { name: label, exact: true });
  await input.fill(name);
  await page
    .locator('app-muscle-picker')
    .filter({ has: input })
    .getByRole('option', { name, exact: true })
    .click();
}
