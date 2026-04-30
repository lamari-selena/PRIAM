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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.loadbalancers.LoadBalancerTimeoutException;
import tools.descartes.teastore.registryclient.rest.LoadBalancedCRUDOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedImageOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedRecommenderOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedStoreOperations;
import tools.descartes.teastore.entities.Category;
import tools.descartes.teastore.entities.ImageSizePreset;
import tools.descartes.teastore.entities.OrderItem;
import tools.descartes.teastore.entities.Product;
import tools.descartes.teastore.entities.message.SessionBlob;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import java.util.logging.Logger;
import java.util.logging.Level;
/**
 * Servlet implementation for the web view of "Cart".
 * 
 * @author Andre Bauer
 */
@WebServlet("/cart")
public class CartServlet extends AbstractUIServlet {
  private static final long serialVersionUID = 1L;

/*///////////////////////////////////////////////////////
  private HttpClient client = HttpClient.newHttpClient();
  private HttpRequest builderHttp(Long userid, String processingId) {
    // Utilisez le nom de la méthode et de la classe dans l'URL
    String url = "http://172.17.0.1:8090/cdp/api/decision/" + processingId + "?idRefList=" + userid.toString();
    return HttpRequest.newBuilder().uri(URI.create(url)).build();
  }

  // Cette méthode envoie la requête HTTP et affiche la réponse
  private HttpResponse<String> sendReq(Long userid, String processingId) throws InterruptedException, IOException {
    HttpResponse<String> response = client.send(builderHttp(userid, processingId), HttpResponse.BodyHandlers.ofString());
    System.out.println("HTTP response status: " + response.statusCode());
    System.out.println("HTTP response body: " + response.body());
    return response;
  }

  // Cette méthode récupère le consentement et utilise le nom de la méthode et de la classe
  private boolean getConsent(Long userid, String processingId) throws InterruptedException, IOException {
    long startTime = System.currentTimeMillis();
    System.out.println("startTime getConsent" + startTime);

    HttpResponse<String> response = sendReq(userid, processingId);

    // Vérifiez si la réponse est correcte et non vide
    if (response.body() == null || response.body().isEmpty()) {
      System.out.println("La réponse est vide ou nulle !");
      return false;
    }

    try {
      JSONObject myObject = new JSONObject(response.body());
      boolean result = myObject.getBoolean(userid.toString());
      System.out.println("time of getConsent ---->" + (System.currentTimeMillis()- startTime));
      return result;
    } catch (Exception e) {
      System.err.println("Erreur lors du traitement de la réponse JSON : " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }*/
///////////////////////////////////////////////////////
  /**
   * @see HttpServlet#HttpServlet()
   */
  public CartServlet() {
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void handleGETRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException, LoadBalancerTimeoutException {
    checkforCookie(request, response);
    SessionBlob blob = getSessionBlob(request);

    List<OrderItem> orderItems = blob.getOrderItems();
    ArrayList<Long> ids = new ArrayList<Long>();
    for (OrderItem orderItem : orderItems) {
      ids.add(orderItem.getProductId());
    }

    HashMap<Long, Product> products = new HashMap<Long, Product>();
    for (Long id : ids) {
      Product product = LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "products",
          Product.class, id);
      products.put(product.getId(), product);
    }


//////
   String authHeader = request.getHeader("authtoken");
String usernameHeader = request.getHeader("x-username");

Logger logger = Logger.getLogger(CartServlet.class.getName());

// DEBUG des headers
logger.info("Auth headers - x-username: " + usernameHeader + ", authtoken: " + 
    (authHeader != null ? authHeader.substring(0, 8) + "..." : "null"));

// Peupler le SessionBlob si nécessaire
if (authHeader != null && blob.getAuthToken() == null) {
    blob.setAuthToken(authHeader);
    logger.info("AuthToken set in SessionBlob");
}

// UID based on username header
if (usernameHeader != null && blob.getUID() == null) {
    if ("user0".equals(usernameHeader)) {
        blob.setUID(507L);
    } else if ("user1".equals(usernameHeader)) {
        blob.setUID(508L);
    } else {
        blob.setUID(507L); // default
    }
    logger.info("UID set to: " + blob.getUID() + " for user: " + usernameHeader);
    
    // save the updated blob
    request.getSession().setAttribute("sessionBlob", blob);
}

// Exctract logging
Long uid = blob.getUID();
String authToken = blob.getAuthToken();

logger.info("SessionBlob final - UID: " + uid + ", AuthToken: " + 
    (authToken != null ? authToken.substring(0, 8) + "..." : "null"));

// Add JSP attributes
request.setAttribute("uid", uid);
request.setAttribute("maskedAuthToken", authToken != null ? authToken.substring(0, 8) + "..." : "null");
//////
    
    request.setAttribute("storeIcon",
        LoadBalancedImageOperations.getWebImage("icon", ImageSizePreset.ICON.getSize()));
    request.setAttribute("title", "TeaStore Cart");
    request.setAttribute("CategoryList", LoadBalancedCRUDOperations.getEntities(Service.PERSISTENCE,
        "categories", Category.class, -1, -1));
    request.setAttribute("OrderItems", orderItems);
    request.setAttribute("Products", products);
    request.setAttribute("login", LoadBalancedStoreOperations.isLoggedIn(getSessionBlob(request)));

    List<Long> productIds = LoadBalancedRecommenderOperations
        .getRecommendations(blob.getOrderItems(), blob.getUID());
    List<Product> ads = new LinkedList<Product>();
    for (Long productId : productIds) {
      ads.add(LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "products", Product.class,
          productId));
    }

    if (ads.size() > 3) {
      ads.subList(3, ads.size()).clear();
    }
    request.setAttribute("Advertisment", ads);

    request.setAttribute("productImages", LoadBalancedImageOperations.getProductPreviewImages(ads));

    request.getRequestDispatcher("WEB-INF/pages/cart.jsp").forward(request, response);

  }
  /*@Override
  protected void handleGETRequest(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException, LoadBalancerTimeoutException {

    checkforCookie(request, response);
    SessionBlob blob = getSessionBlob(request);
    Long userId = blob.getUID();

    String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
    String className = new Object() {}.getClass().getEnclosingClass().getSimpleName();
    String processingId = className + "." + methodName;
    System.out.println("Processing ID ---------------> : " + processingId);
    boolean canUse = false;
    try {
      canUse = getConsent(userId, processingId);
    } catch (InterruptedException | IOException ex) {
      System.err.println("Erreur lors de la vérification du consentement : " + ex.getMessage());
      ex.printStackTrace();
    }

    if (!canUse) {
      // Si pas de consentement → redirection ou affichage d’un message
      request.setAttribute("title", "Accès refusé");
      request.setAttribute("error", "Vous devez donner votre consentement pour accéder à votre panier.");
      request.getRequestDispatcher("WEB-INF/pages/error.jsp").forward(request, response);
      return;
    }

    // onsentement accordé → exécuter le code habituel
    List<OrderItem> orderItems = blob.getOrderItems();
    ArrayList<Long> ids = new ArrayList<>();
    for (OrderItem orderItem : orderItems) {
      ids.add(orderItem.getProductId());
    }

    HashMap<Long, Product> products = new HashMap<>();
    for (Long id : ids) {
      Product product = LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "products", Product.class, id);
      products.put(product.getId(), product);
    }

    request.setAttribute("storeIcon", LoadBalancedImageOperations.getWebImage("icon", ImageSizePreset.ICON.getSize()));
    request.setAttribute("title", "TeaStore Cart");
    request.setAttribute("CategoryList", LoadBalancedCRUDOperations.getEntities(Service.PERSISTENCE, "categories", Category.class, -1, -1));
    request.setAttribute("OrderItems", orderItems);
    request.setAttribute("Products", products);
    request.setAttribute("login", LoadBalancedStoreOperations.isLoggedIn(blob));

    List<Long> productIds = LoadBalancedRecommenderOperations.getRecommendations(blob.getOrderItems(), userId);
    List<Product> ads = new LinkedList<>();
    for (Long productId : productIds) {
      ads.add(LoadBalancedCRUDOperations.getEntity(Service.PERSISTENCE, "products", Product.class, productId));
    }

    if (ads.size() > 3) {
      ads.subList(3, ads.size()).clear();
    }

    request.setAttribute("Advertisment", ads);
    request.setAttribute("productImages", LoadBalancedImageOperations.getProductPreviewImages(ads));

    request.getRequestDispatcher("WEB-INF/pages/cart.jsp").forward(request, response);
  }*/

}

