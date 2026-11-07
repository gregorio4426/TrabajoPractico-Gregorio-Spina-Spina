import { Icon } from '../shared/icon';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { loginError } from '../core/api-error';

@Component({
  selector: 'app-login', imports: [ReactiveFormsModule, Icon], templateUrl: './login.html',
})
export class Login {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroy = inject(DestroyRef);
  readonly pending = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  submit(): void {
    if (this.pending()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.error.set('');
    this.pending.set(true);
    const credentials = this.form.getRawValue();
    this.auth.login({ email: credentials.email.trim().toLowerCase(), password: credentials.password }).pipe(
      takeUntilDestroyed(this.destroy), finalize(() => this.pending.set(false)),
    ).subscribe({
      next: () => { this.form.controls.password.reset(); void this.router.navigateByUrl(this.auth.home()); },
      error: (error: unknown) => this.error.set(loginError(error)),
    });
  }
}
