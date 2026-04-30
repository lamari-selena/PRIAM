import { Component } from '@angular/core';
import { GetAccessService } from '../../shared/services/api/rights/access/get-access/get-access.service';
import { PostSuppressionService } from '../../shared/services/api/rights/suppression/post-suppression/post-suppression.service';
import { SuccessErrorService } from '../../shared/services/success-error/success-error.service';
import { PrimaryKey } from '../../interfaces/suppression';
import { NonPrimaryKey } from '../../interfaces/suppression';
import { Suppression } from '../../interfaces/suppression';
import { Data } from '../../interfaces/rectification';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-suppression',
  templateUrl: './suppression.component.html',
  styleUrls: ['./suppression.component.css']
})
export class SuppressionComponent {
  constructor(
    private getAccessService: GetAccessService,
    private postSuppressionService: PostSuppressionService,
    private successErrorService: SuccessErrorService,
    private _snackBar: MatSnackBar,
  ) {}

  primaryKeys: PrimaryKey[] = this.getAccessService.primaryKeys;
  nonPrimaryKeys: NonPrimaryKey[] = this.getAccessService.nonPrimaryKeys;
  userClaim: string = '';
  selectedKey: string = '';

  ngOnInit() {
    console.log(this.primaryKeys);
    console.log(this.nonPrimaryKeys);
  }

  isPrimaryKey(selectedKey: any): boolean {
    return this.primaryKeys.some(pk => pk.primaryKeyName === selectedKey);
  }

  getDataValue(selectedKey: string): any {
    const selectedNonPrimaryKey = this.nonPrimaryKeys.find(npk => npk.dataValue === selectedKey);
    return selectedNonPrimaryKey?.dataValue ?? '';
  }

  getDataId(selectedKey: string): number {
    const selectedNonPrimaryKey = this.nonPrimaryKeys.find(npk => npk.dataValue === selectedKey);
    return selectedNonPrimaryKey?.dataId ?? 0;
  }

  getDataTypeName(selectedKey: string): string {
    const selectedNonPrimaryKey = this.nonPrimaryKeys.find(npk => npk.dataValue === selectedKey);
    return selectedNonPrimaryKey?.dataType ?? '';
  }

  postSuppression() {
    const suppression: Suppression = {
      dataSubjectId: 1,
      dataTypeName: this.getDataTypeName(this.selectedKey),
      data: {dataId: this.getDataId(this.selectedKey)},
      claim: this.userClaim,
      primaryKeys: this.getAccessService.primaryKeys,
    };

    console.log(suppression);

    this.postSuppressionService.postSuppression(suppression).subscribe(
      response => {
        const message = 'Success!';
        const action = 'X';
        this._snackBar.open(message, action);
        this.successErrorService.handleSuccess('postSuppression', response);
      },
      error => {
        const message = 'Error..';
        const action = 'X';
        this._snackBar.open(message, action);
        this.successErrorService.handleError('postSuppression', error);
      }
    );
  }
}
