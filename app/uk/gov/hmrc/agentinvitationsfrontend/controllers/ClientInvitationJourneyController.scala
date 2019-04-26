/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.agentinvitationsfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentinvitationsfrontend.config.ExternalUrls
import uk.gov.hmrc.agentinvitationsfrontend.connectors.InvitationsConnector
import uk.gov.hmrc.agentinvitationsfrontend.journeys.ClientInvitationJourneyModel.State._
import uk.gov.hmrc.agentinvitationsfrontend.journeys.ClientInvitationJourneyService
import uk.gov.hmrc.agentinvitationsfrontend.models._
import uk.gov.hmrc.agentinvitationsfrontend.services._
import uk.gov.hmrc.agentinvitationsfrontend.validators.Validators.{confirmationChoice, normalizedText}
import uk.gov.hmrc.agentinvitationsfrontend.views.clients._
import uk.gov.hmrc.agentinvitationsfrontend.views.html.clients._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.fsm.{JourneyController, JourneyIdSupport}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class ClientInvitationJourneyController @Inject()(
  invitationsService: InvitationsService,
  invitationsConnector: InvitationsConnector,
  authActions: AuthActions,
  override val journeyService: ClientInvitationJourneyService)(
  implicit configuration: Configuration,
  val externalUrls: ExternalUrls,
  val messagesApi: play.api.i18n.MessagesApi,
  featureFlags: FeatureFlags,
  ec: ExecutionContext)
    extends FrontendController with JourneyController with JourneyIdSupport with I18nSupport {

  import ClientInvitationJourneyController._
  import authActions._
  import invitationsConnector._
  import invitationsService._
  import journeyService.model.{State, Transitions}
  import uk.gov.hmrc.play.fsm.OptionalFormOps._

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier =
    appendJourneyId(super.hc)

  val AsClient: WithAuthorised[AuthorisedClient] = { implicit request: Request[Any] =>
    withAuthorisedAsAnyClient
  }

  /* Here we decide how to handle HTTP request and transition the state of the journey */

  def warmUp(clientType: String, uid: String, normalisedAgentName: String) =
    Action.async { implicit request =>
      journeyId match {
        case None =>
          // redirect to itself with new journeyId generated
          Future.successful(
            appendJourneyId(
              Results.Redirect(routes.ClientInvitationJourneyController.warmUp(clientType, uid, normalisedAgentName)))(
              request))
        case _ =>
          apply(
            Transitions.start(clientType, uid, normalisedAgentName)(getAgentReferenceRecord)(getAgencyName),
            display)
      }
    }

  val submitWarmUp = action { implicit request =>
    authorised(AsClient)(Transitions.submitWarmUp(getAllClientInvitationsInfoForAgentAndStatus))(redirect)
  }

  val showConsent = showCurrentStateWhenAuthorised(AsClient) {
    case _: MultiConsent =>
  }

  def showConsentIndividual = showCurrentStateWhenAuthorised(AsClient) {
    case _ =>
  }

  val showNotFoundInvitation = showCurrentStateWhenAuthorised(AsClient) {
    case NotFoundInvitation =>
  }

  def showIncorrectClientType = showCurrentStateWhenAuthorised(AsClient) {
    case _: IncorrectClientType =>
  }

  def submitConsent = action { implicit request =>
    authorisedWithForm(AsClient)(confirmTermsMultiForm)(Transitions.submitConsents)
  }

  def submitChangeConsents = action { implicit request =>
    authorisedWithForm(AsClient)(confirmTermsMultiForm)(Transitions.submitChangeConsents)
  }

  def showCheckAnswers = showCurrentStateWhenAuthorised(AsClient) {
    case _: CheckAnswers =>
  }

  def submitCheckAnswers = action { implicit request =>
    authorised(AsClient)(Transitions.submitCheckAnswers(acceptInvitation)(rejectInvitation))(redirect)
  }

  def submitCheckAnswersChange(uid: String) = action { implicit request =>
    authorised(AsClient)(Transitions.submitCheckAnswersChange(uid))(redirect)
  }

  def submitWarmUpConfirmDecline = action { implicit request =>
    authorised(AsClient)(Transitions.submitWarmUpToDecline(getAllClientInvitationsInfoForAgentAndStatus))(redirect)
  }

  def showConfirmDecline = showCurrentStateWhenAuthorised(AsClient) {
    case _: ConfirmDecline =>
  }

  def submitConfirmDecline = action { implicit request =>
    authorisedWithForm(AsClient)(confirmDeclineForm)(Transitions.submitConfirmDecline(rejectInvitation))
  }

  def showInvitationsAccepted = action { implicit request =>
    whenAuthorised(AsClient) {
      case _: InvitationsAccepted =>
    }(display)
      .andThen {
        // clears journey history
        case Success(_) => journeyService.cleanBreadcrumbs()
      }
  }

  def showInvitationsDeclined = action { implicit request =>
    whenAuthorised(AsClient) {
      case _: InvitationsDeclined =>
    }(display)
      .andThen {
        // clears journey history
        case Success(_) => journeyService.cleanBreadcrumbs()
      }
  }

  def showAllResponsesFailed = showCurrentStateWhenAuthorised(AsClient) {
    case AllResponsesFailed =>
  }

  def showSomeResponsesFailed = showCurrentStateWhenAuthorised(AsClient) {
    case _: SomeResponsesFailed =>
  }

  /* Here we map states to the GET endpoints for redirecting and back linking */
  override def getCallFor(state: State)(implicit request: Request[_]): Call = state match {
    case Root => routes.AgentInvitationJourneyController.agentsRoot() // would be better to have client's root as well
    case WarmUp(clientType, uid, _, normalisedAgentName) =>
      routes.ClientInvitationJourneyController.warmUp(ClientType.fromEnum(clientType), uid, normalisedAgentName)
    case NotFoundInvitation     => routes.ClientInvitationJourneyController.showNotFoundInvitation()
    case _: IncorrectClientType => routes.ClientInvitationJourneyController.showIncorrectClientType()
    case _: MultiConsent        => routes.ClientInvitationJourneyController.showConsent()
    case _: SingleConsent       => routes.ClientInvitationJourneyController.showConsentIndividual()
    case _: CheckAnswers        => routes.ClientInvitationJourneyController.showCheckAnswers()
    case _: ConfirmDecline      => routes.ClientInvitationJourneyController.showConfirmDecline()
    case _: InvitationsAccepted => routes.ClientInvitationJourneyController.showInvitationsAccepted()
    case _: InvitationsDeclined => routes.ClientInvitationJourneyController.showInvitationsDeclined()
    case AllResponsesFailed     => routes.ClientInvitationJourneyController.showAllResponsesFailed()
    case _: SomeResponsesFailed => routes.ClientInvitationJourneyController.showSomeResponsesFailed()
    case _                      => throw new Exception(s"Link not found for $state")
  }

  /* Here we decide what to render after state transition */
  override def renderState(state: State, breadcrumbs: List[State], formWithErrors: Option[Form[_]])(
    implicit request: Request[_]): Result = state match {

    case Root =>
      // There is no client root and we will not try to render page for it.
      throw new Exception("Unsupported journey state, cannot render the page.")

    case WarmUp(clientType, uid, agentName, _) =>
      Ok(
        warm_up(
          WarmUpPageConfig(
            agentName,
            clientType,
            uid,
            routes.ClientInvitationJourneyController.submitWarmUp(),
            routes.ClientInvitationJourneyController.submitWarmUpConfirmDecline()
          )))

    case NotFoundInvitation =>
      val serviceMessageKey = request.session.get("clientService").getOrElse("Service Is Missing")
      Ok(not_found_invitation(serviceMessageKey))

    case MultiConsent(clientType, uid, agentName, consents) =>
      val clientTypeStr = ClientType.fromEnum(clientType)
      Ok(
        confirm_terms_multi(
          formWithErrors.or(confirmTermsMultiForm),
          ConfirmTermsPageConfig(
            agentName,
            clientTypeStr,
            uid,
            consents,
            backLink = backLinkFor(breadcrumbs),
            submitUrl = routes.ClientInvitationJourneyController.submitConsent(),
            checkAnswersUrl = routes.ClientInvitationJourneyController.showCheckAnswers()
          )
        ))

    case SingleConsent(clientType, uid, agentName, consent, consents) =>
      Ok(
        confirm_terms_multi(
          formWithErrors.or(confirmTermsMultiForm),
          ConfirmTermsPageConfig(
            agentName,
            ClientType.fromEnum(clientType),
            uid,
            Seq(consent),
            backLink = backLinkFor(breadcrumbs),
            submitUrl = routes.ClientInvitationJourneyController.submitChangeConsents(),
            checkAnswersUrl = routes.ClientInvitationJourneyController.showCheckAnswers()
          )
        ))

    case CheckAnswers(clientType, uid, agentName, consents) =>
      Ok(
        check_answers(
          CheckAnswersPageConfig(
            consents,
            agentName,
            ClientType.fromEnum(clientType),
            uid,
            routes.ClientInvitationJourneyController.submitCheckAnswers(),
            (serviceKey: String) => routes.ClientInvitationJourneyController.submitCheckAnswersChange(serviceKey)
          )))

    case IncorrectClientType(clientType) => Ok(incorrect_client_type(ClientType.fromEnum(clientType)))

    case ConfirmDecline(clientType, uid, agentName, consents) =>
      Ok(
        confirm_decline(
          formWithErrors.or(confirmDeclineForm),
          ConfirmDeclinePageConfig(
            agentName,
            ClientType.fromEnum(clientType),
            uid,
            consents.map(_.serviceKey).distinct,
            submitUrl = routes.ClientInvitationJourneyController.submitConfirmDecline(),
            backLink = backLinkFor(breadcrumbs)
          )
        ))

    case InvitationsAccepted(agentName, consents) => Ok(complete(CompletePageConfig(agentName, consents)))

    case InvitationsDeclined(agentName, consents) =>
      Ok(invitation_declined(InvitationDeclinedPageConfig(agentName, consents.map(_.serviceKey).distinct)))

    case AllResponsesFailed => Ok(all_responses_failed())

    case SomeResponsesFailed(agentName, consents) =>
      Ok(
        some_responses_failed(
          SomeResponsesFailedPageConfig(
            consents,
            agentName,
            //this call is wrong, what should it be?
            routes.ClientInvitationJourneyController.showCheckAnswers())))
  }
}

object ClientInvitationJourneyController {

  val confirmTermsMultiForm: Form[ConfirmedTerms] =
    Form[ConfirmedTerms](
      mapping(
        "confirmedTerms.itsa" -> boolean,
        "confirmedTerms.afi"  -> boolean,
        "confirmedTerms.vat"  -> boolean
      )(ConfirmedTerms.apply)(ConfirmedTerms.unapply))

  def confirmationForm(errorMessage: String): Form[Confirmation] =
    Form(
      mapping(
        "accepted" -> optional(normalizedText)
          .transform[String](_.getOrElse(""), s => Some(s))
          .verifying(confirmationChoice(errorMessage))
      )(choice => Confirmation(choice.toBoolean))(confirmation => Some(confirmation.choice.toString)))

  val confirmDeclineForm = confirmationForm("error.confirmDecline.invalid")

}
