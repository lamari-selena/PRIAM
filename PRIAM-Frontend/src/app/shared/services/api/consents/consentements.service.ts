import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from "../../../../../environment/environment";
import { Processing } from 'src/app/interfaces/processing';

@Injectable({
  providedIn: 'root'
})
export class ConsentService {
  constructor(private httpClient: HttpClient) {
  }

  private baseUrl = environment.api_data;
  private baseName = "processing"

  getProcessings(): Observable<Processing[]> {
    return this.httpClient.get<Processing[]>(`${this.baseUrl}/${this.baseName}/listProcessings`);
  }

}

