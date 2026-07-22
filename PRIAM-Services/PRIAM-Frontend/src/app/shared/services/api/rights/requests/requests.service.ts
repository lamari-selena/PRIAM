import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environment/environment';
import { DataRequestAnswer, DataRequestResponseDTO } from 'src/app/interfaces/data-request';

@Injectable({
  providedIn: 'root',
})
export class RequestsService {
  constructor(private httpClient: HttpClient) {}

  private baseUrl = environment.api_right;

  getRequestsByDataSubject(dataSubjectId: number): Observable<DataRequestResponseDTO[]> {
    return this.httpClient.get<DataRequestResponseDTO[]>(
      `${this.baseUrl}/requestsRectification/${dataSubjectId}`
    );
  }

  getAnswer(dataRequestId: number): Observable<DataRequestAnswer> {
    return this.httpClient.get<DataRequestAnswer>(
      `${this.baseUrl}/right/answer/${dataRequestId}`
    );
  }
}
