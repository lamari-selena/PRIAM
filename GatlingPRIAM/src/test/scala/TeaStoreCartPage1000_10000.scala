import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TeaStoreCartPage1000_10000 extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8180/tools.descartes.teastore.webui")
    .inferHtmlResources()

  val headers = Map("Content-Type" -> "application/x-www-form-urlencoded")

  // Scénario 1 : 1 utilisateur
  val scenario1 = scenario("Login_1_User")
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
        .formParam("username", "user3")
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
        .check(status.is(200), substring("Shopping Cart").exists)
    )

  // Scénario 2 : 100 utilisateurs simultanés
  val scenario100 = scenario("Login_100_Users")
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
        .formParam("username", "user3")
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
        .check(status.is(200), substring("Shopping Cart").exists)
    )
    .pause(1)
    .exec(
      http("Access Cart")
        .get("/cart")
        .check(status.is(200), substring("Shopping Cart").exists)
    )

  // Définition du setUp pour injecter les utilisateurs
  setUp(
    // Injection pour 1 utilisateur
    scenario1.inject(atOnceUsers(1)),

    // Injection pour 100 utilisateurs simultanés
    scenario100.inject(atOnceUsers(100)),

    // Injection progressive pour 10000 utilisateurs
  ).protocols(httpProtocol)
}