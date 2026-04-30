interface Purpose {
  purposeDescription: string;
}

interface Data {
  dataName: string;
}

export interface Processing {
  processingName: string;
  purposes: Purpose[];
  data: Data[];
}
