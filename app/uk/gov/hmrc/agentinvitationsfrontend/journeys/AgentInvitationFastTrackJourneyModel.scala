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

package uk.gov.hmrc.agentinvitationsfrontend.journeys

import org.joda.time.LocalDate
import play.api.Logging
import uk.gov.hmrc.agentinvitationsfrontend.journeys.AgentInvitationJourneyModel.Transitions.{CheckDOBMatches, GetCgtSubscription}
import uk.gov.hmrc.agentinvitationsfrontend.models.ClientType.{business, personal}
import uk.gov.hmrc.agentinvitationsfrontend.models.Services._
import uk.gov.hmrc.agentinvitationsfrontend.models._
import uk.gov.hmrc.agentinvitationsfrontend.validators.Validators.{urnPattern, utrPattern}
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.fsm.JourneyModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AgentInvitationFastTrackJourneyModel extends JourneyModel with Logging {

  sealed trait State

  val root: State = Prologue(None, None)

  case class Prologue(failureUrl: Option[String], refererUrl: Option[String]) extends State

  case class CheckDetailsNoPostcode(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsNoDob(originalFastTrackRequest: AgentFastTrackRequest, fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String])
      extends State

  case class CheckDetailsNoVatRegDate(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsNoClientTypeVat(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsCompleteItsa(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsCompleteIrv(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsCompletePersonalVat(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsCompleteBusinessVat(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsCompleteTrust(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class CheckDetailsCompleteCgt(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class NoPostcode(originalFastTrackRequest: AgentFastTrackRequest, fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String])
      extends State

  case class NoDob(originalFastTrackRequest: AgentFastTrackRequest, fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String])
      extends State

  case class NoVatRegDate(originalFastTrackRequest: AgentFastTrackRequest, fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String])
      extends State

  case class SelectClientTypeVat(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String],
    isChanging: Boolean = false)
      extends State

  case class SelectClientTypeCgt(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String],
    isChanging: Boolean = false
  ) extends State

  case class IdentifyPersonalClient(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class IdentifyBusinessClient(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class IdentifyNoClientTypeClient(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class IdentifyTrustClient(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class IdentifyCgtClient(originalFastTrackRequest: AgentFastTrackRequest, fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String])
      extends State

  case class ConfirmClientTrust(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String],
    trustName: String)
      extends State

  case class InvitationSentPersonal(invitationLink: String, continueUrl: Option[String], agencyEmail: String, service: String) extends State

  case class InvitationSentBusiness(invitationLink: String, continueUrl: Option[String], agencyEmail: String, service: String = HMRCMTDVAT)
      extends State

  case class PendingInvitationExists(fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String]) extends State

  case class ActiveAuthorisationExists(fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String]) extends State

  case class KnownFactNotMatched(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String])
      extends State

  case class ClientNotSignedUp(fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String]) extends State

  case class TrustNotFound(originalFastTrackRequest: AgentFastTrackRequest, fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String])
      extends State

  case object TryAgainWithoutFastTrack extends State

  case class ConfirmPostcodeCgt(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String],
    postcodeFromDes: Option[String],
    clientName: String)
      extends State

  case class ConfirmCountryCodeCgt(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String],
    countryCode: String,
    clientName: String)
      extends State

  case class ConfirmClientCgt(
    originalFastTrackRequest: AgentFastTrackRequest,
    fastTrackRequest: AgentFastTrackRequest,
    continueUrl: Option[String],
    clientName: String)
      extends State

  case class SuspendedAgent(service: String, continueUrl: Option[String]) extends State

  case class CgtRefNotFound(cgtRef: CgtRef) extends State

  object Transitions {
    type HasPendingInvitations = (Arn, String, String) => Future[Boolean]
    type HasActiveRelationship = (Arn, String, String) => Future[Boolean]
    type GetClientName = (String, String) => Future[Option[String]]
    type CheckPostcodeMatches = (Nino, String) => Future[Option[Boolean]]
    type CheckRegDateMatches = (Vrn, LocalDate) => Future[Option[Int]]
    type GetAgentLink = (Arn, Option[ClientType]) => Future[String]
    type CreateInvitation =
      (Arn, Invitation) => Future[InvitationId]
    type GetAgencyEmail = () => Future[String]

    type GetTrustName = TrustTaxIdentifier => Future[TrustResponse]
    type GetSuspensionDetails = () => Future[SuspensionDetails]

    def prologue(failureUrl: Option[String], refererUrl: Option[String]) = Transition {
      case _ => goto(Prologue(failureUrl, refererUrl))
    }

    def start(agentSuspensionEnabled: Boolean, getSuspensionDetails: GetSuspensionDetails)(continueUrl: Option[String])(agent: AuthorisedAgent)(
      fastTrackRequest: AgentFastTrackRequest) = Transition {
      case _ =>
        if (agentSuspensionEnabled) getSuspensionDetails().flatMap { suspensionDetails =>
          suspensionDetails.isRegimeSuspended(fastTrackRequest.service) match {
            case true  => goto(SuspendedAgent(fastTrackRequest.service, continueUrl))
            case false => getStateForNonSuspendedAgent(fastTrackRequest, continueUrl)
          }
        } else {
          getStateForNonSuspendedAgent(fastTrackRequest, continueUrl)
        }
    }

    def getStateForNonSuspendedAgent(fastTrackRequest: AgentFastTrackRequest, continueUrl: Option[String]): Future[State] =
      fastTrackRequest match {
        case AgentFastTrackRequest(_, HMRCMTDIT, _, _, knownFact) =>
          val updatedPersonalRequest = fastTrackRequest.copy(clientType = Some(personal))
          goto(knownFact.fold(CheckDetailsNoPostcode(updatedPersonalRequest, updatedPersonalRequest, continueUrl): State)(_ =>
            CheckDetailsCompleteItsa(updatedPersonalRequest, updatedPersonalRequest, continueUrl)))

        case AgentFastTrackRequest(_, HMRCPIR, _, _, knownFact) =>
          val updatedPersonalRequest = fastTrackRequest.copy(clientType = Some(personal))
          goto(knownFact.fold(CheckDetailsNoDob(updatedPersonalRequest, updatedPersonalRequest, continueUrl): State)(_ =>
            CheckDetailsCompleteIrv(updatedPersonalRequest, updatedPersonalRequest, continueUrl)))

        case AgentFastTrackRequest(Some(ClientType.personal), HMRCMTDVAT, _, _, knownFact) =>
          goto(knownFact.fold(CheckDetailsNoVatRegDate(fastTrackRequest, fastTrackRequest, continueUrl): State)(_ =>
            CheckDetailsCompletePersonalVat(fastTrackRequest, fastTrackRequest, continueUrl)))

        case AgentFastTrackRequest(Some(ClientType.business), HMRCMTDVAT, _, _, knownFact) =>
          goto(knownFact.fold(CheckDetailsNoVatRegDate(fastTrackRequest, fastTrackRequest, continueUrl): State)(_ =>
            CheckDetailsCompleteBusinessVat(fastTrackRequest, fastTrackRequest, continueUrl)))

        case AgentFastTrackRequest(None, HMRCMTDVAT, _, _, _) =>
          goto(CheckDetailsNoClientTypeVat(fastTrackRequest, fastTrackRequest, continueUrl))

        case AgentFastTrackRequest(Some(ClientType.business), TAXABLETRUST, _, _, _) =>
          goto(CheckDetailsCompleteTrust(fastTrackRequest, fastTrackRequest, continueUrl))
//here not sure if we should use ANYTRUST/TRUSTNT?
        case AgentFastTrackRequest(Some(ClientType.business), TRUST, _, _, _) =>
          goto(CheckDetailsCompleteTrust(fastTrackRequest, fastTrackRequest, continueUrl))

        case AgentFastTrackRequest(Some(ClientType.business), NONTAXABLETRUST, _, _, _) =>
          goto(CheckDetailsCompleteTrust(fastTrackRequest, fastTrackRequest, continueUrl))

        case AgentFastTrackRequest(_, HMRCCGTPD, _, _, _) =>
          goto(CheckDetailsCompleteCgt(fastTrackRequest, fastTrackRequest, continueUrl))

        case fastTrackRequest =>
          throw new RuntimeException(s"No state found for fast track request: $fastTrackRequest")
      }

    def checkIfPendingOrActiveAndGoto(fastTrackRequest: AgentFastTrackRequest, arn: Arn, invitation: Invitation, continueUrl: Option[String])(
      hasPendingInvitationsFor: HasPendingInvitations,
      hasActiveRelationshipFor: HasActiveRelationship
    )(createInvitation: CreateInvitation, getAgentLink: GetAgentLink, getAgencyEmail: GetAgencyEmail): Future[State] =
      for {
        hasPendingInvitations <- hasPendingInvitationsFor(arn, fastTrackRequest.clientIdentifier, fastTrackRequest.service)
        result <- if (hasPendingInvitations) {
                   goto(PendingInvitationExists(fastTrackRequest, continueUrl))
                 } else {
                   hasActiveRelationshipFor(arn, fastTrackRequest.clientIdentifier, fastTrackRequest.service)
                     .flatMap {
                       case true => goto(ActiveAuthorisationExists(fastTrackRequest, continueUrl))
                       case false =>
                         for {
                           agencyEmail    <- getAgencyEmail()
                           _              <- createInvitation(arn, invitation)
                           invitationLink <- getAgentLink(arn, fastTrackRequest.clientType)
                           result <- fastTrackRequest.clientType match {
                                      case Some(ClientType.personal) =>
                                        goto(InvitationSentPersonal(invitationLink, continueUrl, agencyEmail, fastTrackRequest.service))
                                      case Some(ClientType.business) =>
                                        goto(InvitationSentBusiness(invitationLink, continueUrl, agencyEmail, fastTrackRequest.service))
                                      case None =>
                                        throw new RuntimeException(s"No client type found for fast track request: $fastTrackRequest")
                                    }
                         } yield result
                     }
                 }
      } yield result

    def checkedDetailsNoClientType(agent: AuthorisedAgent) = Transition {
      case CheckDetailsNoClientTypeVat(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
        goto(SelectClientTypeVat(originalFastTrackRequest, fastTrackRequest, continueUrl))
      case NoVatRegDate(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
        goto(SelectClientTypeVat(originalFastTrackRequest, fastTrackRequest, continueUrl))
      case CheckDetailsCompleteCgt(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
        goto(SelectClientTypeCgt(originalFastTrackRequest, fastTrackRequest, continueUrl))
    }

    def checkedDetailsNoKnownFact(getCgtSubscription: GetCgtSubscription)(agent: AuthorisedAgent) =
      Transition {
        case CheckDetailsNoPostcode(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
          goto(NoPostcode(originalFastTrackRequest, fastTrackRequest, continueUrl))

        case CheckDetailsNoDob(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
          goto(NoDob(originalFastTrackRequest, fastTrackRequest, continueUrl))

        case CheckDetailsNoVatRegDate(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
          goto(NoVatRegDate(originalFastTrackRequest, fastTrackRequest, continueUrl))

        case CheckDetailsCompleteCgt(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
          val cgtRef = CgtRef(fastTrackRequest.clientIdentifier)

          getCgtSubscription(cgtRef).map {
            case Some(subscription) =>
              if (subscription.isUKBasedClient) {
                ConfirmPostcodeCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, subscription.postCode, subscription.name)
              } else {
                ConfirmCountryCodeCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, subscription.countryCode, subscription.name)
              }
            case None =>
              CgtRefNotFound(cgtRef)
          }
      }

    def confirmPostcodeCgt(agent: AuthorisedAgent)(postcode: Postcode): Transition =
      Transition {
        case ConfirmPostcodeCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, postcodeFromDes, name) =>
          if (postcodeFromDes.contains(postcode.value)) {
            goto(ConfirmClientCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, name))
          } else {
            goto(KnownFactNotMatched(originalFastTrackRequest, fastTrackRequest, continueUrl))
          }
      }

    def confirmCountryCodeCgt(agent: AuthorisedAgent)(countryCode: CountryCode): Transition =
      Transition {
        case ConfirmCountryCodeCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, countryCodeFromDes, name) =>
          if (countryCodeFromDes.contains(countryCode.value)) {
            goto(ConfirmClientCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, name))
          } else {
            goto(KnownFactNotMatched(originalFastTrackRequest, fastTrackRequest, continueUrl))
          }
      }

    def identifyCgtClient(getCgtSubscription: GetCgtSubscription)(agent: AuthorisedAgent)(cgtClient: CgtClient): Transition =
      Transition {
        case IdentifyCgtClient(originalFastTrackRequest, fastTrackRequest, continueUrl) =>
          getCgtSubscription(CgtRef(fastTrackRequest.clientIdentifier)) map {
            case Some(subscription) => {
              if (subscription.isUKBasedClient)
                ConfirmPostcodeCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, subscription.postCode, subscription.name)
              else
                ConfirmCountryCodeCgt(originalFastTrackRequest, fastTrackRequest, continueUrl, subscription.countryCode, subscription.name)
            }
            case None => CgtRefNotFound(CgtRef(fastTrackRequest.clientIdentifier))
          }
      }

    def checkedDetailsChangeInformation(agent: AuthorisedAgent): AgentInvitationFastTrackJourneyModel.Transition = {
      def gotoIdentifyClient(originalFtr: AgentFastTrackRequest, ftRequest: AgentFastTrackRequest, continueUrl: Option[String]) =
        if (ftRequest.clientType.contains(personal)) {
          goto(IdentifyPersonalClient(originalFtr, ftRequest, continueUrl))
        } else if (ftRequest.clientType.contains(business)) {
          goto(IdentifyBusinessClient(originalFtr, ftRequest, continueUrl))
        } else {
          goto(IdentifyNoClientTypeClient(originalFtr, ftRequest, continueUrl))
        }

      Transition {
        case CheckDetailsCompleteItsa(originalFtr, ftr, continueUrl) =>
          goto(IdentifyPersonalClient(originalFtr, ftr, continueUrl))

        case CheckDetailsCompleteIrv(originalFtr, ftr, continueUrl) =>
          goto(IdentifyPersonalClient(originalFtr, ftr, continueUrl))

        case CheckDetailsCompletePersonalVat(originalFtr, ftr, continueUrl) =>
          goto(IdentifyPersonalClient(originalFtr, ftr, continueUrl))

        case CheckDetailsCompleteBusinessVat(originalFtr, ftr, continueUrl) =>
          goto(IdentifyBusinessClient(originalFtr, ftr, continueUrl))

        case CheckDetailsCompleteTrust(originalFtr, ftr, continueUrl) =>
          goto(IdentifyTrustClient(originalFtr, ftr, continueUrl))

        case CheckDetailsCompleteCgt(originalFtr, ftr, continueUrl) =>
          if (ftr.clientType.isEmpty) goto(SelectClientTypeCgt(originalFtr, ftr, continueUrl, isChanging = true))
          else goto(IdentifyCgtClient(originalFtr, ftr, continueUrl))

        case CheckDetailsNoPostcode(originalFtr, ftr, continueUrl) =>
          goto(IdentifyPersonalClient(originalFtr, ftr, continueUrl))

        case CheckDetailsNoDob(originalFtr, ftr, continueUrl) =>
          goto(IdentifyPersonalClient(originalFtr, ftr, continueUrl))

        case CheckDetailsNoVatRegDate(originalFtr, ftr, continueUrl) =>
          gotoIdentifyClient(originalFtr, ftr, continueUrl)

        case CheckDetailsNoClientTypeVat(originalFtr, ftr, continueUrl) =>
          goto(SelectClientTypeVat(originalFtr, ftr, continueUrl, isChanging = true))
      }
    }

    def checkedDetailsAllInformation(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(
      checkRegDateMatches: CheckRegDateMatches)(createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(
      hasPendingInvitations: HasPendingInvitations)(hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(
      confirmation: Confirmation) = Transition {
      case CheckDetailsCompleteItsa(originalFtr, fastTrackRequest, continueUrl) => {
        if (confirmation.choice) {
          checkPostcodeMatches(Nino(fastTrackRequest.clientIdentifier), fastTrackRequest.knownFact.getOrElse(""))
            .flatMap {
              case Some(true) =>
                checkIfPendingOrActiveAndGoto(
                  fastTrackRequest,
                  agent.arn,
                  ItsaInvitation(Nino(fastTrackRequest.clientIdentifier)),
                  continueUrl
                )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
              case Some(false) => goto(KnownFactNotMatched(originalFtr, fastTrackRequest, continueUrl))
              case None        => goto(ClientNotSignedUp(fastTrackRequest, continueUrl))
            }
        } else goto(IdentifyPersonalClient(originalFtr, fastTrackRequest, continueUrl))
      }

      case CheckDetailsCompleteIrv(originalFtr, fastTrackRequest, continueUrl) =>
        if (confirmation.choice) {
          val knownFact = fastTrackRequest.knownFact.getOrElse("")
          checkDobMatches(Nino(fastTrackRequest.clientIdentifier), LocalDate.parse(knownFact))
            .flatMap {
              case Some(true) =>
                checkIfPendingOrActiveAndGoto(
                  fastTrackRequest,
                  agent.arn,
                  PirInvitation(Nino(fastTrackRequest.clientIdentifier)),
                  continueUrl
                )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
              case _ => goto(KnownFactNotMatched(originalFtr, fastTrackRequest, continueUrl))
            }
        } else goto(IdentifyPersonalClient(originalFtr, fastTrackRequest, continueUrl))

      case CheckDetailsCompletePersonalVat(originalFtr, fastTrackRequest, continueUrl) =>
        if (confirmation.choice) {
          val knownFact = fastTrackRequest.knownFact.getOrElse("")
          checkRegDateMatches(Vrn(fastTrackRequest.clientIdentifier), LocalDate.parse(knownFact))
            .flatMap {
              case Some(204) =>
                checkIfPendingOrActiveAndGoto(
                  fastTrackRequest,
                  agent.arn,
                  VatInvitation(Some(personal), Vrn(fastTrackRequest.clientIdentifier)),
                  continueUrl
                )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
              case Some(_) => goto(KnownFactNotMatched(originalFtr, fastTrackRequest, continueUrl))
              case None    => goto(ClientNotSignedUp(fastTrackRequest, continueUrl))
            }
        } else goto(IdentifyPersonalClient(originalFtr, fastTrackRequest, continueUrl))

      case CheckDetailsCompleteBusinessVat(originalFtr, fastTrackRequest, continueUrl) =>
        if (confirmation.choice) {
          val knownFact = fastTrackRequest.knownFact.getOrElse("")
          checkRegDateMatches(Vrn(fastTrackRequest.clientIdentifier), LocalDate.parse(knownFact))
            .flatMap {
              case Some(204) =>
                checkIfPendingOrActiveAndGoto(
                  fastTrackRequest,
                  agent.arn,
                  VatInvitation(Some(ClientType.business), Vrn(fastTrackRequest.clientIdentifier)),
                  continueUrl
                )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
              case Some(_) => goto(KnownFactNotMatched(originalFtr, fastTrackRequest, continueUrl))
              case None    => goto(ClientNotSignedUp(fastTrackRequest, continueUrl))
            }
        } else goto(IdentifyBusinessClient(originalFtr, fastTrackRequest, continueUrl))

      case CheckDetailsCompleteTrust(originalFtr, fastTrackRequest, continueUrl) =>
        if (confirmation.choice) {
          val trustInvitation = fastTrackRequest.clientIdentifier match {
            case utr if utr.matches(utrPattern) => TrustInvitation(Utr(utr))
            case urn if urn.matches(urnPattern) => TrustNTInvitation(Urn(urn))
          }
          checkIfPendingOrActiveAndGoto(
            fastTrackRequest,
            agent.arn,
            trustInvitation,
            continueUrl
          )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
        } else goto(IdentifyTrustClient(originalFtr, fastTrackRequest, continueUrl))

      case CheckDetailsCompleteCgt(originalFtr, fastTrackRequest, continueUrl) =>
        if (confirmation.choice) {
          checkIfPendingOrActiveAndGoto(
            fastTrackRequest,
            agent.arn,
            CgtInvitation(CgtRef(fastTrackRequest.clientIdentifier), fastTrackRequest.clientType),
            continueUrl
          )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
        } else goto(IdentifyCgtClient(originalFtr, fastTrackRequest, continueUrl))
    }

    def identifiedClientItsa(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(checkRegDateMatches: CheckRegDateMatches)(
      createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(hasPendingInvitations: HasPendingInvitations)(
      hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(itsaClient: ItsaClient) = Transition {
      case IdentifyPersonalClient(originalFtr, ftr, continueUrl) =>
        val newState = CheckDetailsCompleteItsa(
          originalFtr,
          ftr.copy(clientIdentifier = itsaClient.clientIdentifier, knownFact = Some(itsaClient.postcode)),
          continueUrl)
        checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
          hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
          .apply(newState)
    }

    def identifiedClientIrv(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(checkRegDateMatches: CheckRegDateMatches)(
      createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(hasPendingInvitations: HasPendingInvitations)(
      hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(irvClient: IrvClient) = Transition {
      case IdentifyPersonalClient(originalFtr, ftr, continueUrl) =>
        val newState =
          CheckDetailsCompleteIrv(originalFtr, ftr.copy(clientIdentifier = irvClient.clientIdentifier, knownFact = Some(irvClient.dob)), continueUrl)
        checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
          hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
          .apply(newState)
    }

    def identifiedClientVat(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(checkRegDateMatches: CheckRegDateMatches)(
      createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(hasPendingInvitations: HasPendingInvitations)(
      hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(vatClient: VatClient) = Transition {
      case IdentifyPersonalClient(originalFtr, ftr, continueUrl) =>
        val newState = CheckDetailsCompletePersonalVat(
          originalFtr,
          ftr.copy(clientIdentifier = vatClient.clientIdentifier, knownFact = Some(vatClient.registrationDate)),
          continueUrl)
        checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
          hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
          .apply(newState)
      case IdentifyBusinessClient(originalFtr, ftr, continueUrl) =>
        val newState = CheckDetailsCompleteBusinessVat(
          originalFtr,
          ftr.copy(clientIdentifier = vatClient.clientIdentifier, knownFact = Some(vatClient.registrationDate)),
          continueUrl)
        checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
          hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
          .apply(newState)

      case IdentifyNoClientTypeClient(originalFtr, ftr, continueUrl) =>
        val newState = CheckDetailsNoClientTypeVat(
          originalFtr,
          ftr.copy(clientIdentifier = vatClient.clientIdentifier, knownFact = Some(vatClient.registrationDate)),
          continueUrl)
        checkedDetailsNoClientType(agent).apply(newState)
    }

    def showConfirmTrustClient(getTrustName: GetTrustName)(agent: AuthorisedAgent)(trustClient: TrustClient) =
      Transition {
        case IdentifyTrustClient(originalFtr, ftr, continueUrl) =>
          getTrustName(trustClient.taxId).flatMap { trustResponse =>
            trustResponse.response match {
              case Right(TrustName(name)) =>
                goto(ConfirmClientTrust(originalFtr, ftr.copy(clientIdentifier = trustClient.taxId.value), continueUrl, name))
              case Left(invalidTrust) =>
                logger.warn(s"Des returned $invalidTrust response for utr: ${trustClient.taxId}")
                goto(TrustNotFound(originalFtr, ftr, continueUrl))
            }
          }
      }

    def submitConfirmTrustClient(createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(
      hasPendingInvitations: HasPendingInvitations)(hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(
      confirmation: Confirmation) =
      Transition {
        case ConfirmClientTrust(originalFtr, ftr, continueUrl, trustName) =>
          if (confirmation.choice) {
            val trustInvitation = ftr.clientIdentifier match {
              case utr if utr.matches(utrPattern) => TrustInvitation(Utr(utr))
              case urn if urn.matches(urnPattern) => TrustNTInvitation(Urn(urn))
            }
            checkIfPendingOrActiveAndGoto(
              ftr,
              agent.arn,
              trustInvitation,
              continueUrl
            )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
          } else {
            goto(IdentifyTrustClient(originalFtr, ftr, continueUrl))
          }
      }

    def submitConfirmClientCgt(createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(
      hasPendingInvitations: HasPendingInvitations)(hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(
      confirmation: Confirmation) =
      Transition {
        case ConfirmClientCgt(originalFtr, ftr, continueUrl, cgtName) =>
          if (confirmation.choice) {
            checkIfPendingOrActiveAndGoto(
              ftr,
              agent.arn,
              CgtInvitation(CgtRef(ftr.clientIdentifier), ftr.clientType),
              continueUrl
            )(hasPendingInvitations, hasActiveRelationship)(createInvitation, getAgentLink, getAgencyEmail)
          } else {
            goto(IdentifyCgtClient(originalFtr, ftr, continueUrl))
          }
      }

    def moreDetailsItsa(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(checkRegDateMatches: CheckRegDateMatches)(
      createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(hasPendingInvitations: HasPendingInvitations)(
      hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(suppliedKnownFact: String) = Transition {
      case NoPostcode(originalFtr, ftr, continueUrl) =>
        val newState = CheckDetailsCompleteItsa(originalFtr, ftr.copy(knownFact = Some(suppliedKnownFact)), continueUrl)
        checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
          hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
          .apply(newState)
    }

    def moreDetailsIrv(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(checkRegDateMatches: CheckRegDateMatches)(
      createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(hasPendingInvitations: HasPendingInvitations)(
      hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(suppliedKnownFact: String) =
      Transition {
        case NoDob(originalFtr, ftr, continueUrl) =>
          val newState =
            CheckDetailsCompleteIrv(originalFtr, ftr.copy(knownFact = Some(suppliedKnownFact)), continueUrl)
          checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
            hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
            .apply(newState)
      }

    def moreDetailsVat(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(checkRegDateMatches: CheckRegDateMatches)(
      createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(hasPendingInvitations: HasPendingInvitations)(
      hasActiveRelationship: HasActiveRelationship)(agent: AuthorisedAgent)(suppliedKnownFact: String) =
      Transition {
        case NoVatRegDate(originalFtr, ftRequest, continueUrl) => {
          val checkDetailsState =
            if (ftRequest.clientType.contains(personal))
              CheckDetailsCompletePersonalVat
            else
              CheckDetailsCompleteBusinessVat

          val newState =
            checkDetailsState(originalFtr, ftRequest.copy(knownFact = Some(suppliedKnownFact)), continueUrl)

          checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
            hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
            .apply(newState)
        }
      }

    def tryAgainNotMatchedKnownFact(agent: AuthorisedAgent) =
      Transition {
        case KnownFactNotMatched(originalFtr, fastTrackRequest, continueUrl) =>
          val ftrWithoutKF = fastTrackRequest.copy(knownFact = None)

          def stateForMissingKnownFact(forService: String) =
            forService match {
              case HMRCMTDVAT | HMRCMTDIT | HMRCPIR => IdentifyPersonalClient(originalFtr, ftrWithoutKF, continueUrl)
            }

          val tryAgainState = fastTrackRequest match {
            case AgentFastTrackRequest(None, HMRCMTDVAT, _, _, _) =>
              SelectClientTypeVat(originalFtr, fastTrackRequest, continueUrl)

            case AgentFastTrackRequest(_, HMRCCGTPD, _, _, _) =>
              SelectClientTypeCgt(originalFtr, fastTrackRequest, continueUrl)

            case AgentFastTrackRequest(_, service, _, _, _) =>
              stateForMissingKnownFact(service)
          }

          goto(tryAgainState)

        case TrustNotFound(originalFtr, fastTrackRequest, continueUrl) =>
          goto(IdentifyTrustClient(originalFtr, fastTrackRequest, continueUrl))
      }

    def selectedClientType(checkPostcodeMatches: CheckPostcodeMatches)(checkDobMatches: CheckDOBMatches)(checkRegDateMatches: CheckRegDateMatches)(
      createInvitation: CreateInvitation)(getAgentLink: GetAgentLink)(getAgencyEmail: GetAgencyEmail)(hasPendingInvitations: HasPendingInvitations)(
      hasActiveRelationship: HasActiveRelationship)(getCgtSubscription: GetCgtSubscription)(agent: AuthorisedAgent)(suppliedClientType: String) =
      Transition {
        case SelectClientTypeVat(originalFtr, ftr, continueUrl, isChanging) =>
          val isKnownFactRequired = ftr.knownFact.isDefined
          if (isKnownFactRequired) {
            val completeState =
              if (ftr.clientType.contains(personal)) CheckDetailsCompletePersonalVat
              else CheckDetailsCompleteBusinessVat
            val newState =
              completeState(originalFtr, ftr.copy(clientType = Some(ClientType.toEnum(suppliedClientType))), continueUrl)
            checkedDetailsAllInformation(checkPostcodeMatches)(checkDobMatches)(checkRegDateMatches)(createInvitation)(getAgentLink)(getAgencyEmail)(
              hasPendingInvitations)(hasActiveRelationship)(agent)(Confirmation(true))
              .apply(newState)
          } else {
            val newState =
              CheckDetailsNoVatRegDate(originalFtr, ftr.copy(clientType = Some(ClientType.toEnum(suppliedClientType))), continueUrl)
            if (isChanging) checkedDetailsChangeInformation(agent).apply(newState)
            else checkedDetailsNoKnownFact(getCgtSubscription)(agent).apply(newState)
          }

        case SelectClientTypeCgt(originalFtr, ftr, continueUrl, isChanging) =>
          getCgtSubscription(CgtRef(ftr.clientIdentifier)).map {
            case Some(subscription) =>
              val newFtr = ftr.copy(clientType = Some(if (suppliedClientType == "trust") business else personal))
              if (isChanging) {
                IdentifyCgtClient(originalFtr, newFtr, continueUrl)
              } else if (subscription.isUKBasedClient) {
                ConfirmPostcodeCgt(originalFtr, newFtr, continueUrl, subscription.postCode, subscription.name)
              } else {
                ConfirmCountryCodeCgt(originalFtr, newFtr, continueUrl, subscription.countryCode, subscription.name)
              }
            case None =>
              CgtRefNotFound(CgtRef(ftr.clientIdentifier))
          }
      }

  }
}
