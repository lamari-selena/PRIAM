interface DataTransfer {
  data: Data[]
}

interface Data {
  dataName: string;
}

interface SecondaryActorCategory {
  secondaryActorCategoryId: number;
  secondaryActorCategoryName: string;
}

interface Country {
  countryId: number;
  countryName: string;
  minorAge: number;
  adequate: boolean;
}

export interface SecondaryActor {
  secondaryActorName: string;
  secondaryActorCategory: SecondaryActorCategory;
  country: Country;
  dataTransfers: DataTransfer;
  safeguard?: any; // A MODIFIER POUR GERER PDF
  safeguardType?: string;
}
