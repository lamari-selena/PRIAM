import nconf from 'nconf';
import logger from './logger';

// PRIAM integration (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4/§4bis). Every
// PRIAM_*/KEYCLOAK_* variable is empty by default (fail-open/disabled) so
// this file is a no-op unless explicitly wired up in docker-compose.yml.
const CDP_URL = nconf.get('PRIAM_CDP_URL');
const ACTOR_URL = nconf.get('PRIAM_ACTOR_URL');
const DATA_URL = nconf.get('PRIAM_DATA_URL');
const KEYCLOAK_ADMIN_URL = nconf.get('KEYCLOAK_ADMIN_URL');
const KEYCLOAK_REALM = nconf.get('KEYCLOAK_REALM') || 'priam-realm';
const KEYCLOAK_ADMIN_USERNAME = nconf.get('KEYCLOAK_ADMIN_USERNAME') || 'admin';
const KEYCLOAK_ADMIN_PASSWORD = nconf.get('KEYCLOAK_ADMIN_PASSWORD') || 'admin';

// Databases/db_insertion_script.sql: priam-actor.data_subject_category(1) = 'Habitica Player'.
const DATA_SUBJECT_CATEGORY_ID = 1;
// Databases/db_insertion_script.sql: priam-data.data(data_id) for Task fields (id/text/notes).
const TASK_DATA_IDS = [4, 5, 6];
// Databases/db_insertion_script.sql: priam-data.data(data_id) for User fields (username/email/displayName).
const USER_DATA_IDS = [1, 2, 3];
// Databases/db_insertion_script.sql: priam-data.data(data_id) for PushDevice fields (regId/type).
const PUSH_DEVICE_DATA_IDS = [7, 8];
const PUSH_NOTIFICATIONS_PROCESSING = 'Push Notifications';

// CEP (§4): consent for an OPTIONAL processing. Fail-open if PRIAM is not
// configured, fail-closed (deny) if PRIAM is configured but unreachable.
async function getConsent (idRef, processingName) {
  if (!CDP_URL) return true;
  try {
    const url = `${CDP_URL}/api/decision/${encodeURIComponent(processingName)}?idRefList=${encodeURIComponent(idRef)}`;
    const response = await fetch(url, { signal: AbortSignal.timeout(3000) });
    if (!response.ok) return false;
    const decision = await response.json();
    return decision[idRef] === true;
  } catch (err) {
    logger.error(err, 'PRIAM getConsent failed');
    return false;
  }
}

// §4bis: register every new user as a PRIAM data_subject. Idempotent
// (upsert by idRef on the Actor-service side) — never raises, never blocks
// sign-up.
async function registerDataSubject (idRef) {
  if (!ACTOR_URL) return;
  try {
    await fetch(`${ACTOR_URL}/api/DataSubject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idRef, dataSubjectCategoryId: DATA_SUBJECT_CATEGORY_ID }),
      signal: AbortSignal.timeout(3000),
    });
  } catch (err) {
    logger.error(err, 'PRIAM registerDataSubject failed');
  }
}

// §4bis: "is there already a consent decision at all" (Consent Information
// Point) - distinct from getConsent's "is it granted".
async function hasPendingConsentDecision (idRef, processingName) {
  if (!CDP_URL) return false;
  try {
    const url = `${CDP_URL}/api/contract/list/consents/${encodeURIComponent(idRef)}/${encodeURIComponent(processingName)}`;
    const response = await fetch(url, { signal: AbortSignal.timeout(3000) });
    if (!response.ok) return false;
    const decisions = await response.json();
    return Array.isArray(decisions) && decisions.length === 0;
  } catch (err) {
    return false;
  }
}

// §4bis: report which annotated data_ids a subject now holds a record of
// (bookkeeping for the Access Request page - §8.1.b). Must be called at
// every point a personal record is created, not just at sign-up.
async function reportProcessedData (idRef, dataIds) {
  if (!ACTOR_URL || !DATA_URL || !dataIds || dataIds.length === 0) return;
  try {
    const idResponse = await fetch(`${ACTOR_URL}/api/DataSubjectId/${encodeURIComponent(idRef)}`, {
      signal: AbortSignal.timeout(3000),
    });
    if (!idResponse.ok) return;
    const dataSubjectId = await idResponse.json();
    await fetch(`${DATA_URL}/api/processed-data/add?subjectId=${dataSubjectId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dataIds),
      signal: AbortSignal.timeout(3000),
    });
  } catch (err) {
    logger.error(err, 'PRIAM reportProcessedData failed');
  }
}

// §4bis "Automatic Keycloak identity provisioning": Habitica has its own
// local sign-up, so nothing else would ever create a matching Keycloak
// account. Covers local sign-up only (no plaintext password for social
// sign-up - documented limitation, not silently ignored).
async function provisionKeycloakUser (idRef, email, password) {
  // No password to sync for a social sign-up (§4bis "covers local sign-up
  // only") - documented limitation, not silently attempted with a broken
  // credential.
  if (!KEYCLOAK_ADMIN_URL || !email || !password) return;
  try {
    const tokenResponse = await fetch(`${KEYCLOAK_ADMIN_URL}/realms/master/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'password',
        client_id: 'admin-cli',
        username: KEYCLOAK_ADMIN_USERNAME,
        password: KEYCLOAK_ADMIN_PASSWORD,
      }),
      signal: AbortSignal.timeout(5000),
    });
    if (!tokenResponse.ok) return;
    const { access_token: token } = await tokenResponse.json();
    // Keycloak username = email, not the Habitica handle (§4bis/§8.8): a
    // Habitica username can be as short as 1 character, below Keycloak's
    // 3-char minimum, and would silently 400 (real repro: account "w").
    // firstName/lastName are required by the realm's User Profile - reused
    // from email since Habitica has no separate first/last name fields.
    const response = await fetch(`${KEYCLOAK_ADMIN_URL}/admin/realms/${KEYCLOAK_REALM}/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify({
        username: email,
        email,
        enabled: true,
        emailVerified: true,
        firstName: email,
        lastName: email,
        credentials: [{ type: 'password', value: password, temporary: false }],
        attributes: { idReference: [idRef] },
      }),
      signal: AbortSignal.timeout(5000),
    });
    if (response.status !== 201 && response.status !== 409) {
      logger.info('PRIAM provisionKeycloakUser: unexpected status', { status: response.status });
    }
  } catch (err) {
    logger.error(err, 'PRIAM provisionKeycloakUser failed');
  }
}

// Orchestrates the sign-up-time PRIAM calls in the order required to avoid
// the registration race (§4bis/§8.6): registerDataSubject MUST resolve
// before any call that resolves idRef -> dataSubjectId internally
// (reportProcessedData). Fire-and-forget from the caller's point of view
// (never awaited by the HTTP response), but internally sequential.
function onUserRegistered (idRef, defaultTaskCount, email, plainPassword) {
  (async () => {
    await registerDataSubject(idRef);
    await reportProcessedData(idRef, USER_DATA_IDS);
    for (let i = 0; i < defaultTaskCount; i += 1) {
      // eslint-disable-next-line no-await-in-loop
      await reportProcessedData(idRef, TASK_DATA_IDS);
    }
    await provisionKeycloakUser(idRef, email, plainPassword);
  })().catch(err => logger.error(err, 'PRIAM onUserRegistered background chain failed'));
}

export default {
  getConsent,
  registerDataSubject,
  hasPendingConsentDecision,
  reportProcessedData,
  provisionKeycloakUser,
  onUserRegistered,
  TASK_DATA_IDS,
  PUSH_DEVICE_DATA_IDS,
  PUSH_NOTIFICATIONS_PROCESSING,
};
