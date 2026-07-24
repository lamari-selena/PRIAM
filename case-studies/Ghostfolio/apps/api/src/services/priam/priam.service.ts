import { Injectable, Logger } from '@nestjs/common';

// PRIAM integration (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4/§4bis/§4ter).
// Every PRIAM_*/KEYCLOAK_* variable is empty by default (fail-open/disabled),
// so this service is a no-op unless explicitly wired up in
// case-studies/Ghostfolio/docker/docker-compose.yml.
const CDP_URL = process.env.PRIAM_CDP_URL;
const ACTOR_URL = process.env.PRIAM_ACTOR_URL;
const DATA_URL = process.env.PRIAM_DATA_URL;
const KEYCLOAK_ADMIN_URL = process.env.KEYCLOAK_ADMIN_URL;
const KEYCLOAK_REALM = process.env.KEYCLOAK_REALM || 'priam-realm';
const KEYCLOAK_ADMIN_USERNAME = process.env.KEYCLOAK_ADMIN_USERNAME || 'admin';
const KEYCLOAK_ADMIN_PASSWORD = process.env.KEYCLOAK_ADMIN_PASSWORD || 'admin';

// Databases/db_insertion_script.sql: priam-actor.data_subject_category(1) =
// 'Ghostfolio Investor'.
const DATA_SUBJECT_CATEGORY_ID = 1;
// Databases/db_insertion_script.sql: priam-data.data(data_id) per data_type.
export const USER_DATA_IDS = [1, 2, 3, 4];
export const ACCOUNT_DATA_IDS = [5, 6, 7, 8];
export const ORDER_DATA_IDS = [9, 10, 11, 12, 13, 14, 15, 16];
export const ANALYTICS_DATA_IDS = [17, 18, 19];
export const USAGE_ANALYTICS_PROCESSING = 'Usage Analytics';

@Injectable()
export class PriamService {
  private readonly logger = new Logger(PriamService.name);

  // CEP (§4): consent for an OPTIONAL processing. Fail-open if PRIAM is not
  // configured, fail-closed (deny) if PRIAM is configured but unreachable.
  public async getConsent(
    idRef: string,
    processingName: string
  ): Promise<boolean> {
    if (!CDP_URL) {
      return true;
    }

    try {
      const url = `${CDP_URL}/api/decision/${encodeURIComponent(processingName)}?idRefList=${encodeURIComponent(idRef)}`;
      const response = await fetch(url, { signal: AbortSignal.timeout(3000) });

      if (!response.ok) {
        return false;
      }

      const decision = await response.json();

      return decision[idRef] === true;
    } catch (error) {
      this.logger.warn(`getConsent failed: ${error}`);
      return false;
    }
  }

  // §4bis: register every new user as a PRIAM data_subject. Idempotent
  // (upsert by idRef on the Actor-service side) - never throws, never blocks
  // sign-up.
  public async registerDataSubject(idRef: string): Promise<void> {
    if (!ACTOR_URL) {
      return;
    }

    try {
      await fetch(`${ACTOR_URL}/api/DataSubject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          idRef,
          dataSubjectCategoryId: DATA_SUBJECT_CATEGORY_ID
        }),
        signal: AbortSignal.timeout(3000)
      });
    } catch (error) {
      this.logger.warn(`registerDataSubject failed: ${error}`);
    }
  }

  // §4bis: "is there already a consent decision at all" (Consent
  // Information Point) - distinct from getConsent()'s "is it granted".
  public async hasPendingConsentDecision(
    idRef: string,
    processingName: string
  ): Promise<boolean> {
    if (!CDP_URL) {
      return false;
    }

    try {
      const url = `${CDP_URL}/api/contract/list/consents/${encodeURIComponent(idRef)}/${encodeURIComponent(processingName)}`;
      const response = await fetch(url, { signal: AbortSignal.timeout(3000) });

      if (!response.ok) {
        return false;
      }

      const decisions = await response.json();

      return Array.isArray(decisions) && decisions.length === 0;
    } catch {
      return false;
    }
  }

  // §4bis: report which annotated data_ids a subject now holds a record of
  // (bookkeeping for the Access Request page - §8.1.b). Must be called at
  // every point a personal record is created, not just at sign-up.
  public async reportProcessedData(
    idRef: string,
    dataIds: number[]
  ): Promise<void> {
    if (!ACTOR_URL || !DATA_URL || !dataIds?.length) {
      return;
    }

    try {
      const idResponse = await fetch(
        `${ACTOR_URL}/api/DataSubjectId/${encodeURIComponent(idRef)}`,
        { signal: AbortSignal.timeout(3000) }
      );

      if (!idResponse.ok) {
        return;
      }

      const dataSubjectId = await idResponse.json();

      await fetch(
        `${DATA_URL}/api/processed-data/add?subjectId=${dataSubjectId}`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(dataIds),
          signal: AbortSignal.timeout(3000)
        }
      );
    } catch (error) {
      this.logger.warn(`reportProcessedData failed: ${error}`);
    }
  }

  // §4bis "Automatic Keycloak identity provisioning": Ghostfolio's User
  // model has no email (see UserService.createUser) - only the anonymous
  // sign-up's one-time accessToken can serve as the synced password, and
  // only that flow is covered (documented limitation, not silently
  // ignored - Google/OIDC sign-ins have no equivalent secret, §4bis).
  public async provisionKeycloakUser(
    idRef: string,
    password: string
  ): Promise<void> {
    if (!KEYCLOAK_ADMIN_URL || !password) {
      return;
    }

    // Ghostfolio has no email/handle of its own (§8.8) - synthesize a
    // deterministic, always-well-formed Keycloak username.
    const username = `${idRef}@ghostfolio.local`;

    try {
      const tokenResponse = await fetch(
        `${KEYCLOAK_ADMIN_URL}/realms/master/protocol/openid-connect/token`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            grant_type: 'password',
            client_id: 'admin-cli',
            username: KEYCLOAK_ADMIN_USERNAME,
            password: KEYCLOAK_ADMIN_PASSWORD
          }),
          signal: AbortSignal.timeout(5000)
        }
      );

      if (!tokenResponse.ok) {
        return;
      }

      const { access_token: token } = await tokenResponse.json();

      // firstName/lastName/email required by the realm's default User
      // Profile (§4bis) - reused from the synthesized username, Ghostfolio
      // has no separate first/last name at sign-up.
      const response = await fetch(
        `${KEYCLOAK_ADMIN_URL}/admin/realms/${KEYCLOAK_REALM}/users`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({
            username,
            email: username,
            enabled: true,
            emailVerified: true,
            firstName: username,
            lastName: username,
            credentials: [{ type: 'password', value: password, temporary: false }],
            attributes: { idReference: [idRef] }
          }),
          signal: AbortSignal.timeout(5000)
        }
      );

      if (response.status !== 201 && response.status !== 409) {
        this.logger.warn(
          `provisionKeycloakUser: unexpected status ${response.status}`
        );
      }
    } catch (error) {
      this.logger.warn(`provisionKeycloakUser failed: ${error}`);
    }
  }

  // Orchestrates the sign-up-time PRIAM calls in the order required to
  // avoid the registration race (§4bis/§8.6): registerDataSubject() MUST
  // resolve before any call that resolves idRef -> dataSubjectId internally
  // (reportProcessedData()). Fire-and-forget from the caller's point of
  // view (never awaited by the HTTP response), but internally sequential.
  public onUserRegistered(idRef: string, accessToken?: string): void {
    (async () => {
      await this.registerDataSubject(idRef);
      // UserService.createUser() always creates a default Account alongside
      // the User row itself (playbook §1 point 11 / §4bis "processed data
      // right at sign-up").
      await this.reportProcessedData(idRef, USER_DATA_IDS);
      await this.reportProcessedData(idRef, ACCOUNT_DATA_IDS);
      await this.provisionKeycloakUser(idRef, accessToken);
    })().catch((error) =>
      this.logger.warn(`onUserRegistered background chain failed: ${error}`)
    );
  }
}
