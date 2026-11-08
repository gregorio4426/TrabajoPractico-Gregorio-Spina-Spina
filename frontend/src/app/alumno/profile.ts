import { Component, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AlumnoApi } from './alumno-api';
import * as D from '../admin/contracts';
import { LEVELS, OBJECTIVES } from '../admin/catalog';
import { alumnoError, optionalPast } from './utils';
import { AuthService } from '../core/auth.service';
import { Confirm } from '../profesor/confirm';
import { notBlank, passwordValidators } from '../profesor/utils';
@Component({
  selector: 'app-student-profile',
  imports: [ReactiveFormsModule, Confirm],
  templateUrl: './profile.html',
})
export class StudentProfile {
  private readonly api = inject(AlumnoApi);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  private readonly confirmation = viewChild.required<Confirm>('confirmation');
  readonly mode = signal<'profile' | 'email' | 'password'>('profile');
  readonly loading = signal(false);
  readonly pending = signal(false);
  readonly loaded = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  private seq = 0;
  readonly levels = LEVELS;
  readonly objectives = OBJECTIVES;
  readonly age = signal<number | null>(null);
  readonly profile = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, notBlank, Validators.maxLength(20)]],
    apellido: ['', [Validators.required, notBlank, Validators.maxLength(20)]],
    fechaDeNacimiento: ['', optionalPast],
    nivel: ['', Validators.required],
    objetivo: ['', Validators.required],
    emailContacto: ['', [Validators.email, Validators.maxLength(150)]],
  });
  readonly email = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
    passwordActual: ['', Validators.required],
  });
  readonly password = this.fb.nonNullable.group(
    {
      passwordActual: ['', Validators.required],
      nuevaPassword: ['', passwordValidators],
      confirmacion: ['', Validators.required],
    },
    {
      validators: (group) =>
        group.get('nuevaPassword')?.value === group.get('confirmacion')?.value
          ? null
          : { mismatch: true },
    },
  );
  constructor() {
    void this.load();
  }
  async load() {
    const seq = ++this.seq;
    this.loading.set(true);
    this.loaded.set(false);
    this.error.set('');
    try {
      const p = await firstValueFrom(this.api.profile());
      if (seq !== this.seq) return;
      this.profile.setValue({
        nombre: p.nombre ?? '',
        apellido: p.apellido ?? '',
        fechaDeNacimiento: p.fechaNacimiento ?? '',
        nivel: p.nivel ?? '',
        objetivo: p.objetivo ?? '',
        emailContacto: p.emailContacto ?? '',
      });
      this.age.set(p.edad ?? null);
      this.email.controls.email.setValue(this.auth.session()?.email ?? '');
      this.loaded.set(true);
    } catch (e) {
      if (seq === this.seq) this.error.set(alumnoError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
  switchMode(mode: 'profile' | 'email' | 'password') {
    if (this.pending()) return;
    this.mode.set(mode);
    this.error.set('');
    this.success.set('');
    this.email.controls.passwordActual.reset();
    this.password.reset();
  }
  confirm() {
    const form =
      this.mode() === 'profile'
        ? this.profile
        : this.mode() === 'email'
          ? this.email
          : this.password;
    form.markAllAsTouched();
    this.error.set('');
    if (this.pending()) return;
    if (form.invalid) {
      this.error.set(
        'Revisá los campos obligatorios, formatos y límites. La nueva contraseña requiere al menos 8 caracteres, máximo 72 bytes UTF-8 y confirmación coincidente.',
      );
      return;
    }
    this.confirmation().open();
  }
  async save() {
    if (this.pending()) return;
    this.pending.set(true);
    this.error.set('');
    this.success.set('');
    try {
      if (this.mode() === 'profile') {
        const p = this.profile.getRawValue();
        await firstValueFrom(
          this.api.updateProfile({
            nombre: p.nombre.trim(),
            apellido: p.apellido.trim(),
            nivel: p.nivel as D.AlumnoRequest['nivel'],
            objetivo: p.objetivo as D.AlumnoRequest['objetivo'],
            ...(p.fechaDeNacimiento ? { fechaDeNacimiento: p.fechaDeNacimiento } : {}),
            emailContacto: p.emailContacto.trim(),
          }),
        );
        await this.load();
        if (!this.error()) this.success.set('Perfil actualizado correctamente.');
      } else {
        const response =
          this.mode() === 'email'
            ? await firstValueFrom(
                this.api.email({
                  email: this.email.controls.email.value.trim().toLowerCase(),
                  passwordActual: this.email.controls.passwordActual.value,
                }),
              )
            : await firstValueFrom(
                this.api.password({
                  passwordActual: this.password.controls.passwordActual.value,
                  nuevaPassword: this.password.controls.nuevaPassword.value,
                }),
              );
        const session = this.auth.replaceIdentity(response);
        this.email.controls.passwordActual.reset();
        this.password.reset();
        this.email.controls.email.setValue(session.email);
        this.success.set('Credenciales actualizadas. La sesión nueva reemplazó a la anterior.');
        if (session.rol !== 'ALUMNO') await this.router.navigateByUrl(this.auth.home());
      }
    } catch (e) {
      this.error.set(alumnoError(e));
    } finally {
      this.pending.set(false);
    }
  }
}
