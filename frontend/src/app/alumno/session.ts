import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom, Observable } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AlumnoApi } from './alumno-api';
import { AuthService } from '../core/auth.service';
import * as D from '../admin/contracts';
import { Confirm } from '../profesor/confirm';
import { RecordView } from '../admin/record-view';
import { notBlank, positiveInteger, optionalWeight } from '../profesor/utils';
import {
  alumnoError,
  isClosed,
  validSeries,
  nextSeriesNumber,
  text,
  pastOrToday,
  utcDate,
} from './utils';
type Action = 'add' | 'finish' | 'correct' | 'annul' | 'restore' | 'date';
@Component({
  selector: 'app-student-session',
  imports: [RouterLink, ReactiveFormsModule, Confirm, RecordView],
  templateUrl: './session.html',
})
export class StudentSession {
  private readonly api = inject(AlumnoApi);
  private readonly injector = inject(Injector);
  private readonly recordingEntry = viewChild<ElementRef<HTMLElement>>('recordingEntry');
  private readonly actualReps = viewChild<ElementRef<HTMLInputElement>>('actualReps');
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly confirmation = viewChild.required<Confirm>('confirmation');
  readonly id = signal(0);
  readonly data = signal<D.SesionResponse | null>(null);
  readonly day = signal<D.DiaRutinaResponse | null>(null);
  readonly audit = signal<D.AuditoriaEntrenamientoResponse[]>([]);
  readonly last = signal<D.UltimoEjercicioResponse | null>(null);
  readonly lastMessage = signal('');
  readonly showAudit = signal(false);
  readonly loading = signal(false);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly action = signal<Action>('add');
  readonly block = signal<D.DiaEjercicioResponse | null>(null);
  readonly selectedSeries = signal<D.SerieResponse | null>(null);
  readonly edit = signal<'series' | 'date' | null>(null);
  readonly text = text;
  readonly validSeries = validSeries;
  readonly today = utcDate();
  private seq = 0;
  private lastSeq = 0;
  readonly closed = computed(() => (this.data() ? isClosed(this.data()!) : false));
  readonly validCount = computed(() => (this.data()?.series ?? []).filter(validSeries).length);
  readonly addForm = this.fb.group({
    numeroSerie: [1, [Validators.required, positiveInteger]],
    repsHechas: [null as number | null, [Validators.required, positiveInteger]],
    pesoUsado: [null as number | null, optionalWeight],
  });
  readonly correction = this.fb.group({
    numeroSerie: [1, [Validators.required, positiveInteger]],
    repsHechas: [null as number | null, [Validators.required, positiveInteger]],
    pesoUsado: [null as number | null, optionalWeight],
    motivo: ['', [Validators.required, notBlank, Validators.maxLength(500)]],
  });
  readonly dateForm = this.fb.nonNullable.group({
    fecha: ['', [Validators.required, pastOrToday]],
    motivo: ['', [Validators.required, notBlank, Validators.maxLength(500)]],
  });
  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((p) => {
      this.id.set(Number(p.get('id')));
      void this.load();
    });
  }
  async load() {
    const seq = ++this.seq;
    ++this.lastSeq;
    this.loading.set(true);
    this.error.set('');
    this.data.set(null);
    this.day.set(null);
    this.audit.set([]);
    this.last.set(null);
    this.lastMessage.set('');
    this.block.set(null);
    this.edit.set(null);
    this.selectedSeries.set(null);
    try {
      if (!Number.isSafeInteger(this.id()) || this.id() < 1) throw new Error('ID no válido');
      const session = await firstValueFrom(this.api.session(this.id()));
      if (
        this.auth.session()?.rol !== 'ALUMNO' ||
        session.alumnoId !== this.auth.session()?.perfilId
      ) {
        this.error.set('Solo podés consultar tus propias sesiones.');
        return;
      }
      const [routine, audit] = await Promise.all([
        session.rutinaId && session.numeroVersion
          ? firstValueFrom(this.api.version(session.rutinaId, session.numeroVersion))
          : Promise.resolve(null),
        firstValueFrom(this.api.audit(this.id())),
      ]);
      if (seq !== this.seq) return;
      this.day.set(routine?.dias?.find((d) => d.id === session.diaRutinaId) ?? null);
      this.audit.set(audit);
      this.dateForm.patchValue({ fecha: session.fechaEfectiva ?? session.fecha ?? '', motivo: '' });
      this.data.set(session);
    } catch (e) {
      if (seq === this.seq) {
        this.data.set(null);
        this.day.set(null);
        this.audit.set([]);
        this.error.set(alumnoError(e));
      }
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
  choose(block: D.DiaEjercicioResponse) {
    if (this.pending() || this.closed()) return;
    this.block.set(block);
    this.edit.set(null);
    this.selectedSeries.set(null);
    this.error.set('');
    this.success.set('');
    this.last.set(null);
    this.lastMessage.set('');
    this.addForm.reset({
      numeroSerie: nextSeriesNumber(this.data()?.series ?? [], block.ejercicioId!),
      repsHechas: null,
      pesoUsado: null,
    });
    void this.loadLast(block.ejercicioId!);
    afterNextRender(
      () => {
        const entry = this.recordingEntry()?.nativeElement;
        if (entry && typeof entry.scrollIntoView === 'function')
          entry.scrollIntoView({ block: 'start', behavior: 'auto' });
        this.actualReps()?.nativeElement.focus({ preventScroll: true });
      },
      { injector: this.injector },
    );
  }
  async loadLast(exerciseId: number) {
    const seq = ++this.lastSeq;
    try {
      const value = await firstValueFrom(this.api.last(exerciseId));
      if (seq === this.lastSeq) this.last.set(value);
    } catch (e) {
      if (seq !== this.lastSeq) return;
      if (e instanceof HttpErrorResponse && e.status === 404)
        this.lastMessage.set('No hay una ejecución finalizada anterior de este ejercicio.');
      else this.lastMessage.set(alumnoError(e));
    }
  }
  editSeries(series: D.SerieResponse, action: 'correct' | 'annul' | 'restore') {
    if (this.pending()) return;
    this.selectedSeries.set(series);
    this.block.set(null);
    this.edit.set('series');
    this.action.set(action);
    this.error.set('');
    this.success.set('');
    this.correction.reset({
      numeroSerie: series.numeroSerie ?? 1,
      repsHechas: series.repsHechas ?? null,
      pesoUsado: series.pesoUsado ?? null,
      motivo: '',
    });
  }
  editDate() {
    if (this.pending()) return;
    this.edit.set('date');
    this.action.set('date');
    this.block.set(null);
    this.selectedSeries.set(null);
    this.dateForm.patchValue({
      fecha: this.data()?.fechaEfectiva ?? this.data()?.fecha ?? '',
      motivo: '',
    });
    this.error.set('');
    this.success.set('');
  }
  ask(action: Action) {
    if (this.pending() || !this.data()) return;
    this.error.set('');
    this.action.set(action);
    if (action === 'add') {
      this.addForm.markAllAsTouched();
      if (this.closed() || !this.block() || this.addForm.invalid) {
        this.error.set(
          'Completá número de serie y repeticiones con enteros positivos. El peso puede quedar vacío, ser cero o ser positivo.',
        );
        return;
      }
    }
    if (action === 'finish' && (this.closed() || this.validCount() === 0)) {
      this.error.set('No podés finalizar sin al menos una serie válida vigente.');
      return;
    }
    if (['correct', 'restore', 'annul'].includes(action)) {
      this.correction.markAllAsTouched();
      const selected = this.selectedSeries();
      if (
        !selected ||
        selected.revision === null ||
        selected.revision === undefined ||
        !Number.isSafeInteger(selected.revision) ||
        selected.revision < 0
      ) {
        this.error.set('Recargá la sesión para consultar la revisión de esta serie.');
        return;
      }
      if (action === 'annul' ? this.correction.controls.motivo.invalid : this.correction.invalid) {
        this.error.set('Revisá los datos y escribí un motivo de hasta 500 caracteres.');
        return;
      }
    }
    if (action === 'date') {
      this.dateForm.markAllAsTouched();
      if (this.dateForm.invalid || !Number.isSafeInteger(this.data()?.revision)) {
        this.error.set(
          'Ingresá una fecha no futura y un motivo. Recargá si falta la revisión de sesión.',
        );
        return;
      }
    }
    this.confirmation().open();
  }
  confirmMessage() {
    switch (this.action()) {
      case 'add':
        return `Se registrará la serie ${this.addForm.controls.numeroSerie.value} de ${this.block()?.ejercicioNombre}, con ${this.addForm.controls.repsHechas.value} repeticiones y peso ${this.addForm.controls.pesoUsado.value ?? 'sin registrar'}. No se completan resultados a partir de los sugeridos.`;
      case 'finish':
        return 'Se finalizará el entrenamiento con los resultados guardados, completo o parcial. Después no se pueden agregar series; sí corregir registros existentes. El servidor determinará el cumplimiento del día.';
      case 'date':
        return `Se corregirá la fecha efectiva a ${this.dateForm.controls.fecha.value}. La fecha original y la asignación histórica se conservan. Motivo: ${this.dateForm.controls.motivo.value}`;
      default:
        return `Operación ${this.action() === 'annul' ? 'anular' : this.action() === 'restore' ? 'restaurar' : 'corregir'} sobre la serie #${this.selectedSeries()?.id}. Motivo: ${this.correction.controls.motivo.value}. El cambio quedará auditado; no se borra el historial.`;
    }
  }
  async save() {
    if (this.pending() || !this.data()) return;
    this.pending.set(true);
    this.error.set('');
    this.success.set('');
    const action = this.action();
    let request: Observable<unknown>;
    try {
      if (action === 'add') {
        const v = this.addForm.getRawValue();
        request = this.api.addSeries(this.id(), {
          diaEjercicioId: this.block()!.id!,
          numeroSerie: Number(v.numeroSerie),
          repsHechas: Number(v.repsHechas),
          pesoUsado: v.pesoUsado == null || String(v.pesoUsado) === '' ? null : Number(v.pesoUsado),
        });
      } else if (action === 'finish') request = this.api.finish(this.id());
      else if (action === 'date')
        request = this.api.correctDate(this.id(), {
          ...this.dateForm.getRawValue(),
          revision: this.data()!.revision!,
        });
      else {
        const s = this.selectedSeries()!,
          v = this.correction.getRawValue();
        if (action === 'annul')
          request = this.api.annul(this.id(), s.id!, {
            revision: s.revision!,
            motivo: v.motivo!.trim(),
          });
        else {
          const body: D.CorreccionSerieRequest = {
            numeroSerie: Number(v.numeroSerie),
            repsHechas: Number(v.repsHechas),
            pesoUsado:
              v.pesoUsado == null || String(v.pesoUsado) === '' ? null : Number(v.pesoUsado),
            revision: s.revision!,
            motivo: v.motivo!.trim(),
          };
          request =
            action === 'restore'
              ? this.api.restore(this.id(), s.id!, body)
              : this.api.correctSeries(this.id(), s.id!, body);
        }
      }
      await firstValueFrom(request);
      await this.load();
      if (!this.error())
        this.success.set(
          action === 'finish'
            ? 'Entrenamiento finalizado. Consultá el cumplimiento informado por el servidor.'
            : action === 'add'
              ? 'Serie registrada correctamente.'
              : 'Cambio guardado y auditado correctamente.',
        );
    } catch (e) {
      this.error.set(
        alumnoError(e) +
          (e instanceof HttpErrorResponse && [0, 500].includes(e.status)
            ? ' No se repetirá la escritura automáticamente. Recargá y comprobá si el registro se guardó.'
            : ''),
      );
    } finally {
      this.pending.set(false);
    }
  }
}
