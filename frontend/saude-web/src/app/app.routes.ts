import { Routes } from '@angular/router';
export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./pages/login.component').then(m => m.LoginComponent) },
  { path: 'enviar-fhir', loadComponent: () => import('./pages/enviar-fhir.component').then(m => m.EnviarFhirComponent) },
];