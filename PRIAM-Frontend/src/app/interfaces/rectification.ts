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

export interface Rectification {
  dataSubjectId: number;
  dataTypeName: string;
  data: Data;
  newValue: any; // A MODIF PLUS TARD POUR GERER LE RESTE
  claim: string;
  primaryKeys: PrimaryKey[];
}
