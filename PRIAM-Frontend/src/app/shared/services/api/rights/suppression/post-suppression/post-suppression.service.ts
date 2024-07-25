import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Suppression } from '../../../../../../interfaces/suppression';
import {environment} from "../../../../../../../environment/environment";

@Injectable({
  providedIn: 'root'
})
export class PostSuppressionService {
  constructor(private httpClient: HttpClient) { }

  private baseUrl = environment.api_right;

  postSuppression(suppression: Suppression): Observable<Suppression> {
    return this.httpClient.post<Suppression>(`${this.baseUrl}/right/erasureRequest`, suppression);
  }
}
