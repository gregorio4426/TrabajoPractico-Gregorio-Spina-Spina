/** Mirrors the existing backend's inclusive UTC statistics window; no metric calculations. */
export function statisticsPeriodError(
  desde: string,
  hasta: string,
  today = new Date().toISOString().slice(0, 10),
): string {
  const calendar = (s: string) =>
    /^\d{4}-\d{2}-\d{2}$/.test(s) &&
    Number.isFinite(Date.parse(s + 'T00:00:00Z')) &&
    new Date(s + 'T00:00:00Z').toISOString().slice(0, 10) === s;
  if ((desde && !calendar(desde)) || (hasta && !calendar(hasta)))
    return 'Ingresá fechas de calendario válidas (AAAA-MM-DD).';
  const end = hasta || today;
  const start =
    desde || new Date(Date.parse(end + 'T00:00:00Z') - 29 * 86400000).toISOString().slice(0, 10);
  if (end < '1000-01-30' || end > '9999-12-31' || start < '1000-01-01')
    return 'Las fechas están fuera del rango soportado por el servidor.';
  const days = (Date.parse(end + 'T00:00:00Z') - Date.parse(start + 'T00:00:00Z')) / 86400000 + 1;
  if (days < 1) return 'La fecha desde debe ser anterior o igual a hasta.';
  return days > 366 ? 'El período debe contener entre 1 y 366 días inclusivos.' : '';
}
