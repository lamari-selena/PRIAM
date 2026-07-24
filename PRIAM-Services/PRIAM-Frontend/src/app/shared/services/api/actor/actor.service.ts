import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environment/environment';

// Resolves an external idRef (String, what SecurityService.getIdReference()
// returns and what Data-service/Consent-service take) to PRIAM's own
// internal numeric data_subject_id (what PRIAM-Right-service's request
// DTOs — AccessRequestRequestDTO.dataSubjectId, requestsRectification/{id}
// — actually expect). These are two different identifiers; passing idRef
// directly where a numeric dataSubjectId is expected only "worked" for
// case studies whose idRef happened to be numeric and to coincide with the
// internal id.
@Injectable({ providedIn: 'root' })
export class ActorService {
  constructor(private httpClient: HttpClient) {}

  private baseUrl = environment.api_actor;

  getDataSubjectId(idRef: string): Observable<number> {
    return this.httpClient.get<number>(`${this.baseUrl}/api/DataSubjectId/${idRef}`);
  }
}
