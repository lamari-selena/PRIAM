
  import io.gatling.core.Predef._
  import io.gatling.http.Predef._
  import scala.concurrent.duration._
  import scala.jdk.CollectionConverters._

  class ErasureRequestSimulation extends Simulation {

    val httpProtocol = http
      .baseUrl("http://localhost:8083")
      .acceptHeader("application/json")
      .contentTypeHeader("application/json")

    val erasureRequestJson =
      """{
      "dataSubjectId": 2,
      "dataTypeName": "PERSISTENCEUSER",
      "data": {
        "dataId": 5
      },
      "claim": "illegal"
    }"""

    val scn = scenario("Erasure Request Flow")
      .exec(
        http("Submit erasure request")
          .post("/api/right/erasureRequest")
          .body(StringBody(erasureRequestJson)).asJson
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
             |  "providerClaim": "Erasure acceptée"
             |}""".stripMargin
        session.set("answerPayload", answerJson)
      })
      .exec(
        http("Answer rectification request")
          .post("/api/right/answer")
          .body(StringBody("${answerPayload}")).asJson
          .check(status.is(200))
      )
      .exec { session =>
        import gatling.org.DataVerifier

        val ID = "508"
        val expected = ""
        val table = "persistenceuser"
        val column = "REALNAME"
        val primaryKeys = Map("ID" -> ID).asJava

        try {
          val actual = DataVerifier.getDataValue(ID, column, table, primaryKeys)
          assert(actual == expected, s"Bad value in database: expected: expected '$expected', actual '$actual'")
          println(s" Erasure confirmed for $column=$actual")
        } catch {
          case e: Exception =>
            println("Error during database check: " + e.getMessage)
            throw e
        }

        session
      }

    setUp(
      scn.inject(atOnceUsers(1))
    ).protocols(httpProtocol)

}
