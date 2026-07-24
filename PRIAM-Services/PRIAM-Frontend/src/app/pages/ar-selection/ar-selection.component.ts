import { Component, OnInit } from '@angular/core';
import { GetAccessService } from '../../shared/services/api/rights/access/get-access/get-access.service';
import { PostAccessService } from '../../shared/services/api/rights/access/post-access/post-access.service';
import { ActorService } from '../../shared/services/api/actor/actor.service';
import { SlideToggleService } from '../../shared/services/slide-toggle/slide-toggle.service';
import { SuccessErrorService } from '../../shared/services/success-error/success-error.service';
import { SecurityService } from '../../shared/services/security.service';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { IndirectGeneratedDataList } from '../../interfaces/indirect-generated-data-list';
import { AccessRequest } from '../../interfaces/access-request';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-ar-selection',
  templateUrl: './ar-selection.component.html',
  styleUrls: ['./ar-selection.component.css'],
})

export class ArSelectionComponent implements OnInit {
  constructor(
    private getAccessService: GetAccessService,
    private postAccessService: PostAccessService,
    private actorService: ActorService,
    private slideToggleService: SlideToggleService,
    private successErrorService: SuccessErrorService,
    private securityService: SecurityService,
    private _snackBar: MatSnackBar,
  ) {}

  referenceId: string | null = this.securityService.getIdReference();
  selectAll: boolean = false;
  indirectGeneratedDataList: IndirectGeneratedDataList[] = [];
  dataRequestClaim: string = '';

  ngOnInit() {
    this.getIndirectAndGeneratedDataList();
  }

  getIndirectAndGeneratedDataList() {
    if (this.referenceId == null) return;
    this.getAccessService.getIndirectAndGeneratedDataList(this.referenceId).subscribe(
      response => {
        this.indirectGeneratedDataList = response;
        this.successErrorService.handleSuccess('getIndirectAndGeneratedDataList', response);
      },
      error => {
        this.successErrorService.handleError('getIndirectAndGeneratedDataList', error);
      }
    );
  }

  isAtLeastOneSelected() {
    return this.slideToggleService.isAtLeastOneSelected(this.indirectGeneratedDataList);
  }

  onChange($event: MatSlideToggleChange, toggleName: string, dataType: any, data: any) {
    this.selectAll = this.slideToggleService.onChange(this.indirectGeneratedDataList, $event, toggleName, dataType, data);
    console.log(data)
    console.log("onChange(", $event.checked, ",", toggleName, ",", dataType.dataTypeName, ",", data.attributeName, "): ", this.indirectGeneratedDataList);
  }

  postAccessRequest() {
    if (this.referenceId == null) return;
    const data = this.indirectGeneratedDataList
      .flatMap(dataType => dataType.data.filter(data => data.selected));

    // AccessRequestRequestDTO.dataSubjectId is PRIAM's own internal numeric
    // id, not the external idRef this.referenceId holds — resolve it first
    // (see ActorService).
    this.actorService.getDataSubjectId(this.referenceId).subscribe(
      dataSubjectId => {
        const accessRequest: AccessRequest = {
          dataSubjectId,
          data,
          dataRequestClaim: this.dataRequestClaim,
        };

        this.postAccessService.postAccessRequest(accessRequest).subscribe(
          response => {
            const message = 'Success!';
            const action = 'X';
            this._snackBar.open(message, action);
            this.successErrorService.handleSuccess('postAccessRequest', response);
          },
          error => {
            const message = 'Error..';
            const action = 'X';
            this._snackBar.open(message, action);
            this.successErrorService.handleError('postAccessRequest', error);
          }
        );
      },
      error => {
        const message = 'Error..';
        const action = 'X';
        this._snackBar.open(message, action);
        this.successErrorService.handleError('getDataSubjectId', error);
      }
    );
  }
}
