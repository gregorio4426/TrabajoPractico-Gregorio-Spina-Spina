import { Component, ElementRef, input, output, viewChild } from '@angular/core';
let nextConfirmation = 0;
@Component({
  selector: 'app-teacher-confirm',
  template: `<dialog #dialog class="dialog admin-confirm" [attr.aria-labelledby]="headingId">
    <h2 [id]="headingId">{{ title() }}</h2>
    <p>{{ message() }}</p>
    <div class="dialog-actions">
      <button class="secondary" (click)="close()">Cancelar</button
      ><button class="primary" (click)="confirm()">Confirmar cambio</button>
    </div>
  </dialog>`,
})
export class Confirm {
  readonly headingId = 'teacher-confirm-heading-' + ++nextConfirmation;
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
