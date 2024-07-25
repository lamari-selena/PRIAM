import {Component, OnInit, Inject} from '@angular/core';
import {GetAccessService} from '../../shared/services/api/rights/access/get-access/get-access.service';
import {SuccessErrorService} from '../../shared/services/success-error/success-error.service';
import {DataType} from '../../interfaces/data-list';
import {Processing} from '../../interfaces/data-list-purpose';
import {SecondaryActor} from '../../interfaces/data-list-transfer';
import {PrimaryKey} from '../../interfaces/rectification';
import {MatSnackBar} from '@angular/material/snack-bar';
import {
  MatDialogModule,
  MatDialog,
  MAT_DIALOG_DATA,
  MatDialogTitle,
  MatDialogContent,
} from '@angular/material/dialog';
import {CommonModule} from '@angular/common';
import {SecurityService} from "../../shared/services/security.service";

@Component({
  selector: 'app-access-request',
  templateUrl: './access-request.component.html',
  styleUrls: ['./access-request.component.css']
})

export class AccessRequestComponent implements OnInit {
  constructor(
    private getAccessService: GetAccessService,
    private successErrorService: SuccessErrorService,
    private _snackBar: MatSnackBar,
    public dialog: MatDialog,
    public securityService: SecurityService,
  ) {
  }


  referenceId: number | null = this.securityService.getIdReference()
  dataList: DataType[] = [];
  dataListByPurpose: Processing[] = [];
  dataListTransfer: SecondaryActor[] = [];
  euCountries = ['Austria', 'Belgium', 'Bulgaria', 'Croatia', 'Cyprus', 'Czech Republic', 'Denmark', 'Estonia', 'Finland', 'France', 'Germany', 'Greece', 'Hungary', 'Ireland', 'Italy', 'Latvia', 'Lithuania', 'Luxembourg', 'Malta', 'Netherlands', 'Poland', 'Portugal', 'Romania', 'Slovakia', 'Slovenia', 'Spain', 'Sweden'];
  euActors: SecondaryActor[] = this.dataListTransfer.filter(actor => this.euCountries.includes(actor.country.countryName));
  nonEuActors: SecondaryActor[] = this.dataListTransfer.filter(actor => !this.euCountries.includes(actor.country.countryName));

  ngOnInit() {
    this.getPersonalDataList();
    this.getPersonalDataListByPurpose();
    this.getPersonalDataListTransfer();
  }

  getPersonalDataList() {
    if (this.referenceId != null)
    this.getAccessService.getPersonalDataList(this.referenceId).subscribe(
      response => {
        this.dataList = response;
        this.successErrorService.handleSuccess('getPersonalDataList', response);
      },
      error => {
        this.successErrorService.handleError('getPersonalDataList', error);
      }
    );
  }

  getPersonalDataListByPurpose() {
    if (this.referenceId != null)
    this.getAccessService.getPersonalDataListByPurpose(this.referenceId).subscribe(
      response => {
        this.dataListByPurpose = response;
        this.successErrorService.handleSuccess('getPersonalDataListByPurpose', response);
      },
      error => {
        this.successErrorService.handleError('getPersonalDataListByPurpose', error);
      }
    );
  }

  getPersonalDataListTransfer() {
    if (this.referenceId != null)
    this.getAccessService.getPersonalDataListTransfer(this.referenceId).subscribe(
      response => {
        this.dataListTransfer = response;
        this.euActors = this.dataListTransfer.filter(actor => this.euCountries.includes(actor.country.countryName));
        this.nonEuActors = this.dataListTransfer.filter(actor => !this.euCountries.includes(actor.country.countryName));
        this.successErrorService.handleSuccess('getPersonalDataListTransfer', response);
      },
      error => {
        this.successErrorService.handleError('getPersonalDataListTransfer', error);
      }
    );
  }

  openDialogAdditionalData(dataItem: any) {
    this.dialog.open(DialogAdditionalData, {
      data: {
        selectedData: dataItem,
      },
    });
  }

  hasActorOutsideEu(dataList: SecondaryActor[]): boolean {
    return dataList.some(actor => !this.euCountries.includes(actor.country.countryName));
  }

  getPrimaryKeys(dataType: any, rowIndex: number) {
    this.getAccessService.primaryKeys = dataType.data
      .filter((data: any) => data.isPrimaryKey)
      .map((data: any) => ({
          primaryKeyValue: data.dataValue[rowIndex],
          primaryKeyName: data.dataName,
          primaryKeyId: data.dataId
        }
      ));
  }

  getNonPrimaryKeysData(dataType: any, rowIndex: number) {
    this.getAccessService.nonPrimaryKeys = dataType.data
      .filter((data: any) => !data.isPrimaryKey)
      .map((data: any) => ({
        dataValue: data.dataValue[rowIndex],
        dataName: data.dataName,
        dataId: data.dataId,
        dataType: dataType.dataTypeName
      }));
  }
}

@Component({
  selector: 'dialog-additional-data',
  templateUrl: 'dialog-additional-data.html',
  standalone: true,
  imports: [MatDialogModule, CommonModule],
})
export class DialogAdditionalData {
  constructor(@Inject(MAT_DIALOG_DATA) public data: any) {
  }
}
