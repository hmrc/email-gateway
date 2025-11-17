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

import play.api.Logging
import play.api.libs.json.JsValue
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.{PrivilegedApplication, StandardApplication}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthProviders}
import uk.gov.hmrc.emailgateway.ToggledAuthorisedFunctions
import uk.gov.hmrc.emailgateway.config.{AppConfig, Constants}
import uk.gov.hmrc.emailgateway.connector.DownstreamConnector
import uk.gov.hmrc.emailgateway.controllers.actions.CorrelationIdAction
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton()
class EmailInsightsController @Inject()(
  cc: ControllerComponents,
  withCorrelationId: CorrelationIdAction,
  config: AppConfig,
  connector: DownstreamConnector,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with ToggledAuthorisedFunctions
    with Logging {

  def checkInsights(): Action[JsValue] =
    (Action andThen withCorrelationId).async(parse.json) { implicit request =>
      toggledAuthorised(config.rejectInternalTraffic, AuthProviders(StandardApplication, PrivilegedApplication)) {

        val path = request.target.uri.toString.replace("email-gateway", "email-insights-proxy")
        val url = s"${config.emailInsightsProxyBaseUrl}$path"

        connector
          .forward(request, url, config.internalAuthToken)
          .map(_.withHeaders(Constants.xCorrelationId -> request.correlationId))
      }
    }
}
