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
package tools.descartes.teastore.webui.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.rest.LoadBalancedCRUDOperations;
import tools.descartes.teastore.entities.Order;
import tools.descartes.teastore.entities.User;

/**
 * PRIAM Provider bridge (Docs/PRIAM-INTEGRATION-PLAYBOOK.md 2). Mounted on
 * bare {@code /api} (this servlet's context path is
 * {@code tools.descartes.teastore.webui}, the segment
 * {@code CUSTOM_PROVIDER_URL} points at directly - PRIAM-Gateway prefixes
 * it back on for every {@code /provider/**} call, see the root
 * docker-compose.yml comment on {@code CUSTOM_PROVIDER_URL}). No
 * authentication - called only machine-to-machine by PRIAM.
 */
@WebServlet("/api/*")
public class PriamProviderServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final ObjectMapper JSON = new ObjectMapper();

  // Fields returned by dataAccessRight/dataValue (playbook 1 point 4 names).
  private static final List<String> USER_ACCESS_FIELDS = List.of("userName", "email", "realName");
  private static final List<String> ORDER_ACCESS_FIELDS = List.of("id", "addressName", "address1", "address2",
      "creditCardCompany", "creditCardNumber", "creditCardExpiryDate");
  // Fields rectification/erasure may write. Narrower than the access lists
  // above on purpose: "userName" is also this integration's PRIAM idRef
  // (Databases/db_insertion_script.sql), so rectifying it would desync the
  // PRIAM data_subject from the real account - excluded here, still
  // readable via dataAccessRight/dataValue. "Order.id" is the row selector
  // (primaryKeys), not a value of its own to rectify/erase.
  private static final List<String> USER_MUTABLE_FIELDS = List.of("email", "realName");
  private static final List<String> ORDER_MUTABLE_FIELDS = List.of("addressName", "address1", "address2",
      "creditCardCompany", "creditCardNumber", "creditCardExpiryDate");

  private static User findUserByIdRef(String idRef) {
    try {
      return LoadBalancedCRUDOperations.getEntityWithProperties(Service.PERSISTENCE, "users", User.class,
          "name", idRef);
    } catch (Exception e) {
      return null;
    }
  }

  private static List<Order> findOrders(long userId) {
    try {
      List<Order> orders = new ArrayList<>(
          LoadBalancedCRUDOperations.getEntities(Service.PERSISTENCE, "orders", Order.class, "user", userId, -1, -1));
      orders.sort(Comparator.comparingLong(Order::getId));
      return orders;
    } catch (Exception e) {
      return List.of();
    }
  }

  private static Map<String, String> pickUserAttributes(User user, List<String> attributes) {
    List<String> allowed = attributes.isEmpty() ? USER_ACCESS_FIELDS : attributes;
    Map<String, String> record = new LinkedHashMap<>();
    if (allowed.contains("userName")) {
      record.put("userName", user.getUserName());
    }
    if (allowed.contains("email")) {
      record.put("email", user.getEmail());
    }
    if (allowed.contains("realName")) {
      record.put("realName", user.getRealName());
    }
    return record;
  }

  private static Map<String, String> pickOrderAttributes(Order order, List<String> attributes) {
    List<String> allowed = attributes.isEmpty() ? ORDER_ACCESS_FIELDS : attributes;
    Map<String, String> record = new LinkedHashMap<>();
    if (allowed.contains("id")) {
      record.put("id", String.valueOf(order.getId()));
    }
    if (allowed.contains("addressName")) {
      record.put("addressName", order.getAddressName());
    }
    if (allowed.contains("address1")) {
      record.put("address1", order.getAddress1());
    }
    if (allowed.contains("address2")) {
      record.put("address2", order.getAddress2());
    }
    if (allowed.contains("creditCardCompany")) {
      record.put("creditCardCompany", order.getCreditCardCompany());
    }
    if (allowed.contains("creditCardNumber")) {
      record.put("creditCardNumber", order.getCreditCardNumber());
    }
    if (allowed.contains("creditCardExpiryDate")) {
      record.put("creditCardExpiryDate", order.getCreditCardExpiryDate());
    }
    return record;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (!"/dataAccessRight".equals(request.getPathInfo())) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    String idRef = request.getParameter("idRef");
    String dataTypeName = request.getParameter("dataTypeName");
    String attributesParam = request.getParameter("attributes");
    List<String> attributes = attributesParam == null || attributesParam.isEmpty() ? List.of()
        : Arrays.asList(attributesParam.split(","));

    List<Map<String, String>> records = new ArrayList<>();
    if ("User".equals(dataTypeName)) {
      User user = findUserByIdRef(idRef);
      if (user != null) {
        records.add(pickUserAttributes(user, attributes));
      }
    } else if ("Order".equals(dataTypeName)) {
      User user = findUserByIdRef(idRef);
      if (user != null) {
        for (Order order : findOrders(user.getId())) {
          records.add(pickOrderAttributes(order, attributes));
        }
      }
    }
    // GET /api/dataAccessRight must always answer with a JSON array (2),
    // even empty - never a bare object.
    writeJson(response, records);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ProviderRequestBody body = JSON.readValue(request.getReader(), ProviderRequestBody.class);
    Map<String, String> primaryKeys = body.primaryKeys == null ? Map.of() : body.primaryKeys;
    String path = request.getPathInfo();
    if ("/rectification".equals(path)) {
      rectify(response, body.idRef, body.dataTypeName, body.dataName, body.newValue, primaryKeys);
    } else if ("/erasure".equals(path)) {
      rectify(response, body.idRef, body.dataTypeName, body.dataName, "", primaryKeys);
    } else if ("/dataValue".equals(path)) {
      dataValue(response, body.idRef, body.dataName, primaryKeys);
    } else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void rectify(HttpServletResponse response, String idRef, String dataTypeName, String dataName,
      String newValue, Map<String, String> primaryKeys) throws IOException {
    if ("User".equals(dataTypeName) && USER_MUTABLE_FIELDS.contains(dataName)) {
      User user = findUserByIdRef(idRef);
      if (user == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      if ("email".equals(dataName)) {
        user.setEmail(newValue);
      } else if ("realName".equals(dataName)) {
        user.setRealName(newValue);
      }
      boolean ok = LoadBalancedCRUDOperations.sendEntityForUpdate(Service.PERSISTENCE, "users", User.class,
          user.getId(), user);
      writeResult(response, ok);
    } else if ("Order".equals(dataTypeName) && ORDER_MUTABLE_FIELDS.contains(dataName)) {
      String orderIdStr = primaryKeys.get("id");
      if (orderIdStr == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      long orderId = Long.parseLong(orderIdStr);
      Order order = LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "orders", Order.class, orderId);
      if (order == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      setOrderField(order, dataName, newValue);
      boolean ok = LoadBalancedCRUDOperations.sendEntityForUpdate(Service.PERSISTENCE, "orders", Order.class,
          orderId, order);
      writeResult(response, ok);
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  private static void setOrderField(Order order, String dataName, String value) {
    switch (dataName) {
      case "addressName":
        order.setAddressName(value);
        break;
      case "address1":
        order.setAddress1(value);
        break;
      case "address2":
        order.setAddress2(value);
        break;
      case "creditCardCompany":
        order.setCreditCardCompany(value);
        break;
      case "creditCardNumber":
        order.setCreditCardNumber(value);
        break;
      case "creditCardExpiryDate":
        order.setCreditCardExpiryDate(value);
        break;
      default:
        break;
    }
  }

  private void dataValue(HttpServletResponse response, String idRef, String dataName, Map<String, String> primaryKeys)
      throws IOException {
    // 2/8.2.f: no dataTypeName in this request - inferred from dataName's
    // whitelist (the two lists are disjoint by name).
    if (USER_ACCESS_FIELDS.contains(dataName)) {
      User user = findUserByIdRef(idRef);
      if (user == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      writeJson(response, Map.of("value", pickUserAttributes(user, List.of(dataName)).getOrDefault(dataName, "")));
    } else if (ORDER_ACCESS_FIELDS.contains(dataName)) {
      String orderIdStr = primaryKeys.get("id");
      if (orderIdStr == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      Order order = LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "orders", Order.class,
          Long.parseLong(orderIdStr));
      if (order == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      writeJson(response, Map.of("value", pickOrderAttributes(order, List.of(dataName)).getOrDefault(dataName, "")));
    } else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void writeResult(HttpServletResponse response, boolean success) throws IOException {
    if (success) {
      writeJson(response, Map.of("success", true));
    } else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void writeJson(HttpServletResponse response, Object payload) throws IOException {
    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_OK);
    JSON.writeValue(response.getWriter(), payload);
  }

  /**
   * Shared request body shape for rectification/erasure/dataValue - each
   * endpoint only populates the fields it actually sends (2: dataValue
   * omits dataTypeName).
   */
  private static final class ProviderRequestBody {
    public String idRef;
    public String dataTypeName;
    public String dataName;
    public String newValue;
    public Map<String, String> primaryKeys;
  }
}
