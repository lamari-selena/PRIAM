import {DataSubjectCategory} from "./data-subject-category";

export interface Request {
  dataRequestId: number;
  dataRequestType: string;
  dataRequestIssuedAt: Date;
  dataSubjectCategory: DataSubjectCategory;
  response: boolean;
}
