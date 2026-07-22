import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RequestData } from '../../../../../../interfaces/request-data';
import { DataRequestAnswer } from '../../../../../../interfaces/data-request-answer';
import {environment} from "../../../../../../../environment/environment";

@Injectable({
  providedIn: 'root'
})

export class GetAccessService {

  constructor(private httpClient: HttpClient) { }

  private baseUrl = environment.api_right;

  getSelectedAccessRequest(dataRequestId: number): Observable<RequestData> {
    return this.httpClient.get<RequestData>(`${this.baseUrl}/right/requestDetail/${dataRequestId}`);
  }

  getSelectedAccessRequestAnswer(dataRequestId: number): Observable<DataRequestAnswer> {
    return this.httpClient.get<DataRequestAnswer>(`${this.baseUrl}/right/answer/${dataRequestId}`);
  }
}
