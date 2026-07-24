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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import tools.descartes.teastore.auth.priam.PriamClient;
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
    // 4bis: report_processed_data at every point a personal record is
    // created, not just sign-up - an Order is a one-to-many-per-subject
    // record (8.1.b). Backgrounded: pure bookkeeping, must not add
    // latency to the checkout response.
    if (blob.getUserName() != null) {
      String idRef = blob.getUserName();
      new Thread(() -> PriamClient.reportProcessedData(idRef, PriamClient.ORDER_DATA_IDS)).start();
    }
    blob = new ShaSecurityProvider().secure(blob);
    return Response.status(Response.Status.OK).entity(blob).build();
  }

  /**
   * User registration. TeaStore ships no self-service sign-up by default
   * (only DataGenerator-seeded accounts) - this endpoint was added as part
   * of the PRIAM integration so there is a real user-creation point to wire
   * register_data_subject()/forced consent into (playbook 4bis).
   *
   * @param blob
   *          SessionBlob
   * @param name
   *          Username (also used as the PRIAM idRef)
   * @param password
   *          plaintext password
   * @param email
   *          email
   * @param realName
   *          real name
   * @return Response with SessionBlob containing login information, or 409
   *         if the username is already taken.
   */
  @POST
  @Path("register")
  public Response register(SessionBlob blob, @QueryParam("name") String name,
      @QueryParam("password") String password, @QueryParam("email") String email,
      @QueryParam("realName") String realName) {
    if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    User user = new User();
    user.setUserName(name);
    user.setPassword(BCryptProvider.hashPassword(password));
    user.setEmail(email);
    user.setRealName(realName);
    long userId;
    try {
      userId = LoadBalancedCRUDOperations.sendEntityForCreation(Service.PERSISTENCE, "users", User.class, user);
    } catch (LoadBalancerTimeoutException e) {
      return Response.status(408).build();
    } catch (NotFoundException e) {
      return Response.status(404).build();
    }
    if (userId <= 0) {
      // Duplicate username (UserEndpoint.createEntity catches the SQL
      // constraint violation and returns -1L) or maintenance mode.
      return Response.status(Response.Status.CONFLICT).build();
    }
    blob.setUID(userId);
    blob.setUserName(name);
    blob.setSID(new RandomSessionIdGenerator().getSessionId());

    // 4bis/8.6: register_data_subject MUST commit before any call that
    // resolves idRef -> dataSubjectId internally (hasPendingConsentDecision/
    // reportProcessedData below use the Consent/Data-service, both of which
    // look the subject up by idRef) - sequenced synchronously here, not
    // fire-and-forget, precisely because this response's own redirect
    // decision (priamConsentRequired) depends on its result.
    PriamClient.registerDataSubject(name);
    blob.setPriamConsentRequired(PriamClient.hasPendingConsentDecision(name, PriamClient.OPTIONAL_PROCESSING));

    // Bookkeeping + Keycloak provisioning: backgrounded, not needed for
    // this response and slower (Keycloak admin token round-trip).
    new Thread(() -> {
      PriamClient.reportProcessedData(name, PriamClient.USER_DATA_IDS);
      PriamClient.provisionKeycloakUser(name, email, realName, password);
    }).start();

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
  public Response login(SessionBlob blob, @QueryParam("name") String name,
      @QueryParam("password") String password) {
    User user;
    try {
      user = LoadBalancedCRUDOperations.getEntityWithProperties(Service.PERSISTENCE, "users",
          User.class, "name", name);
    } catch (TimeoutException e) {
      return Response.status(408).build();
    } catch (NotFoundException e) {
      return Response.status(Response.Status.OK).entity(blob).build();
    }

    if (user != null && BCryptProvider.checkPassword(password, user.getPassword())
    ) {
      blob.setUID(user.getId());
      blob.setUserName(user.getUserName());
      blob.setSID(new RandomSessionIdGenerator().getSessionId());
      // 4bis: idempotent upsert, self-heals any account missed by the
      // one-off backfill script; also required before the
      // hasPendingConsentDecision lookup below can resolve this idRef
      // (8.6 race), so sequenced synchronously rather than backgrounded.
      PriamClient.registerDataSubject(user.getUserName());
      blob.setPriamConsentRequired(
          PriamClient.hasPendingConsentDecision(user.getUserName(), PriamClient.OPTIONAL_PROCESSING));
      blob = new ShaSecurityProvider().secure(blob);
      return Response.status(Response.Status.OK).entity(blob).build();
    }
    return Response.status(Response.Status.OK).entity(blob).build();
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

}
