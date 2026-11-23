import { DiaEjercicioResponse, SerieResponse } from '../admin/contracts';
export const isTimed = (value: {tipoMedicion?: string | null} | null | undefined) => value?.tipoMedicion === 'SEGUNDOS';
export const unitLabel = (value: {tipoMedicion?: string | null} | null | undefined) => isTimed(value) ? 'segundos' : 'repeticiones';
export const suggested = (e: DiaEjercicioResponse) => isTimed(e) ? e.segundosPersonalizados ?? e.segundosSugeridos : e.repsPersonalizadas ?? e.repsSugeridas;
export const performed = (s: SerieResponse) => isTimed(s) ? s.segundosHechos : s.repsHechas;
export function dateAR(value: string | null | undefined) { if(!value)return ''; const match=/^(\d{4})-(\d{2})-(\d{2})/.exec(value); return match ? `${match[3]}/${match[2]}/${match[1]}` : ''; }
