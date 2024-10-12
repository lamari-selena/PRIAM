import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { DataSubjectCategory } from '../../../../../interfaces/data-subject-category';
import { RequestFilter } from '../../../../../interfaces/request-filter';
import { Request } from '../../../../../interfaces/request';
import { SelectedRequest } from '../../../../../interfaces/selected-request';
import {environment} from "../../../../../../environment/environment";

@Injectable({
  providedIn: 'root'
})
export class GetDashboardService {
  constructor(private httpClient: HttpClient) { }

  private baseUrl = environment.keycloak || 'http://localhost:8080';
  selectedRequest!: SelectedRequest;

  getDataSubjectCategory(): Observable<DataSubjectCategory[]> {
    return this.httpClient.get<DataSubjectCategory[]>(`${environment.api_actor}/actor/DataSubjectCategories`);
  }

  getFilteredRequests(requestFilter: RequestFilter): Observable<Request[]> {
    const params = new HttpParams({fromObject: {
        listOfSelectedTypeDataRequests: requestFilter.requestTypes,
        listOfSelectedStatus: requestFilter.requestResponses,
        listOfSelectedDataSubjectCategories: requestFilter.dataSubjectCategories.map(d => d.dataSubjectCategoryName)
      }});
    return this.httpClient.get<Request[]>(`${environment.api_right}/right/requestList`, {params: params});
  }
}
