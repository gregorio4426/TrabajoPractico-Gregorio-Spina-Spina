import { Icon } from '../shared/icon';
import { Component, DestroyRef, ElementRef, afterRenderEffect, computed, inject, signal, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { ROLE_CONFIG } from './role-config';
@Component({ selector: 'app-shell', imports: [RouterLink, RouterLinkActive, RouterOutlet, Icon], templateUrl: './shell.html' })
export class Shell {
  readonly auth = inject(AuthService);
  private readonly media = window.matchMedia('(max-width: 760px)');
  readonly mobile = signal(this.media.matches);
  readonly menuOpen = signal(false);
  onMenuKeydown(event: KeyboardEvent): void {
    if (!this.mobile() || !this.menuOpen()) return;
    if (event.key === 'Escape') { event.preventDefault(); this.closeMenu(); return; }
    if (event.key !== 'Tab') return;
    const controls = this.sidebar().nativeElement.querySelectorAll<HTMLElement>('a, button');
    const first = controls.item(0); const last = controls.item(controls.length - 1);
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
  }
  private readonly sidebar = viewChild.required<ElementRef<HTMLElement>>('sidebar');
  private readonly menuButton = viewChild.required<ElementRef<HTMLButtonElement>>('menuButton');
  openMenu(): void { this.menuOpen.set(true); }
  closeMenu(): void {
    this.menuOpen.set(false);
    setTimeout(() => this.menuButton().nativeElement.focus());
  }
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('logoutDialog');
  openLogout(): void { this.dialog().nativeElement.showModal(); }
  cancelLogout(): void { this.dialog().nativeElement.close(); }
  readonly config = computed(() => ROLE_CONFIG[this.auth.session()?.rol ?? 'ALUMNO']);
  constructor() {
    const onResize = (event: MediaQueryListEvent) => { this.mobile.set(event.matches); this.menuOpen.set(false); };
    this.media.addEventListener('change', onResize);
    inject(DestroyRef).onDestroy(() => this.media.removeEventListener('change', onResize));
    afterRenderEffect(() => {
      if (this.menuOpen()) this.sidebar().nativeElement.querySelector<HTMLButtonElement>('.sidebar-close')?.focus();
    });
    inject(Router).events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed()).subscribe(() => this.menuOpen.set(false));
  }
  logout(): void { this.cancelLogout(); this.auth.logout(); }
}
