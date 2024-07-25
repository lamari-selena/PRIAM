import { Component } from '@angular/core';
import { GetAccessService } from '../../shared/services/api/rights/access/get-access/get-access.service';
import { PostRectificationService } from '../../shared/services/api/rights/rectification/post-rectification/post-rectification.service';
import { SuccessErrorService } from '../../shared/services/success-error/success-error.service';
import { PrimaryKey } from '../../interfaces/rectification';
import { NonPrimaryKey } from '../../interfaces/rectification';
import { Rectification } from '../../interfaces/rectification';
import { Data } from '../../interfaces/rectification';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-rectification',
  templateUrl: './rectification.component.html',
  styleUrls: ['./rectification.component.css']
})
export class RectificationComponent {
  constructor(
    private getAccessService: GetAccessService,
    private postRectificationService: PostRectificationService,
    private successErrorService: SuccessErrorService,
    private _snackBar: MatSnackBar,
  ) {}

  primaryKeys: PrimaryKey[] = this.getAccessService.primaryKeys;
  nonPrimaryKeys: NonPrimaryKey[] = this.getAccessService.nonPrimaryKeys;
  userClaim: string = '';
  newValue: any = null; // A MODIF POUR TOUT GERER
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

postRectification() {
  const rectification: Rectification = {
    dataSubjectId: 1,
    dataTypeName: this.getDataTypeName(this.selectedKey),
    data: {dataId: this.getDataId(this.selectedKey)},
    newValue: this.newValue,
    claim: this.userClaim,
    primaryKeys: this.getAccessService.primaryKeys,
  };

  this.postRectificationService.postRectification(rectification).subscribe(
    response => {
      const message = 'Success!';
      const action = 'X';
      this._snackBar.open(message, action);
      this.successErrorService.handleSuccess('postRectification', response);
    },
    error => {
      const message = 'Error..';
      const action = 'X';
      this._snackBar.open(message, action);
      this.successErrorService.handleError('postRectification', error);
    }
  );
}

}
