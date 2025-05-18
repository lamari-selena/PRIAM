import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class RectificationRequestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8083")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val newValue = "Richar" // Définir la valeur ici
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
        .post("/api/right/rectificationRequest")
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
           |  "providerClaim": "Rectification acceptée"
           |}""".stripMargin
      session
        .set("answerPayload", answerJson)
        .set("expectedValue", newValue) // stocke newValue dans la session
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
      val expected = session("expectedValue").as[String] // récupérer dynamiquement
      val table = "persistenceuser"
      val column = "REALNAME"
      val primaryKeys = Map("ID" -> ID).asJava

      try {
        val actual = DataVerifier.getDataValue(ID, column, table, primaryKeys)
        assert(actual == expected, s"Mauvaise valeur en base : attendue '$expected', trouvée '$actual'")
        println(s" Rectification confirmée pour $column = '$actual'")
      } catch {
        case e: Exception =>
          println("Erreur lors de la vérification en base : " + e.getMessage)
          throw e
      }

      session
    }

  setUp(
    scn.inject(
      rampUsers(50).during(10.seconds) // 100 utilisateurs répartis sur 0.5s
    )
    //scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}

/*
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class RectificationRequestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8083")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val rectificationRequestJson =
    """{
      "dataSubjectId": 2,
      "dataTypeName": "PERSISTENCEUSER",
      "data": {
        "dataId": 5
      },
      "newValue": "Richardo",
      "claim": "erreur dans le real name"
    }"""

  val scn = scenario("Rectification Request Flow")
    .exec(
      http("Submit rectification request")
        .post("/api/right/rectificationRequest")
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
           |  "providerClaim": "Rectification acceptée"
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
      val expected = "Richard"
      val table = "persistenceuser"
      val column = "REALNAME"
      val primaryKeys = Map("ID" -> ID).asJava

      try {
        val actual = DataVerifier.getDataValue(ID, column, table, primaryKeys)
        assert(actual == expected, s"Mauvaise valeur en base : attendue '$expected', trouvée '$actual'")
        println(s" Rectification confirmée pour $column=$actual")
      } catch {
        case e: Exception =>
          println("Erreur lors de la vérification en base : " + e.getMessage)
          throw e
      }

      session
    }

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
*/
