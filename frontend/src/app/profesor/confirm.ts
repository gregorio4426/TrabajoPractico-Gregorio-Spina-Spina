import { Component, ElementRef, input, output, viewChild } from '@angular/core';
@Component({
  selector: 'app-teacher-confirm',
  template: `<dialog #dialog class="dialog admin-confirm" aria-labelledby="teacher-confirm-heading">
    <h2 id="teacher-confirm-heading">{{ title() }}</h2>
    <p>{{ message() }}</p>
    <div class="dialog-actions">
      <button class="secondary" (click)="close()">Cancelar</button
      ><button class="primary" (click)="confirm()">Confirmar cambio</button>
    </div>
  </dialog>`,
})
export class Confirm {
  readonly title = input('¿Confirmar la operación?');
  readonly message = input('');
  readonly confirmed = output<void>();
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  open() {
    this.dialog().nativeElement.showModal();
  }
  close() {
    this.dialog().nativeElement.close();
  }
  confirm() {
    this.close();
    this.confirmed.emit();
  }
}
