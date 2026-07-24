import { Component } from '@angular/core';
import { GetAccessService } from '../../shared/services/api/rights/access/get-access/get-access.service';
import { PostSuppressionService } from '../../shared/services/api/rights/suppression/post-suppression/post-suppression.service';
import { ActorService } from '../../shared/services/api/actor/actor.service';
import { SecurityService } from '../../shared/services/security.service';
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
    private actorService: ActorService,
    private securityService: SecurityService,
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
    const idRef = this.securityService.getIdReference();
    if (idRef === null) return;

    // Suppression.dataSubjectId is PRIAM's own internal numeric id, not the
    // external idRef - resolve it first (see ActorService), same pattern as
    // ArSelectionComponent.postAccessRequest(). Was previously hardcoded to
    // 1, silently targeting whichever subject happens to have id 1
    // regardless of who is actually logged in (playbook §8.8.a, same bug
    // family).
    this.actorService.getDataSubjectId(idRef).subscribe(
      dataSubjectId => {
        const suppression: Suppression = {
          dataSubjectId,
          dataTypeName: this.getDataTypeName(this.selectedKey),
          data: {dataId: this.getDataId(this.selectedKey)},
          claim: this.userClaim,
          primaryKeys: this.getAccessService.primaryKeys,
        };

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
