import { Component, OnInit } from '@angular/core';
import { GetSuppressionService } from '../../shared/services/api/rights/suppression/get-suppression/get-suppression.service';
import { PostSuppressionService } from '../../shared/services/api/rights/suppression/post-suppression/post-suppression.service';
import { GetDashboardService } from '../../shared/services/api/dashboard/get-dashboard/get-dashboard.service';
import { RequestData } from '../../interfaces/request-data';
import { CurrentValue } from '../../interfaces/current-value';
import { DataRequestAnswer } from '../../interfaces/data-request-answer';
import { CompletedRectificationSuppressionRequest } from '../../interfaces/completed-rectification-suppression-request';
import { SuccessErrorService } from '../../shared/services/success-error/success-error.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-suppression',
  templateUrl: './suppression.component.html',
  styleUrls: ['./suppression.component.css']
})
export class SuppressionComponent {
  suppressionRequest!: RequestData;
  suppressionRequestAnswer!: DataRequestAnswer;
  response: boolean = false;
  providerAnswer: boolean = false;
  providerClaim: string = '';
  currentValue: String = "";

  constructor(
    private getSuppressionService: GetSuppressionService,
    private postSuppressionService: PostSuppressionService,
    private getDashboardService: GetDashboardService,
    private successErrorService: SuccessErrorService,
    private _snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.getSelectedSuppressionRequest();
  }

  getSelectedSuppressionRequest() {
    this.getSuppressionService.getSelectedSuppressionRequest(
      this.getDashboardService.selectedRequest.dataRequestId
    ).subscribe(
      response => {
        this.suppressionRequest = response;
        this.successErrorService.handleSuccess('getSelectedSuppressionRequest', response);

        this.getSelectedSuppressionRequestAnswer();
        if(!this.response)
          this.getCurrentValue();
      },
      error => {
        this.successErrorService.handleError('getSelectedSuppressionRequest', error);
      }
    );
  }

  getSelectedSuppressionRequestAnswer() {
    this.getSuppressionService.getSelectedSuppressionRequestAnswer(this.getDashboardService.selectedRequest.dataRequestId).subscribe(
      response => {
        if(response != null) {
          this.response = true;
          this.suppressionRequestAnswer = response;
          this.successErrorService.handleSuccess('getSelectedSuppressionRequestAnswer', response);
        }
      },
      error => {
        this.successErrorService.handleError('getSelectedSuppressionRequestAnswer', error);
      }
    );
  }

  getCurrentValue() {
    this.getSuppressionService.getCurrentValue(this.suppressionRequest.dataSubject.idRef, this.suppressionRequest.dataTypeList[0].data[0].dataName, this.suppressionRequest.dataTypeList[0].data[0].primaryKeys).subscribe(
        response => {
        this.currentValue = response.value;
        this.successErrorService.handleSuccess('getCurrentValue', response);
      },
      error => {
        this.successErrorService.handleError('getCurrentValue', error);
      }
    );
  }

  postCompletedSuppressionRequest() {
    const completedSuppressionRequest: CompletedRectificationSuppressionRequest = {
      dataRequestId: this.suppressionRequest.dataRequestId,
      providerClaim: this.providerClaim,
      answer: this.providerAnswer,
    };

    this.postSuppressionService.postCompletedSuppressionRequest(completedSuppressionRequest).subscribe(
      response => {
        const message = 'Success!';
        const action = 'X';
        this._snackBar.open(message, action);
        console.log("[Success] postCompletedSuppressionRequest()", response);
      },
      error => {
        const message = 'Error..';
        const action = 'X';
        this._snackBar.open(message, action);
        console.log("[Error] postCompletedSuppressionRequest()", error);
      }
    );
  }
}
