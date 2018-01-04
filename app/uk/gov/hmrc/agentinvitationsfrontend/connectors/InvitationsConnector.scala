/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentinvitationsfrontend.connectors

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.joda.time.{DateTime, LocalDate}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentinvitationsfrontend.UriPathEncoding.encodePathSegment
import uk.gov.hmrc.agentinvitationsfrontend.models.{AgentInvitation, Invitation}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, MtdItId}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class InvitationsConnector @Inject() (
  @Named("agent-client-authorisation-baseUrl") baseUrl: URL,
  http: HttpGet with HttpPost with HttpPut,
  metrics: Metrics) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  import Reads._

  private[connectors] def createInvitationUrl(arn: Arn): URL =
    new URL(baseUrl, s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent")

  private[connectors] def acceptITSAInvitationUrl(mtdItId: MtdItId, invitationId: InvitationId): URL =
    new URL(baseUrl, s"/agent-client-authorisation/clients/MTDITID/${mtdItId.value}/invitations/received/${invitationId.value}/accept")

  private[connectors] def rejectITSAInvitationUrl(mtdItId: MtdItId, invitationId: InvitationId) =
    new URL(baseUrl, s"/agent-client-authorisation/clients/MTDITID/${mtdItId.value}/invitations/received/${invitationId.value}/reject")

  private def invitationUrl(location: String) = new URL(baseUrl, location)

  def createInvitation(arn: Arn, agentInvitation: AgentInvitation)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    monitor(s"ConsumedAPI-Agent-Create-Invitation-POST") {
      http.POST[AgentInvitation, HttpResponse](createInvitationUrl(arn).toString, agentInvitation) map { r =>
        r.header("location")
      }
    }
  }

  def getInvitation(location: String)(implicit hc: HeaderCarrier): Future[Invitation] = {
    monitor(s"ConsumedAPI-Get-Invitation-GET") {
      val url = invitationUrl(location)
      http.GET[Invitation](url.toString)
    }
  }

  def acceptITSAInvitation(mtdItId: MtdItId, invitationId: InvitationId)(implicit hc: HeaderCarrier): Future[Int] = {
    monitor(s"ConsumedAPI-Accept-Invitation-PUT") {
      http.PUT[Boolean, HttpResponse](acceptITSAInvitationUrl(mtdItId, invitationId).toString, false).map(_.status)
    }
  }

  def rejectITSAInvitation(mtdItId: MtdItId, invitationId: InvitationId)(implicit hc: HeaderCarrier): Future[Int] = {
    monitor(s"ConsumedAPI-Reject-Invitation-PUT") {
      http.PUT[Boolean, HttpResponse](rejectITSAInvitationUrl(mtdItId, invitationId).toString, false).map(_.status)
    }
  }

  private[connectors] def acceptAFIInvitationUrl(nino: Nino, invitationId: InvitationId): URL =
    new URL(baseUrl, s"/agent-client-authorisation/clients/NI/${nino.value}/invitations/received/${invitationId.value}/accept")

  private[connectors] def rejectAFIInvitationUrl(nino: Nino, invitationId: InvitationId) =
    new URL(baseUrl, s"/agent-client-authorisation/clients/NI/${nino.value}/invitations/received/${invitationId.value}/reject")


  def acceptAFIInvitation(nino: Nino, invitationId: InvitationId)(implicit hc: HeaderCarrier): Future[Int] = {
    monitor(s"ConsumedAPI-Accept-Invitation-PUT") {
      http.PUT[Boolean, HttpResponse](acceptAFIInvitationUrl(nino, invitationId).toString, false).map(_.status)
    }
  }

  def rejectAFIInvitation(nino: Nino, invitationId: InvitationId)(implicit hc: HeaderCarrier): Future[Int] = {
    monitor(s"ConsumedAPI-Reject-Invitation-PUT") {
      http.PUT[Boolean, HttpResponse](rejectAFIInvitationUrl(nino, invitationId).toString, false).map(_.status)
    }
  }

  object Reads {

    import play.api.libs.functional.syntax._
    import play.api.libs.json.{JsPath, Reads}
    import uk.gov.hmrc.domain.SimpleObjectReads
    import uk.gov.hmrc.http.controllers.RestFormats.dateTimeFormats

    implicit val reads: Reads[Invitation] = {
      implicit val urlReads = new SimpleObjectReads[URL]("href", s => new URL(baseUrl,s))
      (
        (JsPath \ "arn").read[Arn] and
          (JsPath \ "service").read[String] and
          (JsPath \ "clientId").read[String] and
          (JsPath \ "status").read[String] and
          (JsPath \ "created").read[DateTime] and
          (JsPath \ "lastUpdated").read[DateTime] and
          (JsPath \ "expiryDate").read[LocalDate] and
          (JsPath \ "_links" \ "self").read[URL])(Invitation.apply _)
    }
  }
}
