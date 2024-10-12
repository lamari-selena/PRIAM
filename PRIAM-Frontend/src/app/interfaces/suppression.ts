export interface Data {
  dataId: number;
}

export interface PrimaryKey {
  primaryKeyValue: any;
  primaryKeyName: string;
}

export interface NonPrimaryKey {
  dataId: number;
  dataName: string;
  dataValue: any;
  dataType: string;
}

export interface Suppression {
  dataSubjectId: number;
  dataTypeName: string;
  data: Data;
  claim: string;
  primaryKeys: PrimaryKey[];
}
