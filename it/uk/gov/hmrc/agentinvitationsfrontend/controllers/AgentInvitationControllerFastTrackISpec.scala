package uk.gov.hmrc.agentinvitationsfrontend.controllers

import org.joda.time.LocalDate
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentinvitationsfrontend.controllers.AgentsInvitationController._
import uk.gov.hmrc.agentinvitationsfrontend.models.{CurrentInvitationInput, UserInputNinoAndPostcode, UserInputVrnAndRegDate}
import uk.gov.hmrc.agentinvitationsfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, MtdItId, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import play.api.test.Helpers._
import uk.gov.hmrc.agentinvitationsfrontend.audit.AgentInvitationEvent

import scala.concurrent.ExecutionContext.Implicits.global

class AgentInvitationControllerFastTrackISpec extends BaseISpec {

  lazy val controller: AgentsInvitationController = app.injector.instanceOf[AgentsInvitationController]
  val arn = Arn("TARN0000001")
  val mtdItId = MtdItId("ABCDEF123456789")
  private val validNino = Nino("AB123456A")
  private val validNinoSpace = Nino("AB 12 34 56 A")
  val serviceITSA = "HMRC-MTD-IT"
  val servicePIR = "PERSONAL-INCOME-RECORD"
  val validPostcode = "DH14EJ"
  val invitationIdITSA = InvitationId("ABERULMHCKKW3")
  val invitationIdPIR = InvitationId("B9SCS2T4NZBAX")

  val invitationIdVAT = InvitationId("CZTW1KY6RTAAT")
  val serviceVAT = "HMRC-MTD-VAT"
  val identifierVAT = "VRN"
  val validVrn97 = Vrn("101747696")
  val invalidVrn = Vrn("101747692")
  val validRegDateForVrn97 = Some("2007-07-07")
  val validVrn9755 = Vrn("101747641")
  val fromFastTrack: Boolean = true
  val fromManual: Boolean = false

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session12345")))

  //TODO - Along with the other tests, could be split up by service? Note: This test is based on fast-track scenarios

  "POST /agents/select-service" should {
    val request = FakeRequest("POST", "/agents/select-service")
    val submitService = controller.submitService()

    "return 303 for authorised Agent with valid Nino and Known Fact, then selected ITSA, redirect to invitation-sent" in {
      testFastTrackCache.save(
        CurrentInvitationInput(None, Some("ni"), Some(validNino.value), Some(validPostcode), None, fromFastTrack))
      createInvitationStubWithKnownFacts(
        arn,
        mtdItId.value,
        invitationIdITSA,
        validNino.value,
        serviceITSA,
        "NI",
        Some(validPostcode))
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      getInvitationStub(arn, mtdItId.value, invitationIdITSA, serviceITSA, "NI", "Pending")
      val serviceForm = agentInvitationServiceForm.fill(UserInputNinoAndPostcode(serviceITSA, None, None))
      val result =
        submitService(authorisedAsValidAgent(request.withFormUrlEncodedBody(serviceForm.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/invitation-sent")
      verifyAuthoriseAttempt()
    }

    "return 303 for authorised Agent with valid Nino then selected IRV, redirect to invitation-sent" in {
      testFastTrackCache.save(CurrentInvitationInput(None, Some("ni"), Some(validNino.value), None, None, fromFastTrack))
      createInvitationStubForNoKnownFacts(
        arn,
        validNino.value,
        invitationIdPIR,
        validNino.value,
        "ni",
        servicePIR,
        "NI")
      getInvitationStub(arn, validNino.value, invitationIdPIR, servicePIR, "NI", "Pending")
      val serviceForm = agentInvitationServiceForm.fill(UserInputNinoAndPostcode(servicePIR, None, None))
      val result =
        submitService(authorisedAsValidAgent(request.withFormUrlEncodedBody(serviceForm.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/invitation-sent")
      verifyAuthoriseAttempt()
    }

    "return 303 for authorised Agent with valid VAT Information and selected VAT, redirect to invitation-sent" in {
      testFastTrackCache.save(
        CurrentInvitationInput(None, Some("vrn"), Some(validVrn97.value), None, validRegDateForVrn97, fromFastTrack))
      createInvitationStubForNoKnownFacts(
        arn,
        validVrn97.value,
        invitationIdVAT,
        validVrn97.value,
        "vrn",
        serviceVAT,
        identifierVAT)
      getInvitationStub(arn, validVrn97.value, invitationIdVAT, serviceVAT, identifierVAT, "Pending")
      checkVatRegisteredClientStub(validVrn97, LocalDate.parse("2007-07-07"), 204)
      val serviceForm = agentInvitationServiceForm.fill(UserInputNinoAndPostcode(serviceVAT, None, None))
      val result =
        submitService(authorisedAsValidAgent(request.withFormUrlEncodedBody(serviceForm.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/invitation-sent")
      verifyAuthoriseAttempt()
    }

    "return 303 for authorised Agent with valid VAT Information but selected ITSA, redirect to /agents/identify-client" in {
      testFastTrackCache.save(
        CurrentInvitationInput(None, Some("vrn"), Some(validVrn97.value), None, validRegDateForVrn97, fromFastTrack))
      createInvitationStubForNoKnownFacts(
        arn,
        validVrn97.value,
        invitationIdVAT,
        validVrn97.value,
        "vrn",
        serviceVAT,
        identifierVAT)
      getInvitationStub(arn, validVrn97.value, invitationIdVAT, serviceVAT, identifierVAT, "Pending")
      checkVatRegisteredClientStub(validVrn97, LocalDate.parse("2007-07-07"), 204)
      val serviceForm = agentInvitationServiceForm.fill(UserInputNinoAndPostcode(serviceITSA, None, None))
      val result =
        submitService(authorisedAsValidAgent(request.withFormUrlEncodedBody(serviceForm.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/identify-client")
      verifyAuthoriseAttempt()
    }

    "return 303 for authorised Agent with valid Nino but selected VAT, redirect to identify-client" in {
      testFastTrackCache.save(CurrentInvitationInput(None, Some("ni"), Some(validNino.value), None, None, fromFastTrack))
      createInvitationStubForNoKnownFacts(
        arn,
        validNino.value,
        invitationIdPIR,
        validNino.value,
        "ni",
        servicePIR,
        "NI")
      getInvitationStub(arn, validNino.value, invitationIdPIR, servicePIR, "NI", "Pending")
      val serviceForm = agentInvitationServiceForm.fill(UserInputNinoAndPostcode(serviceVAT, None, None))
      val result =
        submitService(authorisedAsValidAgent(request.withFormUrlEncodedBody(serviceForm.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/identify-client")
      verifyAuthoriseAttempt()
    }

  }

  "POST /agents/fast-track" should {
    val request = FakeRequest("POST", "/agents/fast-track")
    val fastTrack = controller.agentFastTrack()

    "return 303 invitation-sent if service calling fast-track is correct for ITSA" in {
      val formData =
        CurrentInvitationInput(Some(serviceITSA), Some("ni"), Some(validNino.value), Some(validPostcode), None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      createInvitationStubWithKnownFacts(
        arn,
        mtdItId.value,
        invitationIdITSA,
        validNino.value,
        serviceITSA,
        "MTDITID",
        Some(validPostcode))
      givenMatchingClientIdAndPostcode(validNino, validPostcode)
      getInvitationStub(arn, mtdItId.value, invitationIdITSA, serviceITSA, "MTDITID", "Pending")
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.invitationSent().url
    }

    "return 303 invitation-sent if service calling fast-track is correct for VAT" in {
      val formData =
        CurrentInvitationInput(Some(serviceVAT), Some("vrn"), Some(validVrn97.value), None, validRegDateForVrn97, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      createInvitationStubForNoKnownFacts(
        arn,
        validVrn97.value,
        invitationIdVAT,
        validVrn97.value,
        "vrn",
        serviceVAT,
        "VRN")
      getInvitationStub(arn, validVrn97.value, invitationIdVAT, serviceVAT, "VRN", "Pending")
      checkVatRegisteredClientStub(validVrn97, LocalDate.parse("2007-07-07"), 204)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.invitationSent().url
    }

    "return 303 invitation-sent if service calling fast-track is correct for IRV" in {
      val formData = CurrentInvitationInput(Some(servicePIR), Some("ni"), Some(validNino.value), None, None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      createInvitationStubForNoKnownFacts(
        arn,
        validNino.value,
        invitationIdPIR,
        validNino.value,
        "ni",
        servicePIR,
        "NI")
      getInvitationStub(arn, validNino.value, invitationIdPIR, servicePIR, "NI", "Pending")
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.invitationSent().url
    }

    "return 303 select-service if service calling fast-track does not have supported service in payload" in {
      val formData = CurrentInvitationInput("INVALID_SERVICE").copy(fromFastTrack = fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.selectService().url
    }

    "return 303 identify-client if service calling fast-track for ITSA contains invalid nino" in {
      val formData = CurrentInvitationInput(Some(serviceITSA), Some("ni"), Some("INVALID_NINO"), None, None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track for PIR contains invalid nino" in {
      val formData = CurrentInvitationInput(Some(servicePIR), Some("ni"), Some("INVALID_NINO"), None, None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track for VAT contains invalid vrn" in {
      val formData = CurrentInvitationInput(Some(serviceVAT), None, Some("INVALID_VRN"), None, None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track for ITSA does not contain nino" in {
      val formData = CurrentInvitationInput(serviceITSA).copy(fromFastTrack = fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track for IRV does not contain nino" in {
      val formData = CurrentInvitationInput(servicePIR).copy(fromFastTrack = fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track for VAT does not contain vrn" in {
      val formData = CurrentInvitationInput(serviceVAT).copy(fromFastTrack = fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track for does not contain postcode for ITSA" in {
      val formData = CurrentInvitationInput(Some(serviceITSA), Some("ni"), Some(validNino.value), None, None, fromFastTrack )
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track contains invalid postcode for ITSA" in {
      val formData =
        CurrentInvitationInput(Some(serviceITSA), Some("ni"), Some(validNino.value), Some("Invalid_Postcode"), None, fromFastTrack  )
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track does not contain vat-reg-date for VAT" in {
      val formData = CurrentInvitationInput(Some(serviceVAT), Some("vrn"), Some(validVrn97.value), None, None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 identify-client if service calling fast-track contains invalid vat-reg-date for VAT" in {
      val formData =
        CurrentInvitationInput(Some(serviceVAT), Some("vrn"), Some(validVrn97.value), None, Some("Invalid_Reg_Date"), fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClientForm().url
    }

    "return 303 select-service if there is no service but all other fields are valid for ITSA" in {
      val formData = CurrentInvitationInput(None, Some("ni"), Some(validNino.value), Some(validPostcode), None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.selectService().url
    }

    "return 303 select-service if there is no service but all other fields are valid for IRV" in {
      val formData = CurrentInvitationInput(None, Some("ni"), Some(validNino.value), None, None, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.selectService().url
    }

    "return 303 select-service if there is no service but all other fields are valid for VAT" in {
      val formData = CurrentInvitationInput(None, Some("vrn"), Some(validVrn97.value), None, validRegDateForVrn97, fromFastTrack)
      val fastTrackFormData = agentFastTrackForm.fill(formData)
      val result = fastTrack(
        authorisedAsValidAgent(request, arn.value)
          .withFormUrlEncodedBody(fastTrackFormData.data.toSeq: _*))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.selectService().url
    }

    "return 303 not-matched if nino and postcode does not match for ITSA" in {
      val formData = CurrentInvitationInput(Some(serviceITSA), Some("ni"), Some(validNino.value), Some(validPostcode), None, fromFastTrack)
      testFastTrackCache.save(formData)
      testFastTrackCache.currentSession.currentInvitationInput.get shouldBe formData
      givenNonMatchingClientIdAndPostcode(validNino, validPostcode)

      val form = agentFastTrackForm.fill(formData)
      val result = fastTrack(authorisedAsValidAgent(request.withFormUrlEncodedBody(form.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/not-matched")

      verifyAuthoriseAttempt()
      verifyAgentClientInvitationSubmittedEvent(arn.value, validNino.value, "ni", "Fail", serviceITSA)
      await(testFastTrackCache.fetch()).get shouldBe formData
    }

    "return 303 not-enrolled if Agent attempts to invite client who does not have an ITSA enrolment" in {
      val formData = CurrentInvitationInput(Some(serviceITSA), Some("ni"), Some(validNino.value), Some(validPostcode), None, fromFastTrack)
      testFastTrackCache.save(formData)
      testFastTrackCache.currentSession.currentInvitationInput.get shouldBe formData
      givenNotEnrolledClient(validNino, validPostcode)

      val form = agentFastTrackForm.fill(formData)
      val result = fastTrack(authorisedAsValidAgent(request.withFormUrlEncodedBody(form.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/not-enrolled")

      verifyAuthoriseAttempt()
      verifyAgentClientInvitationSubmittedEvent(arn.value, validNino.value, "ni", "Fail", serviceITSA)
      await(testFastTrackCache.fetch()).get shouldBe formData
    }

    "return 303 not-matched if vrn and vat-reg-date does not match for VAT" in {
      val invitation =
        CurrentInvitationInput(Some(serviceVAT), Some("vrn"), Some(validVrn97.value), None, validRegDateForVrn97, fromFastTrack)
      testFastTrackCache.save(invitation)
      checkVatRegisteredClientStub(validVrn97, LocalDate.parse("2007-07-07"), 403)

      val form = agentFastTrackForm.fill(invitation)
      val result = fastTrack(authorisedAsValidAgent(request.withFormUrlEncodedBody(form.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      header("Set-Cookie", result) shouldBe None
      redirectLocation(result) shouldBe Some("/invitations/agents/not-matched")
      verifyCheckVatRegisteredClientStubAttempt(validVrn97, LocalDate.parse("2007-07-07"))
      await(testFastTrackCache.fetch()).get shouldBe invitation
    }

    "return 303 not-enrolled if Agent attempted to invite a client for VAT" in {
      val invitation =
        CurrentInvitationInput(Some(serviceVAT), Some("vrn"), Some(validVrn97.value), None, validRegDateForVrn97, fromFastTrack)
      testFastTrackCache.save(invitation)
      checkVatRegisteredClientStub(validVrn97, LocalDate.parse("2007-07-07"), 404)

      val form = agentFastTrackForm.fill(invitation)
      val result = fastTrack(authorisedAsValidAgent(request.withFormUrlEncodedBody(form.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      header("Set-Cookie", result) shouldBe None
      redirectLocation(result) shouldBe Some("/invitations/agents/not-enrolled")
      verifyCheckVatRegisteredClientStubAttempt(validVrn97, LocalDate.parse("2007-07-07"))
      await(testFastTrackCache.fetch()).get shouldBe invitation

    }
  }

  def verifyAgentClientInvitationSubmittedEvent(
    arn: String,
    clientId: String,
    clientIdType: String,
    result: String,
    service: String): Unit =
    verifyAuditRequestSent(
      1,
      AgentInvitationEvent.AgentClientAuthorisationRequestCreated,
      detail = Map(
        "factCheck"            -> result,
        "agentReferenceNumber" -> arn,
        "clientIdType"         -> clientIdType,
        "clientId"             -> clientId,
        "service"              -> service
      ),
      tags = Map(
        "transactionName" -> "Agent client service authorisation request created"
      )
    )
}