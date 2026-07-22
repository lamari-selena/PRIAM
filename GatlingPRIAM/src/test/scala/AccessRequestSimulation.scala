import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._

class AccessRequestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8090/right/api/")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val expectedValues = Map(
    "pu_REALNAME" -> "Helen Johnson",
    "pu_USERNAME" -> "user0",
    "pu_EMAIL"    -> "user0@petsupplystore.com"
  )

  // Mapping dataId to attribute names
  val dataIdToAttribute = Map(
    3 -> "pu_REALNAME",
    5 -> "pu_USERNAME",
    6 -> "pu_EMAIL"
  )

  // Define full answer data list with mixed statuses
  val fullAnswerData = Seq(
    Map("dataId" -> 3, "status" -> "ACCEPTED"),
    Map("dataId" -> 5, "status" -> "REJECTED"),
    Map("dataId" -> 6, "status" -> "REJECTED")
  )

  val scn = scenario("Access Request - Full Flow")

    // STEP 1: Send access request
    .exec { session =>
      println("[STEP 1] Sending access request for dataSubjectId = 1")
      session
    }
    .exec(
      http("Send Access Request")
        .post("right/accessRequest")
        .body(StringBody(
          """{
            |  "dataSubjectId": 1,
            |  "dataRequestClaim": "ACCESS_REQUEST_2025",
            |  "requestType": "ACCESS",
            |  "data": [
            |    { "dataId": 3 },
            |    { "dataId": 5 },
            |    { "dataId": 6 }
            |  ]
            |}""".stripMargin)).asJson
        .check(status.is(200))
        .check(jsonPath("$.dataRequestId").saveAs("requestId"))
    )

    // STEP 2: Send answer with accept/reject statuses
    .exec { session =>
      val requestId = session("requestId").as[Int]
      val dataJson = fullAnswerData.map { entry =>
        s"""{ "dataId": ${entry("dataId")}, "status": "${entry("status")}" }"""
      }.mkString(",\n")

      println(s"[STEP 2] Sending answer for requestId = $requestId")

      val jsonBody =
        s"""{
           |  "answer": true,
           |  "providerClaim": "ACCESS_REQUEST_2025",
           |  "dataRequestId": $requestId,
           |  "data": [ $dataJson ]
           |}""".stripMargin

      session.set("answerBody", jsonBody)
    }
    .exec(
      http("Send Full Answer")
        .post("right/answer")
        .body(StringBody(session => session("answerBody").as[String])).asJson
        .check(status.is(200))
    )

    // STEP 3: Wait for backend propagation
    .exec { session =>
      println("[STEP 3] Waiting for backend propagation...")
      session
    }
    .pause(2)

    // STEP 4: Dynamically extract accepted dataIds from fullAnswerData
    .exec { session =>
      println("[STEP 4] Filtering accepted dataIds and preparing attributes to fetch")

      val acceptedDataIds = fullAnswerData
        .filter(_("status") == "ACCEPTED")
        .map(_("dataId").asInstanceOf[Int])

      val acceptedAttributes = acceptedDataIds.flatMap(dataIdToAttribute.get)

      println(s"Attributes to request: ${acceptedAttributes.mkString(", ")}")

      session
        .set("acceptedAttributes", acceptedAttributes)
    }

    // STEP 5: Fetch only accepted attributes values
    .exec(
      http("AccessRequest - Check Returned Values")
        .get("personalDataValues/accessRight")
        .queryParam("dataSubjectId", "507")
        .queryParam("dataTypeName", "PERSISTENCEUSER")
        .queryParamSeq(session =>
          session("acceptedAttributes").as[Seq[String]].map(attr => ("attributes", attr))
        )
        .check(
          jmesPath("[?attribute=='pu_REALNAME'].value | [0]").optional.saveAs("pu_realname"),
          jmesPath("[?attribute=='pu_USERNAME'].value | [0]").optional.saveAs("pu_username"),
          jmesPath("[?attribute=='pu_EMAIL'].value | [0]").optional.saveAs("pu_email")
        )
    )

// STEP 6: Verify returned values correspond to accepted attributes only
.exec { session =>
  println("[STEP 6] Verifying returned values (only accepted should appear)...")

  val expectedValues = Map(
    "pu_REALNAME" -> "Helen Johnson",
    "pu_USERNAME" -> "user0",
    "pu_EMAIL"    -> "user0@petsupplystore.com"
  )

  // Liste des attributs acceptés dynamiquement
  val acceptedAttrs = session("acceptedAttributes").as[Seq[String]]

  def check(attr: String, expected: String, wasAccepted: Boolean): Unit = {
    session(attr.toLowerCase).asOption[String] match {
      case Some(value) if value == expected =>
        println(s"[OK] $attr = $value")
      case Some(value) =>
        println(s"[KO] $attr expected '$expected', but got '$value'")
      case None if wasAccepted =>
        println(s"[KO] $attr was expected (accepted) but not returned")
      case None =>
        println(s"[OK] $attr correctly not returned (rejected)")
    }
  }

  check("pu_REALNAME", expectedValues("pu_REALNAME"), acceptedAttrs.contains("pu_REALNAME"))
  check("pu_USERNAME", expectedValues("pu_USERNAME"), acceptedAttrs.contains("pu_USERNAME"))
  check("pu_EMAIL", expectedValues("pu_EMAIL"), acceptedAttrs.contains("pu_EMAIL"))

  session
}


  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}
