/*
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TeaStoreCartPage extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8180/tools.descartes.teastore.webui")
    .inferHtmlResources()

  val headers = Map("Content-Type" -> "application/x-www-form-urlencoded")

  val scn = scenario("Login Scenario")
    .exec(
      http("Load login page")
        .get("/login")
        .check(status.is(200))
    )
    .pause(1)
    .exec(
      http("Submit login form")
        .post("/loginAction")
        .headers(headers)
        .formParam("username", "user15")
        .formParam("password", "password")
        .check(
          status.is(200),
          substring("You used wrong credentials!").notExists
        )
    )
    .pause(1)
    .exec(
      http("Access Cart")
        .get("/cart")
        .check(status.is(200), substring("Shopping Cart").exists,
          // Vérifie qu'au moins un bloc de produit recommandé est affiché
          regex("""<div class="col-sm-12 placeholder">.*?</div>""").count.gte(1)
        )
    )
  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
*/
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TeaStoreCartPage extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8180/tools.descartes.teastore.webui")
    .inferHtmlResources()

  val headers = Map("Content-Type" -> "application/x-www-form-urlencoded")

  val scn = scenario("Login Scenario")
    // Étape 1 : Connexion à la page de connexion
    .exec(
      http("Load login page")
        .get("/login")
        .check(status.is(200))
    )
    .pause(1)
    // Étape 2 : Soumettre le formulaire de connexion
    .exec(
      http("Submit login form")
        .post("/loginAction")
        .headers(headers)
        .formParam("username", "user53")
        .formParam("password", "password")
        .check(
          status.is(200),
          substring("You used wrong credentials!").notExists
        )
    )
    .pause(1)
    // Étape 3 : Accéder à la page de catégorie pour extraire un `productId`
    .exec(
      http("Get category page")
        .get("/category?page=1&category=2")  // Utilise une catégorie valide ici
        .check(
          status.is(200),
          // Extraire le `productId` de la page de catégorie via regex
          regex("""href=".*?product.*?id=(\d+)""").find.saveAs("productId")
        )
    )
    .pause(1)
    .exec(
      http("Add product to cart")
        .get("/cartAction?addToCart&productid=7")  // Utilise le `productId` extrait ${productId}
        .check(status.is(200))
    )
    .pause(1)
    // Étape 5 : Accéder à la page du panier pour vérifier les recommandations
    .exec(
      http("Access Cart")
        .get("/cart")
        .check(
          status.is(200),
          substring("Shopping Cart").exists,
          substring("Are you interested in?").exists
        )
    )

  setUp(
    scn.inject(atOnceUsers(100))
  ).protocols(httpProtocol)
}
