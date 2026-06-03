/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.descartes.teastore.recommender.priam;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * PRIAM Consent Enforcement Point (CEP) for the TeaStore recommender service.
 *
 * Queries PRIAM's Consent Decision Point (CDP) before executing the
 * purchase-recommendations optional processing.
 *
 * If the environment variable PRIAM_CDP_URL is not set, getConsent() returns
 * true to preserve existing behaviour in environments without PRIAM.
 * If PRIAM is reachable but consent is denied, or if the CDP is unreachable,
 * returns false (deny-by-default when explicitly configured).
 */
public final class ConsentClient {

    private static final Logger LOG = Logger.getLogger(ConsentClient.class.getName());

    private static final String CDP_URL = System.getenv("PRIAM_CDP_URL");

    private ConsentClient() { }

    /**
     * Check whether the user identified by {@code userId} has granted consent
     * for {@code processingId}.
     *
     * @param userId      The user's identifier (long, as a string).
     * @param processingId The processing activity name declared in PRIAM.
     * @return {@code true} if consent is granted (or PRIAM is not configured),
     *         {@code false} if consent is denied or PRIAM is unreachable.
     */
    public static boolean getConsent(String userId, String processingId) {
        if (CDP_URL == null || CDP_URL.isEmpty()) {
            return true;
        }
        try {
            String encodedProcessing = URLEncoder.encode(processingId, StandardCharsets.UTF_8);
            String encodedUser       = URLEncoder.encode(userId, StandardCharsets.UTF_8);
            URL url = new URL(CDP_URL + "/api/decision/" + encodedProcessing
                    + "?idRefList=" + encodedUser);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() != 200) {
                LOG.warning("PRIAM CDP returned " + conn.getResponseCode() + ". Denying.");
                return false;
            }

            try (InputStream is = conn.getInputStream();
                 JsonReader reader = Json.createReader(is)) {
                JsonObject decision = reader.readObject();
                return decision.getBoolean(userId, false);
            }
        } catch (Exception e) {
            LOG.warning("PRIAM CDP unreachable (" + e.getMessage() + "). Denying by default.");
            return false;
        }
    }
}