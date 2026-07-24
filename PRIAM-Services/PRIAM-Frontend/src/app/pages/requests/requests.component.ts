import { Component, OnInit } from '@angular/core';
import { catchError, of } from 'rxjs';
import { RequestsService } from '../../shared/services/api/rights/requests/requests.service';
import { ActorService } from '../../shared/services/api/actor/actor.service';
import { SecurityService } from '../../shared/services/security.service';
import { DataRequestResponseDTO } from 'src/app/interfaces/data-request';

interface RequestRow extends DataRequestResponseDTO {
  answerStatus: string;
}

@Component({
  selector: 'app-requests',
  templateUrl: './requests.component.html',
  styleUrls: ['./requests.component.css'],
})
export class RequestsComponent implements OnInit {
  displayedColumns: string[] = ['requestType', 'dataRequestClaim', 'newValue', 'answerStatus'];
  rows: RequestRow[] = [];
  loading = true;

  constructor(
    private requestsService: RequestsService,
    private actorService: ActorService,
    private securityService: SecurityService
  ) {}

  ngOnInit() {
    this.refresh();
  }

  // Extracted so the data subject can re-fetch on demand: this page only ever
  // fetched once, in ngOnInit, so an app owner's approval made after the page
  // loaded (the common case — approving takes the owner some time) never
  // appeared without a full browser reload.
  refresh() {
    const idRef = this.securityService.getIdReference();
    if (idRef === null) {
      this.loading = false;
      return;
    }

    this.loading = true;

    // getRequestsByDataSubject wants PRIAM's own internal numeric id, not
    // the external idRef — resolve it first (see ActorService).
    this.actorService.getDataSubjectId(idRef).subscribe({
      next: (dataSubjectId) => {
        this.requestsService.getRequestsByDataSubject(dataSubjectId).subscribe({
          next: (requests) => {
            this.rows = requests.map((r) => ({ ...r, answerStatus: 'Pending' }));
            this.loading = false;
            this.rows.forEach((row) => {
              if (!row.response) {
                return;
              }
              this.requestsService
                .getAnswer(row.dataRequestId)
                .pipe(catchError(() => of(null)))
                .subscribe((answer) => {
                  row.answerStatus = answer ? answer.answer : 'Pending';
                });
            });
          },
          error: () => {
            this.loading = false;
          },
        });
      },
      error: () => {
        this.loading = false;
      },
    });
  }
}
