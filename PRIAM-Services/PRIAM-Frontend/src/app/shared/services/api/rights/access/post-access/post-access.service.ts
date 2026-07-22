import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AccessRequest} from '../../../../../../interfaces/access-request';
import {environment} from "../../../../../../../environment/environment";

@Injectable({
  providedIn: 'root'
})
export class PostAccessService {
  constructor(private httpClient: HttpClient) {
  }

  private baseUrl = environment.api_right;

  postAccessRequest(accessRequest: AccessRequest): Observable<AccessRequest> {
    return this.httpClient.post<AccessRequest>(`${this.baseUrl}/right/accessRequest`, accessRequest);
  }
}
