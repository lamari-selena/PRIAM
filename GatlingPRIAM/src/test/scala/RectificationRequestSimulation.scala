import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class RectificationRequestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8090/right/api/")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val newValue = "Richard"
  val rectificationRequestJson =
    s"""{
       |  "dataSubjectId": 2,
       |  "dataTypeName": "PERSISTENCEUSER",
       |  "data": {
       |    "dataId": 5
       |  },
       |  "newValue": "$newValue",
       |  "claim": "erreur dans le real name"
       |}""".stripMargin

  val scn = scenario("Rectification Request Flow")
    .exec(
      http("Submit rectification request")
        .post("right/rectificationRequest")
        .body(StringBody(rectificationRequestJson)).asJson
        .check(status.is(200))
        .check(jsonPath("$.dataRequestId").saveAs("requestId"))
    )
    .pause(1)
    .exec(session => {
      val requestId = session("requestId").asOption[String]
        .getOrElse(throw new Exception("dataRequestId not found"))
      val answerJson =
        s"""{
           |  "dataRequestId": $requestId,
           |  "answer": true,
           |  "providerClaim": "Rectification accepted"
           |}""".stripMargin
      session
        .set("answerPayload", answerJson)
        .set("expectedValue", newValue) // stores newValue in the session
    })
    .exec(
      http("Answer rectification request")
        .post("right/answer")
        .body(StringBody("${answerPayload}")).asJson
        .check(status.is(200))
    )
    .exec { session =>
      import gatling.org.DataVerifier

      val ID = "508"
      val expected = session("expectedValue").as[String] // retrieve dynamically
      val table = "PERSISTENCEUSER"
      val column = "REALNAME"
      val primaryKeys = Map("ID" -> ID).asJava

      try {
        val actual = DataVerifier.getDataValue(ID, column, table, primaryKeys)
        assert(actual == expected, s"Bad value in database: expected : attendue '$expected', trouvée '$actual'")
        println(s" Rectification confirmed pour $column = '$actual'")
      } catch {
        case e: Exception =>
          println("Error during database check:" + e.getMessage)
          throw e
      }

      session
    }

  setUp(
    //scn.inject(
    //rampUsers(1).during(10.seconds) // 100 utilisateurs répartis sur 0.5s
    //)
    scn.inject(atOnceUsers(1000))
  ).protocols(httpProtocol)
}