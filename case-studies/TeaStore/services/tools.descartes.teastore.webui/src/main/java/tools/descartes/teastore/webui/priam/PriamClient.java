/**
 * PRIAM Consent Enforcement Point (CEP) for the webui module (playbook 4).
 * Talks directly to PRIAM-Consent-Service - no Gateway, no auth
 * (machine-to-machine). Separate, minimal copy from the auth module's
 * PriamClient (which owns registration/bookkeeping/Keycloak provisioning) -
 * this module only ever needs the read-only consent check.
 */
package tools.descartes.teastore.webui.priam;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PriamClient {

  private static final Logger LOG = LoggerFactory.getLogger(PriamClient.class);
  private static final String PRIAM_CDP_URL = System.getenv("PRIAM_CDP_URL");
  // Databases/db_insertion_script.sql: priam-data.processing(3) - the one
  // OPTIONAL processing in this integration (CartServlet's ad block).
  public static final String OPTIONAL_PROCESSING = "Product Recommendations";
  private static final Duration TIMEOUT = Duration.ofSeconds(3);
  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
  private static final ObjectMapper JSON = new ObjectMapper();

  private PriamClient() {
  }

  // URLEncoder.encode() is form-encoding (application/x-www-form-urlencoded):
  // it turns a space into "+", which is only meaningful inside a query
  // string - inside a PATH SEGMENT (as processingName is used here), "+" is
  // a literal plus sign, not a decoded space. "Product+Recommendations" as
  // a path segment does not match the real "Product Recommendations"
  // processing name, and PRIAM-Consent-Service 500s trying to resolve it -
  // silently turned into this class's fail-closed default (a second, more
  // subtle instance of the encoding bug already fixed once for the
  // IllegalArgumentException case - see priam-integration/INTEGRATION-REPORT.md).
  private static String encPathSegment(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  /**
   * CEP (4): consent for an OPTIONAL processing. Fail-open if PRIAM is not
   * configured, fail-closed (deny) if PRIAM is configured but unreachable.
   * @param idRef the userName.
   * @param processingName the OPTIONAL processing's name.
   * @return whether the subject has granted consent.
   */
  public static boolean getConsent(String idRef, String processingName) {
    if (PRIAM_CDP_URL == null || PRIAM_CDP_URL.isEmpty()) {
      return true;
    }
    try {
      String url = PRIAM_CDP_URL + "/api/decision/" + encPathSegment(processingName)
          + "?idRefList=" + URLEncoder.encode(idRef, StandardCharsets.UTF_8);
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT).GET().build();
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() / 100 != 2) {
        return false;
      }
      Map<?, ?> decision = JSON.readValue(response.body(), Map.class);
      return Boolean.TRUE.equals(decision.get(idRef));
    } catch (Exception e) {
      LOG.warn("PRIAM getConsent({}) failed", idRef, e);
      return false;
    }
  }
}
