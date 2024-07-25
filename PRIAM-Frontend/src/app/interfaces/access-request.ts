interface Data {
  dataId: number;
}

export interface AccessRequest {
  dataSubjectId: number;
  data: Data[];
  dataRequestClaim: string;
}
