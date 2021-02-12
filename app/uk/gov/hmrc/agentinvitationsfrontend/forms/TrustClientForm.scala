/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.agentinvitationsfrontend.forms

import play.api.data.Form
import play.api.data.Forms.mapping
import uk.gov.hmrc.agentinvitationsfrontend.config.AppConfig
import uk.gov.hmrc.agentinvitationsfrontend.models.{TrustClient, TrustNTClient}
import uk.gov.hmrc.agentinvitationsfrontend.validators.Validators.{normalizedText, validUrn, validUtr}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr

object TrustClientForm {

  def form()(implicit appConfig: AppConfig): Form[TrustClient] =
    if (appConfig.featuresAcceptTrustURNIdentifier)
      Form(
        mapping(
          "taxId" -> normalizedText.verifying(validUrn())
        )(x => TrustNTClient.apply(x))(x => Some(x.value))
      )
    else
      Form(
        mapping(
          "taxId" -> normalizedText.verifying(validUtr())
        )(x => TrustClient.apply(Utr(x)))(x => Some(x.taxId.value))
      )
}
