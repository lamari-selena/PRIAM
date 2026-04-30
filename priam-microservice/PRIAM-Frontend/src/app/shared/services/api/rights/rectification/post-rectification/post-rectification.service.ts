import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Rectification } from '../../../../../../interfaces/rectification';
import {environment} from "../../../../../../../environment/environment";

@Injectable({
  providedIn: 'root'
})
export class PostRectificationService {
  constructor(private httpClient: HttpClient) { }

  private baseUrl = environment.api_right;

  postRectification(rectification: Rectification): Observable<Rectification> {
    return this.httpClient.post<Rectification>(`${this.baseUrl}/right/rectificationRequest`, rectification);
  }
}
