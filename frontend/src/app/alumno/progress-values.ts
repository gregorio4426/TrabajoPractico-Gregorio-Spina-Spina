import { SesionResponse } from '../admin/contracts';
import { validSeries, validCalendarDate } from './utils';
export function shiftDay(date: string, count: number) {
  const d = new Date(date + 'T12:00:00Z');
  d.setUTCDate(d.getUTCDate() + count);
  return d.toISOString().slice(0, 10);
}
export function monday(date: string) {
  const d = new Date(date + 'T12:00:00Z');
  return shiftDay(date, -((d.getUTCDay() + 6) % 7));
}
export function completedWorkouts(sessions: SesionResponse[]) {
  return sessions.filter(
    (s) =>
      validCalendarDate(s.fechaEfectiva ?? s.fecha ?? '') &&
      s.finalizada &&
      (Number(s.seriesRealizadas) > 0 || s.series?.some(validSeries)),
  );
}
export function progressValues(sessions: SesionResponse[], today: string) {
  const done = completedWorkouts(sessions).filter((s) => (s.fechaEfectiva ?? s.fecha!) <= today),
    start = monday(today),
    dates = new Set(done.map((s) => s.fechaEfectiva ?? s.fecha).filter((s): s is string => !!s));
  const weeks = new Set([...dates].map(monday));
  let consecutive = 0,
    week = weeks.has(start) ? start : shiftDay(start, -7);
  while (weeks.has(week)) {
    consecutive++;
    week = shiftDay(week, -7);
  }
  return {
    weekCount: done.filter((s) => {
      const d = s.fechaEfectiva ?? s.fecha ?? '';
      return d >= start && d <= today;
    }).length,
    monthCount: done.filter(
      (s) => (s.fechaEfectiva ?? s.fecha ?? '').slice(0, 7) === today.slice(0, 7),
    ).length,
    days: ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'].map((label, i) => ({
      label,
      date: shiftDay(start, i),
      trained: dates.has(shiftDay(start, i)),
    })),
    streak: consecutive,
    weeks: Array.from({ length: 8 }, (_, i) => {
      const startOfWeek = shiftDay(start, (i - 7) * 7);
      return {
        date: startOfWeek,
        count: done.filter((s) => monday(s.fechaEfectiva ?? s.fecha!) === startOfWeek).length,
      };
    }),
  };
}
