import { Data } from './data-list';

export enum DataRequestType {
  ACCESS = 'ACCESS',
  RECTIFICATION = 'RECTIFICATION',
  ERASURE = 'ERASURE',
}

export enum AnswerType {
  FULL = 'FULL',
  PARTIAL = 'PARTIAL',
  REFUSED = 'REFUSED',
}

export interface DataRequestResponseDTO {
  dataRequestId: number;
  dataRequestClaim: string;
  newValue: string | null;
  requestType: DataRequestType;
  datas: Data[];
  response: boolean;
}

export interface DataRequestAnswer {
  dataRequestAnswerId: number;
  answer: AnswerType;
  dataRequestClaim: string;
}
