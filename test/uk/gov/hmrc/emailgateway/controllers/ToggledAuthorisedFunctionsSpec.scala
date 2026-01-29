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


import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.emailgateway.ToggledAuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ToggledAuthorisedFunctionsSpec extends AnyWordSpec with Matchers with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "toggledAuthorised should execute body when enabled is true and predicate is satisfied" in {
    val mockAuthConnector = mock[AuthConnector]
    val predicate = mock[Predicate]

    when(mockAuthConnector.authorise(predicate, EmptyRetrieval)).thenReturn(Future.successful(()))

    val toggledFunctions = new ToggledAuthorisedFunctions {
      override val authConnector: AuthConnector = mockAuthConnector
    }

    val result = toggledFunctions.toggledAuthorised(enabled = true, predicate)(Future.successful("Success")).futureValue

    result mustBe "Success"
  }

  "toggledAuthorised should execute body when enabled is false without checking predicate" in {
    val mockAuthConnector = mock[AuthConnector]
    val predicate = mock[Predicate]

    val toggledFunctions = new ToggledAuthorisedFunctions {
      override val authConnector: AuthConnector = mockAuthConnector
    }

    val result = toggledFunctions.toggledAuthorised(enabled = false, predicate)(Future.successful("Success")).futureValue

    result mustBe "Success"
  }

  "toggledAuthorised should fail when enabled is true and predicate is not satisfied" in {
    val mockAuthConnector = mock[AuthConnector]
    val predicate = mock[Predicate]
    when(mockAuthConnector.authorise(predicate, EmptyRetrieval)).thenReturn(Future.failed(new RuntimeException("Unauthorized")))

    val toggledFunctions = new ToggledAuthorisedFunctions {
      override val authConnector: AuthConnector = mockAuthConnector
    }

    intercept[RuntimeException] {
      toggledFunctions.toggledAuthorised(enabled = true, predicate)(Future.successful("Success")).futureValue
    }

  }
}
