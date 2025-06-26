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

import org.apache.pekko.stream.Materializer
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test._

class DocumentationControllerSpec extends PlaySpec with GuiceOneAppPerSuite {

  val materializer: Materializer = app.materializer

  lazy val controller: DocumentationController = app.injector.instanceOf[DocumentationController]

  "DocumentationController" must {

    "return a definition" in {
      val result = controller.definition()(FakeRequest("GET", "/api/definition"))
      status(result) mustBe OK
    }

    "return a specification for a given version and file" in {
      val version = "1.0"
      val file = "application.yaml"
      val result = controller.specification(version, file)(FakeRequest("GET", s"/api/conf/$version/$file"))
      status(result) mustBe OK
    }
  }
}