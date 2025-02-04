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

package uk.gov.hmrc.agentinvitationsfrontend.config

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import javax.inject.{Inject, Singleton}

@Singleton
class ExternalUrls @Inject()(implicit appConfig: AppConfig) {

  val companyAuthUrl = appConfig.companyAuthFrontendExternalUrl
  val companyAuthSignOutPath = appConfig.companyAuthFrontendSignoutPath
  val companyAuthSignInPath = appConfig.companyAuthFrontendSigninPath
  val businessTaxAccountUrl = appConfig.btaExternalUrl
  val agentServicesAccountUrl = s"${appConfig.asaFrontendExternalUrl}/agent-services-account/home"
  val contactFrontendUrl = appConfig.contactFrontendExternalUrl
  val exitSurveyUrl = appConfig.feedbackSurveyUrl
  val agentOriginToken = appConfig.agentOriginToken
  val clientOriginToken = appConfig.clientOriginToken

  val subscriptionURL = appConfig.agentSubscriptionFrontendExternalUrl
  val agentClientManagementUrl = appConfig.acmExternalUrl
  val agentInvitationsExternalUrl = appConfig.agentInvitationsFrontendExternalUrl
  val privacypolicyUrl = appConfig.privacyPolicyExternalUrl
  val agentMappingFrontendUrl = s"${appConfig.agentMappingFrontendExternalUrl}/agent-mapping/start"
  val timeout = appConfig.timeoutDialogTimeoutSeconds
  val timeoutCountdown = appConfig.timeoutDialogCountdownSeconds
  val guidanceUrlVatExisting = s"${appConfig.govUkGuidanceExternalUrl}/sign-up-for-making-tax-digital-for-vat"
  val guidanceUrlVatNew = s"${appConfig.govUkGuidanceExternalUrl}/sign-your-business-up-for-making-tax-digital-for-vat"
  val guidanceUrlSaExisting = s"${appConfig.govUkGuidanceExternalUrl}/agents-use-software-to-send-income-tax-updates"
  val guidanceUrlSaNew = s"${appConfig.govUkGuidanceExternalUrl}/use-software-to-send-income-tax-updates"
  val guidanceAuthoriseAgent =
    s"${appConfig.govUkGuidanceExternalUrl}/authorise-an-agent-to-deal-with-certain-tax-services-for-you"

  val companyAuthFrontendSignOutUrl = s"$companyAuthUrl$companyAuthSignOutPath"
  val companyAuthFrontendSignInUrl = s"$companyAuthUrl$companyAuthSignInPath"

  private def contactFrontendServiceId(isAgent: Boolean) =
    if (isAgent) agentOriginToken else clientOriginToken

  def signOutUrl(isAgent: Boolean, goToSurvey: Option[Boolean]): String = {
    val continueUrl = if (isAgent) {
      if (goToSurvey.getOrElse(false)) s"$exitSurveyUrl/$agentOriginToken"
      else agentServicesAccountUrl
    } else {
      if (goToSurvey.getOrElse(false)) s"$exitSurveyUrl/$clientOriginToken"
      else s"$businessTaxAccountUrl/business-account"
    }
    s"$companyAuthFrontendSignOutUrl?continue=${URLEncoder.encode(continueUrl, StandardCharsets.UTF_8.name())}"
  }

  def contactFrontendAjaxUrl(isAgent: Boolean): String =
    s"$contactFrontendUrl/contact/problem_reports_ajax?service=${contactFrontendServiceId(isAgent)}"

  def contactFrontendNonJsUrl(isAgent: Boolean): String =
    s"$contactFrontendUrl/contact/problem_reports_nonjs?service=${contactFrontendServiceId(isAgent)}"
}
