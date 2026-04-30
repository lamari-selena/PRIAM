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

package tools.descartes.teastore.auth.rest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import tools.descartes.teastore.auth.security.BCryptProvider;
import tools.descartes.teastore.auth.security.RandomSessionIdGenerator;
import tools.descartes.teastore.auth.security.ShaSecurityProvider;
import tools.descartes.teastore.entities.Order;
import tools.descartes.teastore.entities.OrderItem;
import tools.descartes.teastore.entities.User;
import tools.descartes.teastore.entities.message.SessionBlob;
import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.loadbalancers.LoadBalancerTimeoutException;
import tools.descartes.teastore.registryclient.rest.LoadBalancedCRUDOperations;
import tools.descartes.teastore.registryclient.util.NotFoundException;
import tools.descartes.teastore.registryclient.util.TimeoutException;

import java.math.BigInteger;
import java.security.MessageDigest;
/**
 * Rest endpoint for the store user actions.
 * 
 * @author Simon
 */
@Path("useractions")
@Produces({ "application/json" })
@Consumes({ "application/json" })
public class AuthUserActionsRest {

  /**
   * Persists order in database.
   * 
   * @param blob
   *          SessionBlob
   * @param totalPriceInCents
   *          totalPrice
   * @param addressName
   *          address
   * @param address1
   *          address
   * @param address2
   *          address
   * @param creditCardCompany
   *          creditcard
   * @param creditCardNumber
   *          creditcard
   * @param creditCardExpiryDate
   *          creditcard
   * @return Response containing SessionBlob
   */
  @POST
  @Path("placeorder")
  public Response placeOrder(SessionBlob blob,
      @QueryParam("totalPriceInCents") long totalPriceInCents,
      @QueryParam("addressName") String addressName, @QueryParam("address1") String address1,
      @QueryParam("address2") String address2,
      @QueryParam("creditCardCompany") String creditCardCompany,
      @QueryParam("creditCardNumber") String creditCardNumber,
      @QueryParam("creditCardExpiryDate") String creditCardExpiryDate) {
    if (new ShaSecurityProvider().validate(blob) == null || blob.getOrderItems().isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    blob.getOrder().setUserId(blob.getUID());
    blob.getOrder().setTotalPriceInCents(totalPriceInCents);
    blob.getOrder().setAddressName(addressName);
    blob.getOrder().setAddress1(address1);
    blob.getOrder().setAddress2(address2);
    blob.getOrder().setCreditCardCompany(creditCardCompany);
    blob.getOrder().setCreditCardExpiryDate(creditCardExpiryDate);
    blob.getOrder().setCreditCardNumber(creditCardNumber);
    blob.getOrder().setTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    long orderId;
    try {
      orderId = LoadBalancedCRUDOperations.sendEntityForCreation(Service.PERSISTENCE, "orders",
          Order.class, blob.getOrder());
    } catch (LoadBalancerTimeoutException e) {
      return Response.status(408).build();
    } catch (NotFoundException e) {
      return Response.status(404).build();
    }
    for (OrderItem item : blob.getOrderItems()) {
      try {
        item.setOrderId(orderId);
        LoadBalancedCRUDOperations.sendEntityForCreation(Service.PERSISTENCE, "orderitems",
            OrderItem.class, item);
      } catch (TimeoutException e) {
        return Response.status(408).build();
      } catch (NotFoundException e) {
        return Response.status(404).build();
      }
    }
    blob.setOrder(new Order());
    blob.getOrderItems().clear();
    blob = new ShaSecurityProvider().secure(blob);
    return Response.status(Response.Status.OK).entity(blob).build();
  }

  /**
   * User login.
   * 
   * @param blob
   *          SessionBlob
   * @param name
   *          Username
   * @param password
   *          password
   * @return Response with SessionBlob containing login information.
   */
@POST
@Path("login")
public Response login(SessionBlob blob, @QueryParam("name") String name, @QueryParam("password") String password) {
    Logger logger = Logger.getLogger(AuthUserActionsRest.class.getName());
    logger.info("=== DEBUG VERSION - Login attempt for user: " + name + " ===");

    User user;
    try {
        user = LoadBalancedCRUDOperations.getEntityWithProperties(Service.PERSISTENCE, "users", User.class, "name", name);
    } catch (TimeoutException e) {
        return Response.status(408).build();
    } catch (NotFoundException e) {
        return Response.status(Response.Status.OK).entity(blob).build();
    }

    if (user != null && BCryptProvider.checkPassword(password, user.getPassword())) {
        // Assign UID and SID
        blob.setUID(user.getId());
        blob.setSID(new RandomSessionIdGenerator().getSessionId());

        // Calculate username hash
        String usernameHash = hashUsername(user.getUserName());
        logger.info("Generated usernameHash: " + usernameHash);

        // Set authToken BEFORE any security operations
        blob.setAuthToken(usernameHash);
        logger.info("AuthToken set in blob: " + blob.getAuthToken());

        // Update Keycloak attributes
        try {
            updateKeycloakUserAttributes(user, usernameHash, new Date());
        } catch (Exception e) {
            logger.warning("Keycloak update failed: " + e.getMessage());
        }

        // Debug JSON BEFORE security provider
        try {
            ObjectMapper testMapper = new ObjectMapper();
            String jsonBefore = testMapper.writeValueAsString(blob);
            logger.info("JSON BEFORE security provider: " + jsonBefore);
        } catch (Exception e) {
            logger.warning("JSON serialization test failed: " + e.getMessage());
        }

        // Apply security provider
        SessionBlob securedBlob = new ShaSecurityProvider().secure(blob);

        // Verify authToken is still there after security
        if (securedBlob.getAuthToken() == null) {
            logger.warning("AuthToken lost during security, restoring...");
            securedBlob.setAuthToken(usernameHash);
        }

        // Debug JSON AFTER security provider
        try {
            ObjectMapper testMapper = new ObjectMapper();
            String jsonAfter = testMapper.writeValueAsString(securedBlob);
            logger.info("JSON AFTER security provider: " + jsonAfter);
        } catch (Exception e) {
            logger.warning("JSON serialization test after security failed: " + e.getMessage());
        }

        logger.info("Final values before return:");
        logger.info("- UID: " + securedBlob.getUID());
        logger.info("- SID: " + securedBlob.getSID());
        logger.info("- token: " + securedBlob.getToken());
        logger.info("- authToken: " + securedBlob.getAuthToken());

        logger.info("Login successful for user: " + name);
        return Response.status(Response.Status.OK).entity(securedBlob).build();
    }

    // Login failed
    logger.info("Login failed for user: " + name);
    return Response.status(Response.Status.OK).entity(blob).build();
}

@GET
@Path("test-serialization")
public Response testSerialization() {
    Logger logger = Logger.getLogger(AuthUserActionsRest.class.getName());
    
    SessionBlob testBlob = new SessionBlob();
    testBlob.setUID(123L);
    testBlob.setSID("test-sid");
    testBlob.setToken("test-token");
    testBlob.setAuthToken("test-auth-token");
    
    logger.info("Test blob authToken: " + testBlob.getAuthToken());
    
    try {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(testBlob);
        logger.info("Test serialization: " + json);
    } catch (Exception e) {
        logger.severe("Test serialization failed: " + e.getMessage());
    }
    
    return Response.status(Response.Status.OK).entity(testBlob).build();
}

/**
 * Hash the username with a pepper for added security
 */
private String hashUsername(String username) {
    if (username == null || username.trim().isEmpty()) {
        return null;
    }
    
    try {
        String pepper = System.getenv("USERNAME_PEPPER");
        if (pepper == null) {
            logger.warning("USERNAME_PEPPER environment variable not set, using default");
            pepper = "default_pepper"; // Fallback, mais pas recommandé en production
        }
        
        String toHash = username.trim() + pepper;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));
        BigInteger number = new BigInteger(1, hashBytes);
        return number.toString(16);
        
    } catch (Exception e) {
        logger.severe("Error hashing username: " + e.getMessage());
        return null;
    }
}

  private void updateKeycloakUserAttributes(User user, String usernameHash, Date lastLoginDate) {
        try {
            String keycloakUrl = properties.getProperty("keycloak.admin.server-url");
            String clientId = properties.getProperty("keycloak.admin.client-id");
            String adminUsername = properties.getProperty("keycloak.admin.username");
            String adminPassword = properties.getProperty("keycloak.admin.password");
            String realm = properties.getProperty("keycloak.admin.realm");

            // Get admin access token
            String accessToken = getAdminAccessToken(keycloakUrl, "master", clientId, adminUsername, adminPassword);

            // Find the user by username
            String userId = getUserIdByUsername(keycloakUrl, realm, accessToken, user.getUserName());

            if (userId != null) {
                // Update user attributes
                updateUserAttributes(keycloakUrl, realm, accessToken, user, userId, usernameHash, lastLoginDate);
            }
        } catch (Exception e) {
            logger.severe("Error updating Keycloak user attributes: " + e.getMessage());
        }
    }


  private String getUserIdByUsername(String keycloakUrl, String realm, String accessToken, String username) throws Exception {
    URL url = new URL(keycloakUrl + "/admin/realms/" + realm + "/users?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);

    int responseCode = conn.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Parse the response to get the user ID
        // This is a simplified example; use a JSON library to parse the response in a real application
        String responseBody = response.toString();
        // Assuming the response is a JSON array and the first user is the desired one
        return responseBody.split("\"id\":\"")[1].split("\"")[0];
    } else {
        throw new Exception("Failed to get user ID: " + responseCode);
    }
  }

  private void updateUserAttributes(String keycloakUrl, String realm, String accessToken,
                                  User user, String userId, String usernameHash, Date lastLoginDate) throws Exception {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    String lastLoginDateStr = lastLoginDate != null ? dateFormat.format(lastLoginDate) : null;

    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode userNode = objectMapper.createObjectNode();
    userNode.put("username", user.getUserName());
    userNode.put("email", user.getEmail());
    userNode.put("firstName", user.getRealName().split(" ")[0]);
    userNode.put("lastName", user.getRealName().split(" ")[1]);
    userNode.put("emailVerified", true);
    userNode.put("enabled", true);

    ObjectNode attributesNode = objectMapper.createObjectNode();
    attributesNode.putArray("authToken").add(usernameHash); // stocke le hash
    if (lastLoginDateStr != null) {
        attributesNode.putArray("lastLoginDate").add(lastLoginDateStr);
    }

    userNode.set("attributes", attributesNode);

    // PUT vers Keycloak
    URL url = new URL(keycloakUrl + "/admin/realms/" + realm + "/users/" + userId);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setDoOutput(true);

    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
        wr.writeBytes(userNode.toString());
        wr.flush();
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_NO_CONTENT && responseCode != HttpURLConnection.HTTP_OK) {
        throw new Exception("Failed to update user attributes: " + responseCode);
    }
    }

  
  /**
   * User logout.
   * 
   * @param blob
   *          SessionBlob
   * @return Response with SessionBlob
   */
  @POST
  @Path("logout")
  public Response logout(SessionBlob blob) {
    // Retrieve the user from the database using the UID from the blob
    User user = LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "users", User.class, blob.getUID());

    if (user != null) {
        // Update Keycloak user attributes
        updateKeycloakUserAttributes(user, null, null);
    }

    blob.setUID(null);
    blob.setSID(null);
    blob.setOrder(new Order());
    blob.getOrderItems().clear();
    
    return Response.status(Response.Status.OK).entity(blob).build();
  }

  /**
   * Checks if user is logged in.
   * 
   * @param blob
   *          Sessionblob
   * @return Response with true if logged in
   */
  @POST
  @Path("isloggedin")
  public Response isLoggedIn(SessionBlob blob) {
    return Response.status(Response.Status.OK).entity(new ShaSecurityProvider().validate(blob))
        .build();
  }

  private static final Logger logger = Logger.getLogger(AuthUserActionsRest.class.getName());
  private static Properties properties;

  static {
      properties = new Properties();
      try (InputStream input = AuthUserActionsRest.class.getClassLoader().getResourceAsStream("application.properties")) {
          if (input == null) {
              logger.severe("Unable to find application.properties");
          }
          properties.load(input);
      } catch (IOException ex) {
          logger.severe("Error loading application.properties: " + ex.getMessage());
      }
  }

    /**
     * Retrieves all users from the database and injects them into Keycloak.
     *
     * @return Response indicating the success or failure of the operation.
     */
    @GET
    @Path("injectUsersToKeycloak")
    public Response injectUsersToKeycloak() {
      
        try {

          ensureRealmExists(
            properties.getProperty("keycloak.admin.server-url"),
            properties.getProperty("keycloak.admin.realm"),
            properties.getProperty("keycloak.admin.client-id"),
            properties.getProperty("keycloak.admin.username"),
            properties.getProperty("keycloak.admin.password")
          );

          List<User> users = LoadBalancedCRUDOperations.getEntities(Service.PERSISTENCE, "users", User.class, -1, -1);

          for (User user : users) {                
              injectUserToKeycloak(user, user.getPassword()); // Assuming getPassword() retrieves the user's password
          }

          return Response.ok("All users injected into Keycloak successfully").build();
        } catch (Exception e) {
            logger.severe("Error injecting users into Keycloak: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error injecting users into Keycloak").build();
        }
    }

    private void injectUserToKeycloak(User user, String password) {
        try {
            String keycloakUrl = properties.getProperty("keycloak.admin.server-url");
            String realm = properties.getProperty("keycloak.admin.realm");
            String clientId = properties.getProperty("keycloak.admin.client-id");
            String adminUsername = properties.getProperty("keycloak.admin.username");
            String adminPassword = properties.getProperty("keycloak.admin.password");

            String accessToken = getAdminAccessToken(keycloakUrl, "master", clientId, adminUsername, adminPassword);

            boolean userNewlyCreated = ensureUserExists(keycloakUrl, realm, clientId, accessToken, user, password);

            if (userNewlyCreated) {   
              String userId = getUserIdByUsername(keycloakUrl, realm, accessToken, user.getUserName());

              logger.info("User ID created in Keycloak: " + userId);
              if (userId != null) {
                  setUserPassword(keycloakUrl, realm, accessToken, userId, password);
                  logger.info("User " + user.getUserName() + " injected successfully into Keycloak.");
              } else {
                  logger.severe("Failed to inject user " + user.getUserName() + " into Keycloak.");
              }
            }
        } catch (Exception e) {
            logger.severe("Error injecting user " + user.getUserName() + " into Keycloak: " + e.getMessage());
        }
    }

    public void ensureRealmExists(String keycloakUrl, String realm, String clientId, String adminUsername, String adminPassword) throws Exception {
        String accessToken = getAdminAccessToken(keycloakUrl, "master", clientId, adminUsername, adminPassword);

        if (!doesRealmExist(keycloakUrl, realm, accessToken)) {
            createAndConfigureRealm(keycloakUrl, realm, accessToken);
        } else {
            logger.info("Realm " + realm + " already exists.");
        }
    }

    private boolean doesRealmExist(String keycloakUrl, String realm, String accessToken) throws Exception {
        URL url = new URL(keycloakUrl + "/admin/realms/" + realm);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        return responseCode == HttpURLConnection.HTTP_OK;
    }

    public void createAndConfigureRealm(String keycloakUrl, String realm, String accessToken) throws Exception {
        createRealm(keycloakUrl, realm, accessToken);
        configureUnmanagedAttributes(keycloakUrl, realm, accessToken);
    }

    private void createRealm(String keycloakUrl, String realm, String accessToken) throws Exception {
        URL url = new URL(keycloakUrl + "/admin/realms");
        String realmJson = String.format("{\"realm\": \"%s\", \"enabled\": true}", realm);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setDoOutput(true);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.writeBytes(realmJson);
            wr.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new Exception("Failed to create realm: " + responseCode);
        }

        logger.info("Realm " + realm + " created successfully.");
    }

    private void configureUnmanagedAttributes(String keycloakUrl, String realm, String accessToken) throws Exception {
    URL url = new URL(keycloakUrl + "/admin/realms/" + realm);

    // Correct JSON payload to enable unmanaged attributes
    String userProfileConfigJson = "{\"userProfileEnabled\": true}";

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setDoOutput(true);

    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
        wr.writeBytes(userProfileConfigJson);
        wr.flush();
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
        // Read the error response from the server
        String errorResponse = readErrorResponse(conn);
        throw new Exception("Failed to configure unmanaged attributes for realm: " + responseCode + " - " + errorResponse);
    }

    logger.info("Unmanaged attributes configured successfully for realm " + realm + ".");
}

private String readErrorResponse(HttpURLConnection conn) throws Exception {
    try (InputStream errorStream = conn.getErrorStream()) {
        if (errorStream != null) {
            return new String(errorStream.readAllBytes());
        } else {
            return "No error response body";
        }
    }
}

    private String getAdminAccessToken(String keycloakUrl, String realm, String clientId, String username, String password) throws Exception {
    URL url = new URL(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token");
    String urlParameters = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                           "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                           "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8) +
                           "&grant_type=password";

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    conn.setDoOutput(true);

    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
        wr.writeBytes(urlParameters);
        wr.flush();
    }

    int responseCode = conn.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> response = objectMapper.readValue(conn.getInputStream(), Map.class);
        return response.get("access_token");
    } else {
        throw new Exception("Failed to get access token: " + responseCode);
    }
}

  public boolean ensureUserExists(String keycloakUrl, String realm, String clientId, String accessToken, User user, String password) throws Exception {
        
        if (!doesUserExist(keycloakUrl, realm, accessToken, user.getUserName())) {
            createUser(keycloakUrl, realm, accessToken, user);
            // String userId = getUserIdByUsername(keycloakUrl, realm, accessToken, user.getUserName());
            // setUserPassword(keycloakUrl, realm, accessToken, userId, password);
            return true; // User was newly created
        } else {
            logger.info("User " + user.getUserName() + " already exists.");
            return false; // User already exists, no need to set password again
        }
    }

  private static boolean doesUserExist(String keycloakUrl, String realm, String accessToken, String username) throws Exception {
        URL url = new URL(keycloakUrl + "/admin/realms/" + realm + "/users?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> users = objectMapper.readValue(conn.getInputStream(), List.class);
            return !users.isEmpty();
        } else {
            throw new Exception("Failed to check user existence: " + responseCode);
        }
    }

private void createUser(String keycloakUrl, String realm, String accessToken, User user) throws Exception {
    URL url = new URL(keycloakUrl + "/admin/realms/" + realm + "/users");
    String userJson = String.format(
                "{\"username\": \"%s\", \"email\": \"%s\", \"firstName\": \"%s\", \"lastName\": \"%s\", \"emailVerified\": %b, \"enabled\": %b}",
                user.getUserName(), user.getEmail(), user.getRealName().substring(0,user.getRealName().indexOf(' ')), user.getRealName().substring(user.getRealName().indexOf(' ')+1), true, true);

    logger.info("Sending request to Keycloak to add user: " + url);
    logger.info("User JSON: " + userJson);

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setDoOutput(true);

    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
        wr.writeBytes(userJson);
        wr.flush();
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_CREATED) {
        throw new Exception("Failed to create user: " + responseCode);
    } 
  }

  private void setUserPassword(String keycloakUrl, String realm, String accessToken, String userId, String password) throws Exception {
    URL url = new URL(keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password");
    String passwordJson = String.format(
        "{\"type\": \"password\", \"value\": \"%s\", \"temporary\": false}", password);

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("PUT");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + accessToken);
    conn.setDoOutput(true);

    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
        wr.writeBytes(passwordJson);
        wr.flush();
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
      logger.severe("Failed to set user password for user ID " + userId + ": " + responseCode);
      throw new Exception("Failed to set user password: " + responseCode);
    }
    else {
      logger.info("Password set successfully for user ID " + userId);
    } 
  }
  }
