import { SymbolProfile } from '@prisma/client';

import { Statistics } from './statistics.interface';
import { SubscriptionOffer } from './subscription-offer.interface';

export interface InfoItem {
  baseCurrency: string;
  benchmarks: Partial<SymbolProfile>[];
  countriesOfSubscribers?: string[];
  currencies: string[];
  demoAuthToken: string;
  fearAndGreedStocksMarketPrice?: number;
  globalPermissions: string[];
  isDataGatheringEnabled?: string;
  isReadOnlyMode?: boolean;
  // Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4ter - drives the "Manage on PRIAM"
  // link and the forced-consent redirect target; undefined = PRIAM not wired up.
  priamFrontendUrl?: string;
  statistics: Statistics;
  subscriptionOffer?: SubscriptionOffer;
}
