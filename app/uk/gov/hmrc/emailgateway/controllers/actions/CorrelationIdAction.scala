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

package uk.gov.hmrc.emailgateway.controllers.actions

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.Results.BadRequest
import play.api.mvc.{ActionRefiner, Request, Result}
import uk.gov.hmrc.emailgateway.config.Constants
import uk.gov.hmrc.emailgateway.models.{Error, MissingCorrelationId, RequestWithCorrelationId}

import scala.concurrent.{ExecutionContext, Future}

trait CorrelationIdAction extends ActionRefiner[Request, RequestWithCorrelationId]

class CorrelationIdActionImpl @Inject() (implicit override val executionContext: ExecutionContext) extends CorrelationIdAction {

  override def refine[A](request: Request[A]): Future[Either[Result, RequestWithCorrelationId[A]]] =
    Future.successful(request.headers.get(Constants.xCorrelationId) match {
      case Some(id) if id.trim != "" => Right(RequestWithCorrelationId(id, request))
      case _                         => Left(BadRequest(Json.toJson[Error](MissingCorrelationId)))
    })
}
