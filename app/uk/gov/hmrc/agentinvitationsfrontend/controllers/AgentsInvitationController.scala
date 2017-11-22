/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Named, Singleton}

import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.data.validation._
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentinvitationsfrontend.audit.AuditService
import uk.gov.hmrc.agentinvitationsfrontend.models.AgentInvitationUserInput
import uk.gov.hmrc.agentinvitationsfrontend.services.InvitationsService
import uk.gov.hmrc.agentinvitationsfrontend.views.html.agents._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.domain.Nino.isValid
import uk.gov.hmrc.http.Upstream4xxResponse
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

@Singleton
class AgentsInvitationController @Inject() (
  @Named("agent-invitations-frontend.base-url") externalUrl: String,
  invitationsService: InvitationsService,
  auditService: AuditService,
  val messagesApi: play.api.i18n.MessagesApi,
  val authConnector: AuthConnector)(implicit val configuration: Configuration)
  extends FrontendController with I18nSupport with AuthActions {

  import AgentsInvitationController._

  def agentsRoot: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.AgentsInvitationController.showNinoForm().url)
  }

  def showNinoForm: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      Future successful Ok(enter_nino(agentInvitationNinoForm))
    }
  }

  def submitNino: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      agentInvitationNinoForm.bindFromRequest().fold(
        formWithErrors => {
          Future successful Ok(enter_nino(formWithErrors))
        },
        userInput => {
          Future successful Redirect(routes.AgentsInvitationController.showPostcodeForm).withSession(request.session + ("nino" -> userInput.nino.value))
        })
    }
  }

  def showPostcodeForm: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      request.session.get("nino") match {
        case Some(nino) =>
          Future successful Ok(enter_postcode(agentInvitationNinoForm.fill(AgentInvitationUserInput(Nino(nino), ""))))
        case None =>
          Future successful Redirect(routes.AgentsInvitationController.showNinoForm())
      }
    }
  }

  def submitPostcode: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      agentInvitationPostCodeForm.bindFromRequest().fold(
        formWithErrors => {
          Future successful Ok(enter_postcode(formWithErrors))
        },
        userInput => {
          invitationsService
            .createInvitation(arn, userInput)
            .map(invitation => {
              val id = extractInvitationId(invitation.selfUrl.toString)
              auditService.sendAgentInvitationSubmitted(arn, id, userInput, "Success")
              Redirect(routes.AgentsInvitationController.invitationSent).withSession(request.session + ("invitationId" -> id))
            })
            .recoverWith {
              case noMtdItId: Upstream4xxResponse if noMtdItId.message.contains("CLIENT_REGISTRATION_NOT_FOUND") => {
                auditService.sendAgentInvitationSubmitted(arn, "", userInput, "Fail", Some("CLIENT_REGISTRATION_NOT_FOUND"))
                Future successful Redirect(routes.AgentsInvitationController.notEnrolled())
              }
              case noPostCode: Upstream4xxResponse if noPostCode.message.contains("POSTCODE_DOES_NOT_MATCH") => {
                auditService.sendAgentInvitationSubmitted(arn, "", userInput, "Fail", Some("POSTCODE_DOES_NOT_MATCH"))
                Future successful Redirect(routes.AgentsInvitationController.notMatched())
              }
              case e =>
                auditService.sendAgentInvitationSubmitted(arn, "", userInput, "Fail", Option(e.getMessage))
                Future.failed(e)
            }
        })
    }
  }

  def invitationSent: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { arn =>
      request.session.get("invitationId") match {
        case Some(invitationId) =>
          Future successful Ok(invitation_sent(s"$externalUrl${routes.ClientsInvitationController.start(invitationId)}"))
        case None => throw new RuntimeException("User attempted to browse to invitationSent")
      }
    }
  }

  def notEnrolled: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      Future successful Forbidden(not_enrolled())
    }
  }

  def notMatched: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedAsAgent { _ =>
      Future successful Forbidden(not_matched())
    }
  }

  private def extractInvitationId(url: String) = url.substring(url.lastIndexOf("/") + 1)

}

object AgentsInvitationController {

  private def postcodeRegex = "^[A-Z]{1,2}[0-9][0-9A-Z]?\\s?[0-9][A-Z]{2}$|BFPO\\s?[0-9]{1,5}$"

  private def validateField(failure: String)(condition: String => Boolean) = Constraint[String] { fieldValue: String =>
    Constraints.nonEmpty(fieldValue) match {
      case i: Invalid =>
        i
      case Valid =>
        if (condition(fieldValue.trim.toUpperCase))
          Valid
        else
          Invalid(ValidationError(failure))
    }
  }

  private val invalidNino =
    validateField("enter-nino.invalid-format")(nino => isValid(nino))
  private val invalidPostcode =
    validateField("enter-postcode.invalid-format")(postcode => postcode.matches(postcodeRegex))

  val agentInvitationNinoForm: Form[AgentInvitationUserInput] = {
    Form(mapping(
      "nino" -> text.verifying(invalidNino),
      "postcode" -> text)
    ({ (nino, postcode) => AgentInvitationUserInput(Nino(nino.trim.toUpperCase()), postcode) })
    ({ user => Some((user.nino.value, user.postcode)) }))
  }

  val agentInvitationPostCodeForm: Form[AgentInvitationUserInput] = {
    Form(mapping(
      "nino" -> text,
      "postcode" -> text.verifying(invalidPostcode))
    ({ (nino, postcode) => AgentInvitationUserInput(Nino(nino), postcode.trim.toUpperCase()) })
    ({ user => Some((user.nino.value, user.postcode)) }))
  }
}
