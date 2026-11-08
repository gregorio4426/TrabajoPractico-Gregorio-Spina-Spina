import { RouterLink } from '@angular/router';
import { Component, computed, input } from '@angular/core';
export interface ChartPoint {
  label: string;
  value: string | number | null | undefined;
  link?: string;
  note?: string;
}
export function chartNumber(value: ChartPoint['value']): number | null {
  if (value === null || value === undefined || value === '') return null;
  if (!/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(String(value))) return null;
  const digits = String(value)
    .replace(/[.\-+]/g, '')
    .replace(/^0+/, '');
  const n = Number(value);
  return Number.isFinite(n) && Math.abs(n) <= Number.MAX_SAFE_INTEGER && digits.length <= 15
    ? n
    : null;
}
@Component({
  selector: 'app-progress-chart',
  imports: [RouterLink],
  template: `<section class="teacher-chart">
    <h3>{{ title() }}</h3>
    @if (points().length) {
      <div
        class="teacher-chart-scroll"
        tabindex="0"
        role="region"
        [attr.aria-label]="title() + ' · desplazamiento horizontal'"
      >
        <svg
          viewBox="0 0 640 220"
          role="img"
          [attr.aria-label]="title() + ' · representación aproximada; valores exactos en la tabla'"
        >
          <path d="M45 15V180H625" class="chart-axis" />
          <text x="20" y="182">{{ axisText(0) }}</text>
          <text x="6" y="20">{{ axisText(scale()) }}</text>
          @if (kind() === 'line') {
            @for (line of segments(); track $index) {
              <polyline [attr.points]="line" class="chart-line" />
            }
          }
          @for (point of plotted(); track $index) {
            @if (point.y !== null) {
              @if (kind() === 'bar' && point.y !== 180) {
                <rect
                  [attr.x]="point.x - 12"
                  [attr.y]="point.y"
                  width="24"
                  [attr.height]="180 - point.y"
                  class="chart-bar"
                >
                  <title>{{ point.label }}: {{ point.value }}</title>
                </rect>
              } @else {
                <circle [attr.cx]="point.x" [attr.cy]="point.y" r="4" class="chart-dot">
                  <title>{{ point.label }}: {{ point.value }}</title>
                </circle>
              }
            }
          }
          @if (points().length === 1) {
            <text x="335" y="205" text-anchor="middle">{{ shortLabel(points()[0]!.label) }}</text>
          } @else {
            <text x="55" y="205">{{ shortLabel(points()[0]!.label) }}</text>
            <text x="605" y="205" text-anchor="end">
              {{ shortLabel(points()[points().length - 1]!.label) }}
            </text>
          }
        </svg>
      </div>
      <details>
        <summary>Ver valores exactos de {{ title().toLowerCase() }}</summary>
        <div class="admin-table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th scope="col">Registro</th>
                <th scope="col">{{ unit() }}</th>
              </tr>
            </thead>
            <tbody>
              @for (point of points(); track $index) {
                <tr>
                  <td>
                    @if (point.link) {
                      <a
                        class="admin-link"
                        [routerLink]="point.link"
                        [attr.aria-label]="'Ver entrenamiento: ' + point.label"
                        >{{ point.label }}</a
                      >
                    } @else {
                      {{ point.label }}
                    }
                    @if (point.note) {
                      <p class="chart-coverage">{{ point.note }}</p>
                    }
                  </td>
                  <td>
                    {{
                      point.value === null || point.value === undefined ? 'Sin dato' : point.value
                    }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </details>
      <p class="admin-help">
        El trazado es aproximado. Los datos ausentes o fuera de precisión gráfica no se unen ni se
        convierten en cero. Se muestran solo los registros de la página consultada, espaciados por
        registro y no por tiempo transcurrido.
      </p>
    } @else {
      <p class="admin-empty">Sin datos registrados para este gráfico.</p>
    }
  </section>`,
})
export class Chart {
  readonly title = input.required<string>();
  readonly points = input<ChartPoint[]>([]);
  readonly kind = input<'line' | 'bar'>('line');
  readonly unit = input('Valor');
  readonly scale = computed(() =>
    Math.max(1, ...this.points().map((p) => chartNumber(p.value) ?? 0)),
  );
  readonly plotted = computed(() =>
    this.points().map((p, i) => ({
      ...p,
      x:
        55 + (this.points().length === 1 ? 280 : (i * 550) / Math.max(1, this.points().length - 1)),
      y: chartNumber(p.value) === null ? null : 180 - (chartNumber(p.value)! / this.scale()) * 155,
    })),
  );
  readonly segments = computed(() => {
    const lines: string[] = [];
    let current: string[] = [];
    for (const p of this.plotted()) {
      if (p.y === null) {
        if (current.length) lines.push(current.join(' '));
        current = [];
      } else current.push(`${p.x},${p.y}`);
    }
    if (current.length) lines.push(current.join(' '));
    return lines;
  });
  shortLabel(value: string) {
    return /^\d{4}-\d{2}-\d{2}/.test(value) ? value.slice(0, 10) : value.slice(0, 34);
  }
  axisText(value: number) {
    return new Intl.NumberFormat('es-AR', { maximumFractionDigits: 2, notation: 'compact' }).format(
      value,
    );
  }
}
