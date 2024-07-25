import { Data } from './data-list';

export interface Processing {
  processingName: string;
  processingId: number;
  processingType: ProcessingType;
  processingCategory: ProcessingCategory;
  createAt: number; // ?
  modifiedAt: number; // ?
  dataUsages: DataUsage[];
  purposes: Purpose[];
  measures: Measure[];
}

export enum ProcessingType {
  DEFAULT = "DEFAULT",
  NECESSARY = "NECESSARY",
  MANDATORY = "MANDATORY",
  OPTIONAL = "OPTIONAL",
}

export enum ProcessingCategory {
  CONSENT,
  LEGITIMATE_INTEREST,
  LEGAL_OBLIGATION,
  PUBLIC_INTEREST,
  VITAL_INTERESTS,
}

export interface DataUsage {
  dataUsageId: number;
  personnalStatus: boolean;
  c: boolean;
  r: boolean;
  u: boolean;
  d: boolean;
  processing?: Processing;
  data: Data;
}

export interface Purpose {
  purposeId: number;
  purposeDescription: string;
  purposeType: PurposeType;
}

export enum PurposeType {
  MAIN,
  SECONDARY,
}

export interface Measure {
  measureId: number;
  measureDescription: string;
  measureType: TypeMesure;
  measureCategory: CategoryMesure;
}

export enum TypeMesure {
  ORGANISATIONAL,
  TECHNICAL,
}

export enum CategoryMesure {
  CRYPTION,
  ANONYMISATION,
}
