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

package uk.gov.hmrc.emailgateway.connector

import play.api.Logging
import play.api.http.HeaderNames._
import play.api.http.{HeaderNames, HttpEntity, MimeTypes}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadGateway, InternalServerError, MethodNotAllowed}
import play.api.mvc.{Request, ResponseHeader, Result}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DownstreamConnector @Inject()(httpClient: HttpClientV2) extends Logging {

  def forward(request: Request[JsValue], url: String, authToken: String)(implicit ec: ExecutionContext): Future[Result] = {
    import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

    logger.info(s"Forwarding to downstream url: $url")

    (request.method, request.headers(HeaderNames.CONTENT_TYPE)) match {
      case ("POST", MimeTypes.JSON) =>
        val onwardHeaders = request.headers.remove(CONTENT_LENGTH, HOST, AUTHORIZATION).headers
        implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(authToken)))

        try {
          httpClient
            .post(url"$url")
            .withBody(request.body)
            .setHeader(onwardHeaders: _*)
            .execute[HttpResponse]
            .map { response: HttpResponse =>
              Result(
                ResponseHeader(response.status, cleanseResponseHeaders(response)),
                HttpEntity.Streamed(response.bodyAsSource, None, response.header(CONTENT_TYPE))
              )
            }
            .recover {
              case _: Throwable =>
                BadGateway(toErrorResponse("REQUEST_DOWNSTREAM", "An issue occurred when the downstream service tried to handle the request"))
            }
        } catch {
          case _: Throwable =>
            Future.successful(InternalServerError(toErrorResponse("REQUEST_FORWARDING", "An issue occurred when forwarding the request to the downstream service")))
        }

      case _ =>
        Future.successful(MethodNotAllowed(toErrorResponse("UNSUPPORTED_METHOD", "Unsupported HTTP method or content-type")))
    }
  }

  private def toErrorResponse(code: String, desc: String): JsValue =
    Json.obj("code" -> code, "desc" -> desc)

  private def cleanseResponseHeaders(response: HttpResponse): Map[String, String] =
    response.headers
      .filterNot { case (k, _) => Seq(CONTENT_TYPE, CONTENT_LENGTH, TRANSFER_ENCODING).map(_.toUpperCase).contains(k.toUpperCase) }
      .view.mapValues(_.mkString).toMap
}
