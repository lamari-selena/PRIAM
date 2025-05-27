import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class AccessRequestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8090/right/api/")
    .acceptHeader("application/json")

  val expectedValues = Map(
    "pu_REALNAME" -> "Helen Johnson",
    "pu_USERNAME" -> "user0",
    "pu_EMAIL"    -> "user0@petsupplystore.com"
  )

  val scn = scenario("Access Request and Value Verification")
    // 1. Access request + extraction
    .exec(
      http("AccessRequest")
        .get("personalDataValues/accessRight")
        .queryParam("dataSubjectId", "507")
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
      var hasFailure = false

      def check(attr: String, key: String): Unit = {
        val got  = session(attr).asOption[String].getOrElse("Not found")
        val want = expectedValues(key)
        if (got != want) {
          println(s"[KO] $key attendu '$want', trouvé '$got'")
          hasFailure = true
        } else {
          println(s"[OK] $key = $got")
        }
      }

      check("realname", "pu_REALNAME")
      check("username", "pu_USERNAME")
      check("email",    "pu_EMAIL")

      if (hasFailure) session.markAsFailed else session
    }


  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}