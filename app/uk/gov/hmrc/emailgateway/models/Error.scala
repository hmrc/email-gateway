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

package uk.gov.hmrc.emailgateway.models

import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.emailgateway.config.Constants

sealed class Error(val code: String, val desc: String)

case object RequestForwardingError extends
  Error("REQUEST_FORWARDING", "An issue occurred when forwarding the request to the downstream service")

case object DownstreamError extends
  Error("REQUEST_DOWNSTREAM", "An issue occurred when the downstream service tried to handle the request")

case object UnsupportedMethodError extends
  Error("UNSUPPORTED_METHOD", "Unsupported HTTP method")

case object MissingCorrelationId extends Error("MISSING_CORRELATION_ID", s"${Constants.xCorrelationId} header is missing from the request")

object Error {
  implicit val writes: Writes[Error] = Writes { model =>
    Json.obj(
      "code" -> model.code,
      "desc" -> model.desc
    )
  }
}
