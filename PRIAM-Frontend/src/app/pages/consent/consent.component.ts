import { Component, OnInit } from '@angular/core';
import {
  CategoryMesure,
  DataUsage,
  Measure,
  Processing,
  ProcessingCategory,
  ProcessingType,
  Purpose,
  PurposeType,
  TypeMesure,
} from 'src/app/interfaces/processing';
import { Consent } from 'src/app/interfaces/consent';

import { SecurityService } from 'src/app/shared/services/security.service';
import { DialogAdditionalData } from '../access-request/access-request.component';
import { MatDialog } from '@angular/material/dialog';
import { Data } from 'src/app/interfaces/data-list';
import { ConsentService } from 'src/app/shared/services/api/consents/consentements.service';

@Component({
  selector: 'app-consent',
  templateUrl: './consent.component.html',
  styleUrls: ['./consent.component.css'],
})
export class ConsentComponent implements OnInit {
  constructor(
    public securityService: SecurityService,
    public getConsentsService: ConsentService,
    public dialog: MatDialog
  ) {}

  referenceId: number | null = this.securityService.getIdReference();
  dataList: Processing[] = [];
  optionalList: Processing[] = [];
  necessaryList: Processing[] = [];
  consents: { [id: Processing['processingId']]: boolean } = {};
  devs: any = {};
  loaded: Promise<unknown> = this.getProcessings();

  ngOnInit() {}

  getProcessings() {
    return new Promise((res, rej) => {
      this.getConsentsService
        .getProcessings()
        .subscribe((response: Processing[]) => {
          this.dataList = response;
          this.optionalList = this.dataList.filter(
            (a) => a.processingType === ProcessingType.OPTIONAL
          );
          this.necessaryList = this.dataList.filter(
            (a) => a.processingType === ProcessingType.NECESSARY
          );
          if (this.referenceId)
            for (const pro of response) {
              this.getConsentsService
                .getConsents(this.referenceId, pro.processingId)
                .subscribe((res: Consent[]) => {
                  if (res.length)
                    this.consents[pro.processingId] =
                      !res[res.length - 1].endDate;
                });
            }
          res(1);
        });
    });
  }

  post(processing: Processing) {
    if (this.referenceId)
      this.getConsentsService.postConsent(
        this.referenceId,
        processing.processingId
      ).subscribe((rep) => {
      })
    
  }

  openDialogAdditionalData(dataItem: any) {
    this.dialog.open(DialogAdditionalData, {
      data: {
        selectedData: dataItem,
      },
    });
  }

  isDisable(data: Processing) {
    return data.processingType === ProcessingType.MANDATORY ||
      data.processingType === ProcessingType.NECESSARY
      ? true
      : null;
  }

  isActivate(data: Processing) {
    return this.consents[data.processingId];
  }

  open(data: Processing) {
    this.devs[data.processingId] =
      this.devs[data.processingId] !== undefined
        ? !this.devs[data.processingId]
        : true;
  }
}

function generateurData(): Processing {
  const genGenEnum = (enums: Object) =>
    Object.values(enums)[
      Math.floor(Math.random() * (Object.keys(enums).length / 2))
    ];
  const genProcessingCategory = () => genGenEnum(ProcessingCategory);
  const genProcessingType = () => genGenEnum(ProcessingType);
  const genPurposeType = () => genGenEnum(PurposeType);
  const genTypeMesure = () => genGenEnum(TypeMesure);
  const genCategoryMesure = () => genGenEnum(CategoryMesure);

  const alpha = 'abcdefghijklmopqrstuvwxyz';
  const genName: (size: number) => string = (size: number = 10) =>
    new Array(size)
      .fill(undefined)
      .map(() => alpha[Math.floor(Math.random() * alpha.length)])
      .join('');

  const genBool = () => Math.random() > 0.5;
  const genNb = () => Math.floor(Math.random() * 100000);
  const genData = (): Data =>
    ({
      dataId: genNb(),
      dataName: genName(10),
      dataValue: genName(30),
      isPrimaryKey: genBool(),
      dataConservationDuration: genName(10),
      personalDataCategory: genName(10),
      source: genName(10),
      sourceDetails: genName(10),
    } as Data);

  const genDataUsages = (size: number = 10): DataUsage[] =>
    new Array(size).fill(undefined).map((a) => {
      return {
        c: genBool(),
        r: genBool(),
        u: genBool(),
        d: genBool(),
        dataUsageId: Math.floor(Math.random() * 10000),
        data: genData(),
        personnalStatus: genBool(),
        processing: undefined,
      } as DataUsage;
    });

  const genMeasures = (size: number = 10): Measure[] =>
    new Array(size).fill(undefined).map(() => {
      return {
        measureCategory: genCategoryMesure(),
        measureDescription: genName(100),
        measureId: genNb(),
        measureType: genTypeMesure(),
      } as Measure;
    });

  const genPurpose = (size: number = 10): Purpose[] =>
    new Array(size).fill(undefined).map(() => {
      return {
        purposeDescription: genName(10),
        purposeId: genNb(),
        purposeType: genPurposeType(),
      } as Purpose;
    });

  return {
    createAt: new Date().getTime(),
    modifiedAt: new Date().getTime(),
    processingCategory: genProcessingCategory(),
    processingId: Math.floor(Math.random() * 1000000),
    processingName: genName(10),
    processingType: genProcessingType(),
    dataUsages: genDataUsages(10),
    measures: genMeasures(10),
    purposes: genPurpose(10),
  };
}
