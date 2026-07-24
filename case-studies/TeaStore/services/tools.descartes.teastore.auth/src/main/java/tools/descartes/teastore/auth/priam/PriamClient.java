/**
 * PRIAM registration/consent/bookkeeping client (playbook 4/4bis/4bis
 * "Automatic Keycloak identity provisioning"). Every PRIAM_ and KEYCLOAK_
 * variable is empty by default (fail-open/disabled), so this class is a
 * no-op unless explicitly wired up in docker-compose.yml. Talks directly to
 * PRIAM-Actor-service/PRIAM-Consent-service/PRIAM-Data-service and to
 * Keycloak's Admin API - no Gateway, no auth (machine-to-machine, playbook
 * 6) except for the Keycloak admin token itself.
 */
package tools.descartes.teastore.auth.priam;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PriamClient {

  private static final Logger LOG = LoggerFactory.getLogger(PriamClient.class);

  private static final String PRIAM_ACTOR_URL = System.getenv("PRIAM_ACTOR_URL");
  private static final String PRIAM_CDP_URL = System.getenv("PRIAM_CDP_URL");
  private static final String PRIAM_DATA_URL = System.getenv("PRIAM_DATA_URL");
  private static final String KEYCLOAK_ADMIN_URL = System.getenv("KEYCLOAK_ADMIN_URL");
  private static final String KEYCLOAK_REALM = orDefault(System.getenv("KEYCLOAK_REALM"), "priam-realm");
  private static final String KEYCLOAK_ADMIN_USERNAME = orDefault(System.getenv("KEYCLOAK_ADMIN_USERNAME"), "admin");
  private static final String KEYCLOAK_ADMIN_PASSWORD = orDefault(System.getenv("KEYCLOAK_ADMIN_PASSWORD"), "admin");

  // Databases/db_insertion_script.sql: priam-actor.data_subject_category(1) = 'TeaStore Customer'.
  private static final int DATA_SUBJECT_CATEGORY_ID = 1;
  // Databases/db_insertion_script.sql: priam-data.data(data_id) for User fields (userName/email/realName).
  public static final List<Integer> USER_DATA_IDS = List.of(1, 2, 3);
  // Databases/db_insertion_script.sql: priam-data.data(data_id) for Order fields (id + address/credit-card).
  public static final List<Integer> ORDER_DATA_IDS = List.of(4, 5, 6, 7, 8, 9, 10);
  public static final String OPTIONAL_PROCESSING = "Product Recommendations";

  private static final Duration TIMEOUT = Duration.ofSeconds(3);
  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
  private static final ObjectMapper JSON = new ObjectMapper();

  private PriamClient() {
  }

  private static String orDefault(String value, String fallback) {
    return (value == null || value.isEmpty()) ? fallback : value;
  }

  // Path segments (idRef, processingName) must be percent-encoded before
  // being concatenated into a URI - "Product Recommendations" (a real
  // processing_name with a space) otherwise makes URI.create() throw,
  // silently falling back to this class's fail-closed/fail-open defaults.
  private static String enc(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * 4bis: register every new user as a PRIAM data_subject. Idempotent
   * (upsert by idRef, DataSubjectServiceImpl.saveDataSubject) - never
   * raises, never blocks sign-up.
   * @param idRef the userName.
   */
  public static void registerDataSubject(String idRef) {
    if (PRIAM_ACTOR_URL == null || PRIAM_ACTOR_URL.isEmpty()) {
      return;
    }
    try {
      String body = JSON.writeValueAsString(Map.of("idRef", idRef,
          "dataSubjectCategoryId", DATA_SUBJECT_CATEGORY_ID));
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(PRIAM_ACTOR_URL + "/api/DataSubject"))
          .timeout(TIMEOUT)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body))
          .build();
      HTTP.send(request, HttpResponse.BodyHandlers.discarding());
    } catch (Exception e) {
      LOG.warn("PRIAM registerDataSubject({}) failed", idRef, e);
    }
  }

  /**
   * 4bis: report which annotated data_ids a subject now holds a record of
   * (bookkeeping for the Access Request page - 8.1.b). Must be called
   * after registerDataSubject() has committed - see 8.6 (idRef ->
   * dataSubjectId resolution races with the DataSubject insert).
   * @param idRef the userName.
   * @param dataIds the data_id values (1 point 4) held by this subject.
   */
  public static void reportProcessedData(String idRef, List<Integer> dataIds) {
    if (PRIAM_ACTOR_URL == null || PRIAM_ACTOR_URL.isEmpty()
        || PRIAM_DATA_URL == null || PRIAM_DATA_URL.isEmpty()) {
      return;
    }
    try {
      HttpRequest idRequest = HttpRequest.newBuilder()
          .uri(URI.create(PRIAM_ACTOR_URL + "/api/DataSubjectId/" + enc(idRef)))
          .timeout(TIMEOUT)
          .GET()
          .build();
      HttpResponse<String> idResponse = HTTP.send(idRequest, HttpResponse.BodyHandlers.ofString());
      if (idResponse.statusCode() / 100 != 2) {
        LOG.warn("PRIAM reportProcessedData({}): DataSubjectId lookup returned {}", idRef, idResponse.statusCode());
        return;
      }
      String subjectId = idResponse.body().trim();
      HttpRequest addRequest = HttpRequest.newBuilder()
          .uri(URI.create(PRIAM_DATA_URL + "/api/processed-data/add?subjectId=" + subjectId))
          .timeout(TIMEOUT)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(dataIds)))
          .build();
      HTTP.send(addRequest, HttpResponse.BodyHandlers.discarding());
    } catch (Exception e) {
      LOG.warn("PRIAM reportProcessedData({}) failed", idRef, e);
    }
  }

  /**
   * 4bis: "is there already a consent decision at all" (Consent
   * Information Point) - distinct from getConsent()'s "is it granted".
   * @param idRef the userName.
   * @param processingName the OPTIONAL processing's name.
   * @return true if the subject has never answered this processing yet.
   */
  public static boolean hasPendingConsentDecision(String idRef, String processingName) {
    if (PRIAM_CDP_URL == null || PRIAM_CDP_URL.isEmpty()) {
      return false;
    }
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(PRIAM_CDP_URL + "/api/contract/list/consents/" + enc(idRef) + "/" + enc(processingName)))
          .timeout(TIMEOUT)
          .GET()
          .build();
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        return false;
      }
      List<?> decisions = JSON.readValue(response.body(), List.class);
      return decisions.isEmpty();
    } catch (Exception e) {
      LOG.warn("PRIAM hasPendingConsentDecision({}) failed", idRef, e);
      return false;
    }
  }

  /**
   * 4bis "Automatic Keycloak identity provisioning": TeaStore has its own
   * local sign-up (no OIDC/SSO of its own), so nothing else would ever
   * create the matching Keycloak account. Fire-and-forget: never raises,
   * never blocks sign-up. A 409 (already provisioned) is not an error.
   * @param idRef the userName (kept as the Keycloak idReference attribute).
   * @param email used as the Keycloak login username (8.8: idRef can be
   *     shorter than Keycloak's 3-character minimum).
   * @param realName reused for firstName/lastName (TeaStore has no separate fields).
   * @param password the plaintext password, only available at this exact moment.
   */
  public static void provisionKeycloakUser(String idRef, String email, String realName, String password) {
    if (KEYCLOAK_ADMIN_URL == null || KEYCLOAK_ADMIN_URL.isEmpty()) {
      return;
    }
    try {
      HttpRequest tokenRequest = HttpRequest.newBuilder()
          .uri(URI.create(KEYCLOAK_ADMIN_URL + "/realms/master/protocol/openid-connect/token"))
          .timeout(TIMEOUT)
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(
              "grant_type=password&client_id=admin-cli"
                  + "&username=" + KEYCLOAK_ADMIN_USERNAME + "&password=" + KEYCLOAK_ADMIN_PASSWORD))
          .build();
      HttpResponse<String> tokenResponse = HTTP.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
      if (tokenResponse.statusCode() / 100 != 2) {
        LOG.warn("PRIAM provisionKeycloakUser({}): admin token request returned {}", idRef,
            tokenResponse.statusCode());
        return;
      }
      String adminToken = (String) JSON.readValue(tokenResponse.body(), Map.class).get("access_token");

      Map<String, Object> body = Map.of(
          "username", email,
          "email", email,
          "enabled", true,
          "emailVerified", true,
          "firstName", realName,
          "lastName", realName,
          "credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)),
          "attributes", Map.of("idReference", List.of(idRef)));
      HttpRequest createRequest = HttpRequest.newBuilder()
          .uri(URI.create(KEYCLOAK_ADMIN_URL + "/admin/realms/" + KEYCLOAK_REALM + "/users"))
          .timeout(TIMEOUT)
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + adminToken)
          .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
          .build();
      HttpResponse<String> createResponse = HTTP.send(createRequest, HttpResponse.BodyHandlers.ofString());
      if (createResponse.statusCode() != 201 && createResponse.statusCode() != 409) {
        LOG.warn("PRIAM provisionKeycloakUser({}): unexpected status {}: {}", idRef,
            createResponse.statusCode(), createResponse.body());
      }
    } catch (Exception e) {
      LOG.warn("PRIAM provisionKeycloakUser({}) failed", idRef, e);
    }
  }
}
