import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AccessRequestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8083")
    .acceptHeader("application/json")

  val expectedValues = Map(
    "pu_REALNAME" -> "Rich",
    "pu_USERNAME" -> "use1",
    "pu_EMAIL"    -> "use1@petsupplystore.com"
  )

  val scn = scenario("Access Request and Value Verification")
    // 1. Requête d'accès avec extraction des données
    .exec(
      http("01-AccessRequest - HTTP 200 Check")
        .get("/api/personalDataValues/accessRight")
        .queryParam("dataSubjectId", "508")
        .queryParam("dataTypeName", "PERSISTENCEUSER")
        .queryParam("attributes", "pu_REALNAME")
        .queryParam("attributes", "pu_USERNAME")
        .queryParam("attributes", "pu_EMAIL")
        .check(status.is(200)) // Check visible
        .check(jmesPath("[?attribute=='pu_REALNAME'].value | [0]").saveAs("realname"))
        .check(jmesPath("[?attribute=='pu_USERNAME'].value | [0]").saveAs("username"))
        .check(jmesPath("[?attribute=='pu_EMAIL'].value | [0]").saveAs("email"))
    )
    // 2. Check REALNAME
    .exec(
      http("02-Check REALNAME Placeholder")
        .get("/fake") // requête bidon
    ).exec { session =>
      val realname = session("realname").asOption[String].getOrElse("Not found")
      val expected = expectedValues("pu_REALNAME")
      if (realname != expected) {
        println(s"[KO] REALNAME attendu '$expected', trouvé '$realname'")
        session.markAsFailed
      } else {
        println(s"[OK] REALNAME = $realname")
        session
      }
    }
    // 3. Check USERNAME
    .exec(
      http("03-Check USERNAME Placeholder")
        .get("/fake")
    ).exec { session =>
      val username = session("username").asOption[String].getOrElse("Not found")
      val expected = expectedValues("pu_USERNAME")
      if (username != expected) {
        println(s"[KO] USERNAME attendu '$expected', trouvé '$username'")
        session.markAsFailed
      } else {
        println(s"[OK] USERNAME = $username")
        session
      }
    }
    // 4. Check EMAIL
    .exec(
      http("04-Check EMAIL Placeholder")
        .get("/fake") // requête bidon
    ).exec { session =>
      val email = session("email").asOption[String].getOrElse("Not found")
      val expected = expectedValues("pu_EMAIL")
      if (email != expected) {
        println(s"[KO] EMAIL attendu '$expected', trouvé '$email'")
        session.markAsFailed
      } else {
        println(s"[OK] EMAIL = $email")
        session
      }
    }

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
/*
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AccessRequestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8083")
    .acceptHeader("application/json")

  val expectedValues = Map(
    "pu_REALNAME" -> "Rich",
    "pu_USERNAME" -> "user1",
    "pu_EMAIL"    -> "user1@petsupplystore.com"
  )

  val scn = scenario("Access Request and Value Verification")
    .exec(
      http("Submit Access Request")
        .get("/api/personalDataValues/accessRight")
        .queryParam("dataSubjectId", "508")
        .queryParam("dataTypeName", "PERSISTENCEUSER")
        .queryParam("attributes", "pu_REALNAME")
        .queryParam("attributes", "pu_USERNAME")
        .queryParam("attributes", "pu_EMAIL")
        .check(status.is(200))
        .check(jmesPath("[?attribute=='pu_REALNAME'].value | [0]").saveAs("realname"))
        .check(jmesPath("[?attribute=='pu_USERNAME'].value | [0]").saveAs("username"))
        .check(jmesPath("[?attribute=='pu_EMAIL'].value | [0]").saveAs("email"))
    )
    .exec { session =>
      val realname = session("realname").asOption[String].getOrElse("Not found")
      val username = session("username").asOption[String].getOrElse("Not found")
      val email    = session("email").asOption[String].getOrElse("Not found")

      assert(realname == expectedValues("pu_REALNAME"), s" REALNAME incorrect : attendu '${expectedValues("pu_REALNAME")}', trouvé '$realname'")
      assert(username == expectedValues("pu_USERNAME"), s" USERNAME incorrect : attendu '${expectedValues("pu_USERNAME")}', trouvé '$username'")
      assert(email == expectedValues("pu_EMAIL"), s" EMAIL incorrect : attendu '${expectedValues("pu_EMAIL")}', trouvé '$email'")

      println(s"Accès validé : $realname / $username / $email")
      session
    }

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
*/
