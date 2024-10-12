import { Component, OnInit } from '@angular/core';
import { GetAccessService } from '../../shared/services/api/rights/access/get-access/get-access.service';
import { PostAccessService } from '../../shared/services/api/rights/access/post-access/post-access.service';
import { SlideToggleService } from '../../shared/services/slide-toggle/slide-toggle.service';
import { SuccessErrorService } from '../../shared/services/success-error/success-error.service';
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
    private slideToggleService: SlideToggleService,
    private successErrorService: SuccessErrorService,
    private _snackBar: MatSnackBar,
  ) {}

  referenceId: number = 507;
  selectAll: boolean = false;
  indirectGeneratedDataList: IndirectGeneratedDataList[] = [];
  dataRequestClaim: string = '';

  ngOnInit() {
    this.getIndirectAndGeneratedDataList();
  }

  getIndirectAndGeneratedDataList() {
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
    const accessRequest: AccessRequest = {
      dataSubjectId: 1,
      data: this.indirectGeneratedDataList
        .flatMap(dataType => dataType.data.filter(data => data.selected)),
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
  }
}
