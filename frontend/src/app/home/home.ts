import { Icon } from '../shared/icon';
import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../core/auth.service';
import { ROLE_CONFIG } from '../layout/role-config';
@Component({selector:'app-home', imports:[Icon], templateUrl:'./home.html'})
export class Home {
  readonly auth = inject(AuthService);
  readonly config = computed(() => ROLE_CONFIG[this.auth.session()?.rol ?? 'ALUMNO']);
}
