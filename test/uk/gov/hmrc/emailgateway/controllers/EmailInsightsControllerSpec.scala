/*
 * Copyright 2025 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.emailgateway.config.{AppConfig, Constants}
import uk.gov.hmrc.emailgateway.connector.DownstreamConnector
import uk.gov.hmrc.emailgateway.controllers.actions.CorrelationIdActionImpl
import uk.gov.hmrc.emailgateway.models.{Error, MissingCorrelationId}

import scala.concurrent.{ExecutionContextExecutor, Future}

class EmailInsightsControllerSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  val cc: ControllerComponents = stubControllerComponents()
  val config: AppConfig = mock[AppConfig]
  val connector: DownstreamConnector = mock[DownstreamConnector]
  val authConnector: AuthConnector = mock[AuthConnector]

  lazy val controller = new EmailInsightsController(
    cc = cc,
    withCorrelationId = new CorrelationIdActionImpl(),
    config = config,
    connector = connector,
    authConnector = authConnector
  )(cc.executionContext)

  "PhoneNumberInsightsController" should {

    "forward the request and preserve CorrelationId header when present" in {

      when(config.rejectInternalTraffic).thenReturn(false)
      when(config.emailInsightsProxyBaseUrl).thenReturn("http://proxy/")
      when(config.internalAuthToken).thenReturn("token")

      val fakeRequest = FakeRequest("GET", "/phone-number-gateway/insights")
        .withHeaders(Constants.xCorrelationId -> "abc-123")
        .withBody(Json.obj("phoneNumber" -> "0234567890"))

      val resultStub = Results.Ok("forwarded").withHeaders("SomeHeader" -> "value")
      when(connector.forward(any(), any(), any())(any())).thenReturn(Future.successful(resultStub))

      val result = controller.checkInsights()(fakeRequest)
      status(result) shouldBe OK
      contentAsString(result) shouldBe "forwarded"
      headers(result) should contain(Constants.xCorrelationId -> "abc-123")
    }

    "return BadRequests when xCorrelationId is missing" in {

      when(config.rejectInternalTraffic).thenReturn(false)
      when(config.emailInsightsProxyBaseUrl).thenReturn("http://proxy/")
      when(config.internalAuthToken).thenReturn("token")

      val fakeRequest = FakeRequest("GET", "/phone-number-gateway/insights")
        .withBody(Json.obj("phoneNumber" -> "0234567890"))

      val result = controller.checkInsights()(fakeRequest)

      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe Json.toJson[Error](MissingCorrelationId)
      headers(result).get(Constants.xCorrelationId) shouldBe None
    }

  }
}
