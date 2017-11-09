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

package uk.gov.hmrc.agentinvitationsfrontend.audit

import javax.inject.{ Inject, Singleton }

import play.api.mvc.Request
import uk.gov.hmrc.agentinvitationsfrontend.audit.AgentInvitationEvent.AgentInvitationEvent
import uk.gov.hmrc.agentinvitationsfrontend.models.AgentInvitationUserInput
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.Try

object AgentInvitationEvent extends Enumeration {
  val AgentClientInvitationSubmitted = Value
  type AgentInvitationEvent = Value
}

@Singleton
class AuditService @Inject() (val auditConnector: AuditConnector) {

  def sendAgentInvitationSubmitted(arn: Arn, invitationId: String, agentInvitationUserInput: AgentInvitationUserInput, result: String)(implicit hc: HeaderCarrier, request: Request[Any]): Unit = {
    auditEvent(AgentInvitationEvent.AgentClientInvitationSubmitted, "agent-client-invitation-submitted",
      Seq(
        "result" -> result,
        "invitationId" -> invitationId,
        "agentReferenceNumber" -> arn.value,
        "regimeId" -> agentInvitationUserInput.nino.value,
        "regime" -> "HMRC-MTD-IT"))
  }

  private[audit] def auditEvent(event: AgentInvitationEvent, transactionName: String, details: Seq[(String, Any)] = Seq.empty)(implicit hc: HeaderCarrier, request: Request[Any]): Future[Unit] = {
    send(createEvent(event, transactionName, details: _*))
  }

  private def createEvent(event: AgentInvitationEvent, transactionName: String, details: (String, Any)*)(implicit hc: HeaderCarrier, request: Request[Any]): DataEvent = {

    val detail = hc.toAuditDetails(details.map(pair => pair._1 -> pair._2.toString): _*)
    val tags = hc.toAuditTags(transactionName, request.path)
    DataEvent(
      auditSource = "agent-invitations-frontend",
      auditType = event.toString,
      tags = tags,
      detail = detail)
  }

  private def send(events: DataEvent*)(implicit hc: HeaderCarrier): Future[Unit] = {
    Future {
      events.foreach { event =>
        Try(auditConnector.sendEvent(event))
      }
    }
  }

}