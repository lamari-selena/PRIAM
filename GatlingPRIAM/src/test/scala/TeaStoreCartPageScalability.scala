import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class TeaStoreCartPageScalability extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8180/tools.descartes.teastore.webui")
    .inferHtmlResources()

  val headers = Map("Content-Type" -> "application/x-www-form-urlencoded")

  val scn = scenario("Login Scenario")
    // Step 1: Load login page
    .exec(
      http("Load login page")
        .get("/login")
        .check(status.is(200))
    )
    .pause(1)
    // Step 2: Submit login form
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
    // Step 3: Access category page to extract a productId
    .exec(
      http("Get category page")
        .get("/category?page=1&category=2")  // Use a valid category here
        .check(
          status.is(200),
          // Extract the productId from the category page using regex
          regex("""href=".*?product.*?id=(\d+)""").find.saveAs("productId")
        )
    )
    .pause(1)
    // Step 4: Add product to cart
    .exec(
      http("Add product to cart")
        .get("/cartAction?addToCart&productid=7")  // You can replace 7 with ${productId} if needed
        .check(status.is(200))
    )
    .pause(1)
    // Step 5: Access the cart page to check recommendations
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
    // Inject users: ramp up 50 users over 10 seconds
    scn.inject(
      incrementUsersPerSec(10)         // ➕ On augmente le débit de 10 utilisateurs par seconde...
        .times(5)                      // ...et on répète ça 5 fois (donc 5 paliers)
        .eachLevelLasting(30.seconds)  // Chaque palier dure 30 secondes
        .separatedByRampsLasting(10.seconds) // Entre chaque palier, 10s pour monter au niveau suivant
    )
  ).protocols(httpProtocol)
}

