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
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import tools.descartes.teastore.registryclient.Service;
import tools.descartes.teastore.registryclient.loadbalancers.LoadBalancerTimeoutException;
import tools.descartes.teastore.registryclient.rest.LoadBalancedCRUDOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedImageOperations;
import tools.descartes.teastore.registryclient.rest.LoadBalancedStoreOperations;
import tools.descartes.teastore.entities.Category;
import tools.descartes.teastore.entities.ImageSizePreset;
import tools.descartes.teastore.entities.message.SessionBlob;

/**
 * Servlet for the sign-up page and its POST action. TeaStore ships no
 * self-service registration by default (only DataGenerator-seeded
 * accounts) - added as part of the PRIAM integration so there is a real
 * user-creation point to wire register_data_subject()/forced consent into
 * (Docs/PRIAM-INTEGRATION-PLAYBOOK.md 4bis).
 */
@WebServlet("/register")
public class RegisterServlet extends AbstractUIServlet {

  private static final long serialVersionUID = 1L;

  /**
   * PRIAM-Frontend's consent page, read from the environment like every
   * other PRIAM_ or CUSTOM_ variable in this integration (empty = feature
   * disabled, playbook 4bis).
   */
  private static final String PRIAM_FRONTEND_URL = System.getenv("PRIAM_FRONTEND_URL");

  @Override
  protected void handleGETRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException, LoadBalancerTimeoutException {
    checkforCookie(request, response);
    request.setAttribute("CategoryList",
        LoadBalancedCRUDOperations.getEntities(Service.PERSISTENCE, "categories", Category.class, -1, -1));
    request.setAttribute("storeIcon",
        LoadBalancedImageOperations.getWebImage("icon", ImageSizePreset.ICON.getSize()));
    request.setAttribute("title", "TeaStore Sign Up");
    request.setAttribute("login", LoadBalancedStoreOperations.isLoggedIn(getSessionBlob(request)));
    request.getRequestDispatcher("WEB-INF/pages/register.jsp").forward(request, response);
  }

  @Override
  protected void handlePOSTRequest(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException, LoadBalancerTimeoutException {
    String name = request.getParameter("username");
    String password = request.getParameter("password");
    if (name == null || name.isEmpty() || password == null || password.isEmpty()) {
      redirect("/register", response, ERRORMESSAGECOOKIE, "Username and password are required!");
      return;
    }
    SessionBlob blob = LoadBalancedStoreOperations.register(getSessionBlob(request), name, password,
        request.getParameter("email"), request.getParameter("realName"));
    if (blob == null || blob.getSID() == null) {
      redirect("/register", response, ERRORMESSAGECOOKIE, "Username already taken!");
      return;
    }
    saveSessionBlob(blob, response);
    // Forced consent (playbook 4bis): redirect to PRIAM's consent page for
    // this brand-new subject instead of the usual post-signup destination.
    // Only ever fires once - priamConsentRequired becomes false as soon as
    // a decision exists (4bis "the redirect happens only once by
    // construction").
    if (blob.isPriamConsentRequired() && PRIAM_FRONTEND_URL != null && !PRIAM_FRONTEND_URL.isEmpty()) {
      response.sendRedirect(PRIAM_FRONTEND_URL + "/consent");
      return;
    }
    redirect("/", response, MESSAGECOOKIE, SUCESSLOGIN);
  }

}
