import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { NgIf } from '@angular/common';
@Component({
  standalone: true, selector: 'app-enviar-fhir', imports:[FormsModule, NgIf],
  template: `<h2>Enviar FHIR - Immunization</h2>
    <form (ngSubmit)="submit()">
      <label>Nome</label><input [(ngModel)]="form.name" name="name" required/>
      <label>Nascimento</label><input [(ngModel)]="form.birthDate" name="birthDate" type="date" required/>
      <label>Código</label><input [(ngModel)]="form.code" name="code" placeholder="207"/>
      <label>Lote</label><input [(ngModel)]="form.lotNumber" name="lotNumber"/>
      <button type="submit">Enviar</button>
    </form>
    <div *ngIf="resp"><pre>{{resp|json}}</pre></div>`
}) export class EnviarFhirComponent {
  form:any={{name:'',birthDate:'',code:'207',lotNumber:''}}; resp:any;
  constructor(private api: ApiService){}
  submit(){ const f=this.form; const imm={{resourceType:'Immunization',status:'completed',vaccineCode:{{coding:[{{system:'http://hl7.org/fhir/sid/cvx',code:f.code}}]}},occurrenceDateTime:f.birthDate,lotNumber:f.lotNumber||undefined,patient:{{display:f.name}} }}; this.api.post('/immunizations',imm).subscribe(r=>this.resp=r); }
}