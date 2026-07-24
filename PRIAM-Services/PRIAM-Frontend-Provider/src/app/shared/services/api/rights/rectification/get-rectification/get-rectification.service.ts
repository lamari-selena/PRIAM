import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {DataValue, RequestData} from '../../../../../../interfaces/request-data';
import { DataRequestAnswer } from '../../../../../../interfaces/data-request-answer';
import { CurrentValue } from '../../../../../../interfaces/current-value';
import {environment} from "../../../../../../../environment/environment";

@Injectable({
  providedIn: 'root'
})
export class GetRectificationService {

  constructor(private httpClient: HttpClient) { }

  private baseUrlRight = environment.api_right;
  private baseUrlProvider = environment.api_provider;

  getSelectedRectificationRequest(dataRequestId: number): Observable<RequestData> {
    return this.httpClient.get<RequestData>(`${this.baseUrlRight}/right/requestDetail/${dataRequestId}`);
  }

  getSelectedRectificationRequestAnswer(dataRequestId: number): Observable<DataRequestAnswer> {
    return this.httpClient.get<DataRequestAnswer>(`${this.baseUrlRight}/right/answer/${dataRequestId}`);
  }

  getCurrentValue(idRef: string, dataName: string, primaryKeys: Map<String, String>): Observable<DataValue> {
    let data = {
      idRef: idRef,
      dataName: dataName,
      primaryKeys: primaryKeys
    }
    return this.httpClient.post<DataValue>(`${this.baseUrlProvider}/api/dataValue`, data);
  }
}
