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

import tools.descartes.teastore.registryclient.loadbalancers.LoadBalancerTimeoutException;
import tools.descartes.teastore.registryclient.rest.LoadBalancedStoreOperations;
import tools.descartes.teastore.entities.message.SessionBlob;

/**
 * Servlet for handling the login actions.
 * 
 * @author Andre Bauer
 */
@WebServlet("/loginAction")
public class LoginActionServlet extends AbstractUIServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * PRIAM-Frontend's consent page (playbook 4bis) - empty = feature disabled.
	 */
	private static final String PRIAM_FRONTEND_URL = System.getenv("PRIAM_FRONTEND_URL");

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LoginActionServlet() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void handleGETRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, LoadBalancerTimeoutException {

		redirect("/", response);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void handlePOSTRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, LoadBalancerTimeoutException {
		boolean login = false;
		if (request.getParameter("username") != null && request.getParameter("password") != null) {
			SessionBlob blob = LoadBalancedStoreOperations.login(getSessionBlob(request),
					request.getParameter("username"), request.getParameter("password"));
			login = (blob != null && blob.getSID() != null);

			if (login) {
				saveSessionBlob(blob, response);
				// Forced consent (playbook 4bis): fires at most once per
				// subject - priamConsentRequired becomes false as soon as a
				// decision exists, so this never loops on a later login.
				if (blob.isPriamConsentRequired() && PRIAM_FRONTEND_URL != null && !PRIAM_FRONTEND_URL.isEmpty()) {
					response.sendRedirect(PRIAM_FRONTEND_URL + "/consent");
					return;
				}
				if (request.getParameter("referer") != null
						&& request.getParameter("referer").contains("tools.descartes.teastore.webui/cart")) {
					redirect("/cart", response, MESSAGECOOKIE, SUCESSLOGIN);
				} else {
					redirect("/", response, MESSAGECOOKIE, SUCESSLOGIN);
				}

			} else {
				redirect("/login", response, ERRORMESSAGECOOKIE, WRONGCREDENTIALS);
			}

		} else if (request.getParameter("logout") != null) {
			SessionBlob blob = LoadBalancedStoreOperations.logout(getSessionBlob(request));
			saveSessionBlob(blob, response);
			destroySessionBlob(blob, response);
			redirect("/", response, MESSAGECOOKIE, SUCESSLOGOUT);

		} else {
			handleGETRequest(request, response);
		}

	}

}
