import { Metricas } from '../admin/contracts';
/** Preserve backend decimal strings and coverage. Never derive totals from rounded plot values. */
export function volumeCoverage(data: Metricas | null | undefined): string {
  if (data?.volumenRegistrado == null) return '(sin pesos conocidos)';
  return data.volumenCompleto
    ? '(completo)'
    : `(parcial · ${data.seriesSinPeso ?? 'sin dato de'} series sin peso)`;
}
