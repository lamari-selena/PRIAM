import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { GetRectificationService } from '../../shared/services/api/rights/rectification/get-rectification/get-rectification.service';
import { PostRectificationService } from '../../shared/services/api/rights/rectification/post-rectification/post-rectification.service';
import { GetDashboardService } from '../../shared/services/api/dashboard/get-dashboard/get-dashboard.service';
import { RequestData } from '../../interfaces/request-data';
import { DataRequestAnswer } from '../../interfaces/data-request-answer';
import { CurrentValue } from '../../interfaces/current-value';
import { CompletedRectificationSuppressionRequest } from '../../interfaces/completed-rectification-suppression-request';
import { SuccessErrorService } from '../../shared/services/success-error/success-error.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-rectification',
  templateUrl: './rectification.component.html',
  styleUrls: ['./rectification.component.css']
})
export class RectificationComponent {
  rectificationRequest!: RequestData;
  rectificationRequestAnswer!: DataRequestAnswer;
  response: boolean = false;
  providerAnswer: boolean = false;
  providerClaim: string = '';
  currentValue: String = "";
  submitting: boolean = false;

  constructor(
    private getRectificationService: GetRectificationService,
    private postRectificationService: PostRectificationService,
    private getDashboardService: GetDashboardService,
    private successErrorService: SuccessErrorService,
    private _snackBar: MatSnackBar,
    private router: Router,
  ) {}

  ngOnInit() {
    this.getSelectedRectificationRequest();
  }

  getSelectedRectificationRequest() {
    this.getRectificationService.getSelectedRectificationRequest(
      this.getDashboardService.selectedRequest.dataRequestId
    ).subscribe(
      response => {
        this.rectificationRequest = response;
        this.successErrorService.handleSuccess('getSelectedRectificationRequest', response);

        this.getSelectedRectificationRequestAnswer();
        if(!this.response)
          this.getCurrentValue();
      },
      error => {
        this.successErrorService.handleError('getSelectedRectificationRequest', error);
      }
    );
  }

  getSelectedRectificationRequestAnswer() {
    this.getRectificationService.getSelectedRectificationRequestAnswer(this.getDashboardService.selectedRequest.dataRequestId).subscribe(
      response => {
        if(response != null) {
          this.response = true;
          this.rectificationRequestAnswer = response;
          this.successErrorService.handleSuccess('getSelectedRectificationRequestAnswer', response);
        }
      },
      error => {
        this.successErrorService.handleError('getSelectedRectificationRequestAnswer', error);
      }
    );
  }

  getCurrentValue() {
    this.getRectificationService.getCurrentValue(this.rectificationRequest.dataSubject.idRef, this.rectificationRequest.dataTypeList[0].data[0].dataName, this.rectificationRequest.dataTypeList[0].data[0].primaryKeys).subscribe(
      response => {
        this.currentValue = response.value;
        this.successErrorService.handleSuccess('getCurrentValue', response);
      },
      error => {
        this.successErrorService.handleError('getCurrentValue', error);
      }
    );
  }

  postCompletedRectificationRequest() {
    // Guard against double-submit (the backend has no clean handling for
    // answering the same request twice — it 500s) and against navigating
    // away before the request completes: the submit button used to combine
    // a (click) handler with routerLink="/dashboard" on the same element,
    // so clicking it navigated away immediately, racing the async POST and
    // hiding whether it actually succeeded.
    if (this.submitting) return;
    this.submitting = true;

    const completedRectificationRequest: CompletedRectificationSuppressionRequest = {
      dataRequestId: this.rectificationRequest.dataRequestId,
      providerClaim: this.providerClaim,
      answer: this.providerAnswer,
    };

    this.postRectificationService.postCompletedRectificationRequest(completedRectificationRequest).subscribe(
      response => {
        const message = 'Success!';
        const action = 'X';
        this._snackBar.open(message, action);
        console.log("[Success] postCompletedRectificationRequest()", response);
        this.router.navigate(['/dashboard']);
      },
      error => {
        this.submitting = false;
        const message = 'Error..';
        const action = 'X';
        this._snackBar.open(message, action);
        console.log("[Error] postCompletedRectificationRequest()", error);
      }
    );
  }
}
