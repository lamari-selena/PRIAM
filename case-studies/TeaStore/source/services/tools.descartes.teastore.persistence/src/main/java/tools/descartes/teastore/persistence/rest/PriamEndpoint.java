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
package tools.descartes.teastore.persistence.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import tools.descartes.teastore.persistence.domain.OrderRepository;
import tools.descartes.teastore.persistence.domain.UserRepository;
import tools.descartes.teastore.entities.Order;
import tools.descartes.teastore.entities.User;

/**
 * PRIAM Provider endpoints for the TeaStore persistence service.
 *
 * Exposes the three Provider endpoints required by PRIAM's Right Management service:
 *   GET  /rest/priam/dataAccessRight  — Right of Access (GDPR Art. 15)
 *   POST /rest/priam/rectification    — Right to Rectification (GDPR Art. 16)
 *   POST /rest/priam/erasure          — Right to Erasure (GDPR Art. 17)
 *
 * Supported data types: User (userName, realName, email),
 *   Order (addressName, address1, address2, creditCardCompany, creditCardNumber).
 * The idRef parameter maps to the User.id (long).
 *
 * Auto-discovered by Jersey package scan of tools.descartes.teastore.persistence.rest.
 */
@Path("priam")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PriamEndpoint {

    private static final Logger LOG = Logger.getLogger(PriamEndpoint.class.getName());

    private static final Map<String, List<String>> ALLOWED_FIELDS = new HashMap<>();

    static {
        ALLOWED_FIELDS.put("User",  Arrays.asList("userName", "realName", "email"));
        ALLOWED_FIELDS.put("Order", Arrays.asList(
                "addressName", "address1", "address2",
                "creditCardCompany", "creditCardNumber"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Response badField(String field) {
        return Response.status(Status.BAD_REQUEST)
                .entity(Map.of("error", "Field not allowed: " + field))
                .build();
    }

    private Response validateFields(String dataTypeName, List<String> fields) {
        List<String> allowed = ALLOWED_FIELDS.get(dataTypeName);
        if (allowed == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(Map.of("error", "Unknown dataTypeName: " + dataTypeName))
                    .build();
        }
        for (String f : fields) {
            if (!allowed.contains(f)) {
                return badField(f);
            }
        }
        return null;
    }

    private Object invoke(Object entity, String getterPrefix, String fieldName) {
        try {
            String name = getterPrefix + Character.toUpperCase(fieldName.charAt(0))
                    + fieldName.substring(1);
            return entity.getClass().getMethod(name).invoke(entity);
        } catch (Exception e) {
            LOG.warning("Cannot invoke getter for " + fieldName);
            return null;
        }
    }

    private void invokeSetter(Object entity, String fieldName, String value) {
        try {
            String name = "set" + Character.toUpperCase(fieldName.charAt(0))
                    + fieldName.substring(1);
            entity.getClass().getMethod(name, String.class).invoke(entity, value);
        } catch (Exception e) {
            LOG.warning("Cannot invoke setter for " + fieldName);
        }
    }

    // ── GET /rest/priam/dataAccessRight ───────────────────────────────────────

    @GET
    @Path("dataAccessRight")
    public Response dataAccessRight(
            @QueryParam("idRef") String idRef,
            @QueryParam("dataTypeName") String dataTypeName,
            @QueryParam("attributes") String attributesParam) {

        if (idRef == null || dataTypeName == null || attributesParam == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(Map.of("error", "idRef, dataTypeName and attributes are required"))
                    .build();
        }

        List<String> attrs = Arrays.stream(attributesParam.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        Response validation = validateFields(dataTypeName, attrs);
        if (validation != null) {
            return validation;
        }

        long userId = Long.parseLong(idRef);

        if ("User".equals(dataTypeName)) {
            User user = UserRepository.REPOSITORY.getEntity(userId);
            if (user == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            Map<String, Object> row = new HashMap<>();
            for (String attr : attrs) {
                row.put(attr, invoke(user, "get", attr));
            }
            return Response.ok(List.of(row)).build();
        }

        if ("Order".equals(dataTypeName)) {
            List<Order> allOrders = OrderRepository.REPOSITORY.getAllEntities(0, Integer.MAX_VALUE);
            List<Map<String, Object>> results = allOrders.stream()
                    .filter(o -> o.getUserId() == userId)
                    .map(o -> {
                        Map<String, Object> row = new HashMap<>();
                        row.put("orderId", o.getId());
                        for (String attr : attrs) {
                            row.put(attr, invoke(o, "get", attr));
                        }
                        return row;
                    })
                    .collect(Collectors.toList());
            return Response.ok(results).build();
        }

        return Response.status(Status.BAD_REQUEST).build();
    }

    // ── POST /rest/priam/rectification ────────────────────────────────────────

    @POST
    @Path("rectification")
    public Response rectification(Map<String, Object> body) {
        if (body == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        String idRef       = (String) body.get("idRef");
        String dataTypeName = (String) body.get("dataTypeName");
        String dataName    = (String) body.get("dataName");
        String newValue    = (String) body.get("newValue");

        if (idRef == null || dataTypeName == null || dataName == null || newValue == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(Map.of("error", "idRef, dataTypeName, dataName and newValue are required"))
                    .build();
        }

        Response validation = validateFields(dataTypeName, List.of(dataName));
        if (validation != null) {
            return validation;
        }

        long userId = Long.parseLong(idRef);

        if ("User".equals(dataTypeName)) {
            User user = UserRepository.REPOSITORY.getEntity(userId);
            if (user == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            invokeSetter(user, dataName, newValue);
            UserRepository.REPOSITORY.updateEntity(userId, user);
            LOG.info("Rectification: user=" + idRef + " model=User field=" + dataName);
            return Response.ok().build();
        }

        if ("Order".equals(dataTypeName)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> primaryKeys = (Map<String, Object>) body.getOrDefault("primaryKeys", new HashMap<>());
            long orderId = Long.parseLong(String.valueOf(primaryKeys.getOrDefault("id", "0")));
            Order order = OrderRepository.REPOSITORY.getEntity(orderId);
            if (order == null || order.getUserId() != userId) {
                return Response.status(Status.NOT_FOUND).build();
            }
            invokeSetter(order, dataName, newValue);
            OrderRepository.REPOSITORY.updateEntity(orderId, order);
            LOG.info("Rectification: user=" + idRef + " model=Order field=" + dataName);
            return Response.ok().build();
        }

        return Response.status(Status.BAD_REQUEST).build();
    }

    // ── POST /rest/priam/erasure ──────────────────────────────────────────────

    @POST
    @Path("erasure")
    public Response erasure(Map<String, Object> body) {
        if (body == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        String idRef        = (String) body.get("idRef");
        String dataTypeName = (String) body.get("dataTypeName");
        String dataName     = (String) body.get("dataName");

        if (idRef == null || dataTypeName == null || dataName == null) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(Map.of("error", "idRef, dataTypeName and dataName are required"))
                    .build();
        }

        Response validation = validateFields(dataTypeName, List.of(dataName));
        if (validation != null) {
            return validation;
        }

        long userId = Long.parseLong(idRef);

        if ("User".equals(dataTypeName)) {
            User user = UserRepository.REPOSITORY.getEntity(userId);
            if (user == null) {
                return Response.status(Status.NOT_FOUND).build();
            }
            invokeSetter(user, dataName, null);
            UserRepository.REPOSITORY.updateEntity(userId, user);
            LOG.info("Erasure: user=" + idRef + " model=User field=" + dataName);
            return Response.ok().build();
        }

        if ("Order".equals(dataTypeName)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> primaryKeys = (Map<String, Object>) body.getOrDefault("primaryKeys", new HashMap<>());
            long orderId = Long.parseLong(String.valueOf(primaryKeys.getOrDefault("id", "0")));
            Order order = OrderRepository.REPOSITORY.getEntity(orderId);
            if (order == null || order.getUserId() != userId) {
                return Response.status(Status.NOT_FOUND).build();
            }
            invokeSetter(order, dataName, null);
            OrderRepository.REPOSITORY.updateEntity(orderId, order);
            LOG.info("Erasure: user=" + idRef + " model=Order field=" + dataName);
            return Response.ok().build();
        }

        return Response.status(Status.BAD_REQUEST).build();
    }
}