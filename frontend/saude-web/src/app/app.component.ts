import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
@Component({
  selector: 'app-root', standalone: true, imports: [RouterLink, RouterOutlet],
  template: `<div class="container"><h1>Saúde Web</h1><nav>
    <a routerLink="/login">Login</a> | <a routerLink="/enviar-fhir">Enviar FHIR</a>
  </nav><router-outlet></router-outlet></div>`
}) export class AppComponent {}
