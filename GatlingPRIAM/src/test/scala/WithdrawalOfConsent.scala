
  import io.gatling.core.Predef._
  import io.gatling.http.Predef._
  import scala.concurrent.duration._

  class WithdrawalOfConsent  extends Simulation {

    val httpProtocol = http
      .baseUrl("http://localhost:8090/cdp/api/consent")
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")

    val refId = 508
    val processingId = 3

    val consentBodyJson =
      s"""{
         |  "processingId": $processingId
         |}""".stripMargin

    val scn = scenario("Submit newConsent with refId in URL and processingId in body")
      .exec(
        http("Submit newConsent request")
          .post(s"/create/$refId")
          .body(StringBody(consentBodyJson)).asJson
          .check(status.is(200))
      )

    setUp(
      scn.inject(atOnceUsers(1))
    ).protocols(httpProtocol)
  }