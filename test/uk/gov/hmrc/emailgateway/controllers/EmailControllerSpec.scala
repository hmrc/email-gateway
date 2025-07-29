/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.emailgateway.controllers

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results._
import play.api.routing.sird.{POST => SPOST, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.core.server.{Server, ServerConfig}

class EmailControllerSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite {
  val verificationPort = 11222

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.email-verification.port" -> verificationPort
    )
    .build()

  private val controller = app.injector.instanceOf[EmailController]
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "POST /insights" should {

    "forward a 200 response from the downstream service" in {
      val response = Json.obj(
        "correlationId" -> "some-correlation-id",
        "email" -> "joe@blogs.co.uk",
      )

      Server.withRouterFromComponents(
        ServerConfig(port = Some(verificationPort))
      ) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r @ SPOST(p"/verify") =>
            Action(Ok(response))
        }
      } { _ =>
        val requestEmailJson = Json
          .parse("""{"email":"joe@blogs.co.uk"}""")
          .as[JsObject]
        val fakeRequest = FakeRequest("POST", "/verify")
          .withBody(requestEmailJson)
          .withHeaders(
            "True-Calling-Client" -> "example-service",
            HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
          )

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.OK
        contentAsJson(result) shouldBe response
      }
    }

    "forward a 404 response from the downstream service" in {
      val errorResponse =
        Json.obj("code" -> "NOT_FOUND", "desc" -> "API not found")

      Server.withRouterFromComponents(
        ServerConfig(port = Some(verificationPort))
      ) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r @ SPOST(p"/verify") =>
            Action(NotFound(errorResponse))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/verify")
          .withBody(Json.parse("""{"no-email": ""}"""))
          .withHeaders(
            "True-Calling-Client" -> "example-service",
            HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
          )

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
        contentAsJson(result) shouldBe errorResponse
      }
    }

    "forward a 400 response from the downstream service" in {
      val errorResponse =
        Json.obj("code" -> "MALFORMED_JSON", "path.missing" -> "email")

      Server.withRouterFromComponents(
        ServerConfig(port = Some(verificationPort))
      ) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r @ SPOST(p"/verify") =>
            Action(BadRequest(errorResponse))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/verify")
          .withBody(Json.parse("""{"no-email": ""}"""))
          .withHeaders(
            "True-Calling-Client" -> "example-service",
            HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
          )

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsJson(result) shouldBe errorResponse
      }
    }

    "handle a malformed json payload" in {

      val errorResponse =
        Json.obj("statusCode" -> 400, "message" -> "bad request, cause: invalid json")

      Server.withRouterFromComponents(
        ServerConfig(port = Some(verificationPort))
      ) { components =>
        import components.{defaultActionBuilder => Action}
        {
          case r @ SPOST(p"/verify") =>
            Action(BadRequest(errorResponse))
        }
      } { _ =>
        val fakeRequest = FakeRequest("POST", "/verify")
          .withTextBody("""{""")
          .withHeaders(
            "True-Calling-Client" -> "example-service",
            HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
          )

        val result = controller.any()(fakeRequest)
        status(result) shouldBe Status.BAD_REQUEST
        contentAsJson(result) shouldBe errorResponse
      }
    }

    "return bad gateway if there is no connectivity to the downstream service" in {

      val fakeRequest = FakeRequest("POST", "/email-gateway/verify")
        .withBody(Json.parse("""{"address": "AB123456C"}"""))
        .withHeaders(
          "True-Calling-Client" -> "example-service",
          HeaderNames.CONTENT_TYPE -> MimeTypes.JSON
        )

      val result = controller.any()(fakeRequest)
      status(result) shouldBe Status.BAD_GATEWAY
      contentAsJson(result) shouldBe Json.obj(
        "code" -> "REQUEST_DOWNSTREAM",
        "desc" -> "An issue occurred when the downstream service tried to handle the request"
      )
    }
  }
}
