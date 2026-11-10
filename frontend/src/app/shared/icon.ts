import { Component, input } from '@angular/core';
export type IconName = 'arrow' | 'grid' | 'bolt' | 'person' | 'shield' | 'menu';
@Component({
  selector: 'app-icon',
  template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    @switch (name()) {
      @case ('arrow') { <path d="M5 19 19 5M5 5h14v14"/> }
      @case ('grid') { <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/> }
      @case ('bolt') { <path d="m13 2-9 12h7l-1 8 10-12h-8l1-8Z"/> }
      @case ('person') { <circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/> }
      @case ('shield') { <path d="m12 2 8 4v6c0 5-8 10-8 10S4 17 4 12V6l8-4Z"/><path d="m8 12 3 3 5-6"/> }
      @case ('menu') { <path d="M4 6h16M4 12h16M4 18h16"/> }
    }
  </svg>`,
})
export class Icon { readonly name = input<IconName>('arrow'); }
