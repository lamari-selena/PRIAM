import {Component, OnInit} from '@angular/core';
import {GetAccessService} from '../../shared/services/api/rights/access/get-access/get-access.service';
import {PostAccessService} from '../../shared/services/api/rights/access/post-access/post-access.service';
import {GetDashboardService} from '../../shared/services/api/dashboard/get-dashboard/get-dashboard.service';
import {SlideToggleService} from '../../shared/services/slide-toggle/slide-toggle.service';
import {MatSlideToggleChange} from '@angular/material/slide-toggle';
import {RequestData} from '../../interfaces/request-data';
import {DataRequestAnswer} from '../../interfaces/data-request-answer';
import {CompletedAccessRequest} from '../../interfaces/completed-access-request';
import {SuccessErrorService} from '../../shared/services/success-error/success-error.service';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-access-request',
  templateUrl: './access-request.component.html',
  styleUrls: ['./access-request.component.css']
})
export class AccessRequestComponent implements OnInit {
  accessRequest!: RequestData;
  accessRequestAnswer!: DataRequestAnswer;
  response: boolean = false;
  providerDataClaims: string[] = ["","NON JE VEUX PAS >:(", "C'EST A MOI MTN!", "J'PARTAGE PAS!"];
  providerClaim: string = '';
  selectedProviderClaims: { [key: string]: string } = {};

  constructor(
    private getAccessService: GetAccessService,
    private postAccessService: PostAccessService,
    private getDashboardService: GetDashboardService,
    private slideToggleService: SlideToggleService,
    private successErrorService: SuccessErrorService,
    private _snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.getSelectedAccessRequest();
  }

  getSelectedAccessRequest() {
    this.getAccessService.getSelectedAccessRequest(
      this.getDashboardService.selectedRequest.dataRequestId
    ).subscribe(
      response => {
        this.accessRequest = response;
        this.successErrorService.handleSuccess('getSelectedAccessRequest', response);

        this.getSelectedAccessRequestAnswer();
      },
      error => {
        this.successErrorService.handleError('getSelectedAccessRequest', error);
      }
    );
  }

  getSelectedAccessRequestAnswer() {
    this.getAccessService.getSelectedAccessRequestAnswer(this.getDashboardService.selectedRequest.dataRequestId).subscribe(
      response => {
        if(response != null) {
          this.response = true;
          this.accessRequestAnswer = response;
          this.successErrorService.handleSuccess('getSelectedAccessRequestAnswer', response);
        }
      },
      error => {
        this.successErrorService.handleError('getSelectedAccessRequestAnswer', error);
      }
    );
  }

  onChange($event: MatSlideToggleChange, toggleName: string, dataType: any, data: any) {
    this.slideToggleService.onChange(this.accessRequest.dataTypeList, $event, toggleName, dataType, data);
    console.log("onChange(", $event.checked, ",", toggleName, ",", dataType.dataTypeName, ",", data.attributeName, "): ", this.accessRequest.dataTypeList);
  }


  isConfirmButtonDisabled(): boolean {
    if (this.response) {
      return true;
    }

    if (!this.providerClaim.trim()) {
      return true;
    }

    return this.accessRequest.dataTypeList
        .flatMap(dataType => dataType.data.filter(data => !data.answerByData))
        .some(unselectedData => !this.selectedProviderClaims[unselectedData.dataId]?.trim());
  }

  isDataUnselected(dataId: string): boolean {
    return this.accessRequest.dataTypeList.some(dataType =>
      dataType.data.some(data => data.dataId.toString() === dataId && !data.answerByData)
    );
  }

  getDataNameById(dataId: string): string {
    const data = this.accessRequest.dataTypeList
      .flatMap(dataType => dataType.data)
      .find(data => data.dataId.toString() === dataId);

    return data ? data.dataName : '';
  }

postCompletedAccessRequest() {
  const completedAccessRequest: CompletedAccessRequest = {
    dataRequestId: this.accessRequest.dataRequestId,
    data: this.accessRequest.dataTypeList
      .flatMap(dataType => dataType.data.filter(data => data.answerByData)),
    providerClaim: `${this.providerClaim}<br>${Object.keys(this.selectedProviderClaims)
      .filter(dataId => this.selectedProviderClaims[dataId] != null && this.isDataUnselected(dataId))
      .map(dataId => `- ${this.getDataNameById(dataId)}: ${this.selectedProviderClaims[dataId]}`)
      .join('<br>')}`,
  };

  this.postAccessService.postCompletedAccessRequest(completedAccessRequest).subscribe(
    response => {
      // Show a positive snackbar message upon success
      const message = 'Success!';
      const action = 'X';
      this._snackBar.open(message, action);
      console.log("[Success] postCompletedAccessRequest()", response);
    },
    error => {
      // Show a negative snackbar message upon error
      const message = 'Error..';
      const action = 'X';
      this._snackBar.open(message, action);
      console.log("[Error] postCompletedAccessRequest()", error);
    }
  );
 }
}
