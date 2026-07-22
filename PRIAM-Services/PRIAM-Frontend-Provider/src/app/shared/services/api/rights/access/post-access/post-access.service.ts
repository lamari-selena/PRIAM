import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompletedAccessRequest } from '../../../../../../interfaces/completed-access-request';
import {environment} from "../../../../../../../environment/environment";

@Injectable({
  providedIn: 'root'
})
export class PostAccessService {
  constructor(private httpClient: HttpClient) { }

  private baseUrl = environment.api_right;

  postCompletedAccessRequest(completedAccessRequest: CompletedAccessRequest): Observable<CompletedAccessRequest> {
    return this.httpClient.post<CompletedAccessRequest>(`${this.baseUrl}/right/answer`, completedAccessRequest);
  }
}
