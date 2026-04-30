interface PrimaryKey {
  primaryKeyId: number;
  primaryKeyName: string;
}

interface Data {
  dataId: number;
  dataName: string;
  answerByData: boolean;
  primaryKeys: Map<String, String>;
}

interface DataType {
  dataTypeName: string;
  primaryKeys: PrimaryKey[];
  data: Data[];
  answerByData: boolean;
}

interface DataSubject {
  dataSubjectId: number;
  idRef: string;
  dataSubjectCategoryName: string;
}

// export interface RequestData {
//   requestId: number;
//   userClaim: string;
//   issuedAt: Date;
//   newValue?: any; // A MODIF POUR GERER TOUS TYPES
//   isIsolated: boolean;
//   dataTypes: DataType[];
//   dataSubject: DataSubject;
// }

export interface RequestData {
  dataRequestId: number;
  typeRequest: String;
  response: boolean;
  dataRequestClaim: string;
  dataRequestIssuedAt: Date;
  newValue?: any;
  isIsolated: boolean;
  dataSubject: DataSubject;
  dataTypeList: DataType[];
}

export interface DataValue {
  value: String
}