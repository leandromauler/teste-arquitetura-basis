import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private base = environment.apiBase;
  get(p:string){ return this.http.get(this.base+p); }
  post(p:string,b:any){ return this.http.post(this.base+p,b); }
}