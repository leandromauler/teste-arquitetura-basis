import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
@Component({
  standalone: true, selector: 'app-login', imports: [FormsModule],
  template: `<h2>Login (demo)</h2>
    <form (ngSubmit)="login()">
      <label>Usuário</label><input [(ngModel)]="user" name="user" required/>
      <label>Senha</label><input [(ngModel)]="pass" name="pass" type="password" required/>
      <button type="submit">Entrar</button>
    </form>`
}) export class LoginComponent {
  user=''; pass=''; constructor(private router: Router) {}
  login(){ if(this.user && this.pass){ localStorage.setItem('token','demo'); this.router.navigateByUrl('/enviar-fhir'); } }
}