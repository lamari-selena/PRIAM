interface Data {
  dataId: number;
}

export interface CompletedAccessRequest {
  dataRequestId: number;
  providerClaim: string;
  data: Data[];
}
