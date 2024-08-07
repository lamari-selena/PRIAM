import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from "../../../../../environment/environment";
import { Processing } from 'src/app/interfaces/processing';
import { Consent } from 'src/app/interfaces/consent';

@Injectable({
  providedIn: 'root'
})
export class ConsentService {
  constructor(private httpClient: HttpClient) {
  }

  private baseUrlData = environment.api_data;
  private baseUrlConsent = environment.api_const;
  private baseNameData = "processing"
  private baseNameContract = "contract"
  private baseNameConsent= "consent"

  getProcessings(): Observable<Processing[]> {
    return this.httpClient.get<Processing[]>(`${this.baseUrlData}/${this.baseNameData}/listProcessings`);
  }

  getConsents(idRef: number, idP: number): Observable<Consent[]> {
    return this.httpClient.get<Consent[]>(`${this.baseUrlConsent}/${this.baseNameContract}/list/consents/${idRef}/${idP}`);
  }
  
  postConsent(idRef: number, processingId: number): Observable<Consent[]> {
    return this.httpClient.post<Consent[]>(`${this.baseUrlConsent}/${this.baseNameConsent}/create/${idRef}`, { processingId });
  }
}

