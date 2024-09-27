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

package uk.gov.hmrc.emailgateway

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.test.ExternalWireMockSupport

class EmailControllerIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with ExternalWireMockSupport {

  private val wsClient = app.injector.instanceOf[WSClient]
  private val baseUrl = s"http://localhost:$port"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.enabled" -> false,
        "microservice.services.email-verification.port" -> externalWireMockPort
      )
      .build()

  "AddressInsightsController" should {
    "respond with OK status" when {
      "valid json payload is provided" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/verify"))
            .withRequestBody(equalToJson("""{"email":"email@test.com"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
            .willReturn(
              aResponse()
                .withBody(
                  """{"correlationId":"220967234589763549876", "email": "email@test.com"}"""
                )
                .withStatus(OK)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/verify")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"email":"email@test.com"}""")
            .futureValue

        response.status shouldBe OK
        response.json shouldBe Json.parse(
          """{"correlationId":"220967234589763549876", "email":"email@test.com"}"""
        )
      }
    }

    "respond with BAD_REQUEST status" when {
      "invalid json payload is provided" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/verify"))
            .withRequestBody(equalToJson("""{"email":"email@test.com"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
            .willReturn(
              aResponse()
                .withBody(
                  """{"correlationId":"220967234589763549876", "email": "email@test.com"}"""
                )
                .withStatus(OK)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/verify")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"email": "email@test.com"""")
            .futureValue

        response.status shouldBe BAD_REQUEST
        response.json shouldBe Json.parse(
          """{"statusCode":400,"message":"bad request, cause: invalid json"}"""
        )
      }
    }

    "respond with NOT_FOUND status" when {
      "endpoint is not found" in {
        externalWireMockServer.stubFor(
          post(urlEqualTo(s"/non-existent"))
            .withRequestBody(equalToJson("""{"email":"email@test.com"}"""))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo(MimeTypes.JSON))
            .willReturn(
              aResponse()
                .withStatus(NOT_FOUND)
            )
        )
        val response =
          wsClient
            .url(s"$baseUrl/non-existent")
            .withHttpHeaders(HeaderNames.CONTENT_TYPE -> MimeTypes.JSON)
            .post("""{"email": "email@test.com"""")
            .futureValue

        response.status shouldBe NOT_FOUND
        response.json shouldBe Json.parse(
          """{"statusCode":404,"message":"URI not found","requested":"/non-existent"}"""
        )
      }
    }
  }
}
