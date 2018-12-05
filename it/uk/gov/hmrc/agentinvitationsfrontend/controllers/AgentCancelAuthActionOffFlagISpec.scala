package uk.gov.hmrc.agentinvitationsfrontend.controllers

import com.google.inject.AbstractModule
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.agentinvitationsfrontend.services.{ContinueUrlCache, CurrentAuthorisationRequestCache}
import uk.gov.hmrc.agentinvitationsfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class AgentCancelAuthActionOffFlagISpec extends BaseISpec {

  override protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                                     -> wireMockPort,
        "microservice.services.agent-client-authorisation.port"               -> wireMockPort,
        "microservice.services.agent-client-relationships.port"               -> wireMockPort,
        "microservice.services.agent-services-account.port"                   -> wireMockPort,
        "microservice.services.company-auth.login-url"                        -> wireMockHost,
        "microservice.services.company-auth.port"                             -> wireMockPort,
        "microservice.services.des.port"                                      -> wireMockPort,
        "microservice.services.agent-fi-relationship.port"                    -> wireMockPort,
        "microservice.services.citizen-details.host"                          -> wireMockHost,
        "microservice.services.citizen-details.port"                          -> wireMockPort,
        "microservice.services.agent-invitations-frontend.external-url"       -> wireMockBaseUrlAsString,
        "microservice.services.agent-services-account-frontend.external-url"  -> wireMockBaseUrlAsString,
        "microservice.services.company-auth-frontend.external-url"            -> companyAuthUrl,
        "microservice.services.company-auth-frontend.sign-out.path"           -> companyAuthSignOutPath,
        "microservice.services.business-tax-account.external-url"             -> businessTaxAccountUrl,
        "microservice.services.tax-account-router-frontend.account-url"       -> taxAccountRelativeUrl,
        "microservice.services.personal-tax-account.external-url"             -> personalTaxAccountUrl,
        "microservice.services.citizen-details.host"                          -> wireMockHost,
        "microservice.services.citizen-details.port"                          -> wireMockPort,
        "auditing.enabled"                                                    -> true,
        "auditing.consumer.baseUri.host"                                      -> wireMockHost,
        "auditing.consumer.baseUri.port"                                      -> wireMockPort,
        "features.show-hmrc-mtd-it"                                           -> true,
        "features.show-personal-income"                                       -> true,
        "features.show-hmrc-mtd-vat"                                          -> true,
        "features.show-kfc-mtd-it"                                            -> true,
        "features.show-kfc-personal-income"                                   -> true,
        "features.show-kfc-mtd-vat"                                           -> true,
        "features.enable-fast-track"                                          -> true,
        "features.enable-track-requests"                                      -> true,
        "features.enable-track-cancel-auth-action"                                   -> false,
        "microservice.services.agent-subscription-frontend.external-url"      -> "someSubscriptionExternalUrl",
        "microservice.services.agent-client-management-frontend.external-url" -> "someAgentClientManagementFrontendExternalUrl"
      )
      .overrides(new TestGuiceModule)

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[CurrentAuthorisationRequestCache]).toInstance(testCurrentAuthorisationRequestCache)
      bind(classOf[ContinueUrlCache]).toInstance(testContinueUrlKeyStoreCache)
    }
  }

  lazy val requestTrackingController: AgentsRequestTrackingController = app.injector.instanceOf[AgentsRequestTrackingController]
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session12345")))

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    testCurrentAuthorisationRequestCache.clear()
    testContinueUrlKeyStoreCache.clear()
  }

  "GET /track/" should {

    val request = FakeRequest("GET", "/track/")
    val showTrackRequests = requestTrackingController.showTrackRequests

    "render a page without cancel authorisation link when flag is off" in {
      givenGetInvitations(arn)
      givenInactiveITSARelationships(arn)
      givenInactiveVATRelationships(arn)
      givenInactiveAfiRelationship(arn)
      givenNinoForMtdItId(MtdItId("JKKL80894713304"), Nino("AB123456A"))
      givenNinoForMtdItId(MtdItId("ABCDE1234567890"), Nino("AB123456A"))
      givenTradingName(Nino("AB123456A"), "FooBar Ltd.")
      givenCitizenDetailsAreKnownFor("AB123456B", "John", "Smith")
      givenCitizenDetailsAreKnownFor("GZ753451B", "Cosmo", "Kramer")
      givenCitizenDetailsAreKnownFor("AB123456A", "Rodney", "Jones")
      givenClientDetails(Vrn("101747696"))
      givenClientDetails(Vrn("101747641"))
      val result = showTrackRequests(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 200
      checkHtmlResultWithBodyText(
        result,
        "Accepted by client",
        "Client has not yet responded",
        "Declined by client",
        "Request expired as client did not respond in time",
        "You cancelled this request",
        "You cancelled your authorisation",
        "FooBar Ltd.",
        "John Smith",
        "Cosmo Kramer",
        "GDT",
        "11 September 2018",
        "21 September 2015",
        "24 September 2018",
        "01 January 2099",
        "Resend request to client",
        "Cancel this request",
        "Start new request",
        htmlEscapedMessage("recent-invitations.description", 30)
      )
      checkHtmlResultWithNotBodyText(result, "Cancel your authorisation")
      checkHtmlResultWithBodyMsgs(
        result,
        "recent-invitations.header",
        "recent-invitations.table-row-header.clientName",
        "recent-invitations.table-row-header.service",
        "recent-invitations.table-row-header.status",
        "recent-invitations.table-row-header.actions",
        "recent-invitations.invitation.service.HMRC-MTD-IT",
        "recent-invitations.invitation.service.HMRC-MTD-VAT",
        "recent-invitations.invitation.service.PERSONAL-INCOME-RECORD"
      )

    }
  }

}
