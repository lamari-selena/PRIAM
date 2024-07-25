interface Data {
  dataId: number;
  dataName: string;
  selected: boolean;
}

export interface IndirectGeneratedDataList {
  dataTypeName: string;
  data: Data[];
  selected: boolean;
}
