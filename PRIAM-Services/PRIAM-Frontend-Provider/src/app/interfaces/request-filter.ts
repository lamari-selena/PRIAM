import {DataSubjectCategory} from "./data-subject-category";

export interface RequestFilter {
  requestTypes: string[];
  requestResponses: string[];
  dataSubjectCategories: DataSubjectCategory[];
}
