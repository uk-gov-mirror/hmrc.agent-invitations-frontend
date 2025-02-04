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

package journeys
import play.api.libs.json.{Format, JsArray, Json}
import uk.gov.hmrc.agentinvitationsfrontend.journeys.AgentInvitationJourneyModel.State
import uk.gov.hmrc.agentinvitationsfrontend.journeys.AgentInvitationJourneyModel._
import uk.gov.hmrc.agentinvitationsfrontend.journeys.AgentInvitationJourneyStateFormats
import uk.gov.hmrc.agentinvitationsfrontend.models.ClientType.{business, personal}
import uk.gov.hmrc.agentinvitationsfrontend.models.Services.{HMRCCGTPD, HMRCMTDIT, HMRCMTDVAT, HMRCPIR, TAXABLETRUST}
import uk.gov.hmrc.agentinvitationsfrontend.models._
import uk.gov.hmrc.agentmtdidentifiers.model.{CgtRef, Utr, Vrn}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec

class AgentInvitationJourneyStateFormatsSpec extends UnitSpec {

  implicit val formats: Format[State] = AgentInvitationJourneyStateFormats.formats

  "AgentsInvitationJourneyFormats" should {

    "serialize and deserialize state" when {

      "ConfirmClientCgt" in {
        val state = ConfirmClientCgt(AuthorisationRequest("Sylvia Plath", CgtInvitation(CgtRef("123456"), Some(business)), itemId = "ABC"), Set.empty)
        val json = Json.parse("""{
                                |"state":"ConfirmClientCgt",
                                |"properties":
                                |   {"request":
                                |     {"clientName":"Sylvia Plath","invitation":
                                |       {"type":"CgtInvitation","data":
                                |         {"clientType":"business",
                                |         "service":"HMRC-CGT-PD",
                                |         "clientIdentifier":"123456",
                                |         "clientIdentifierType":"CGTPDRef"
                                |       }
                                |     },
                                |    "state":"New",
                                |    "itemId":"ABC"
                                |    },
                                |  "basket":[]
                                |  }
                                |}""".stripMargin)
        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "CgtRefNotFound" in {
        Json.toJson(CgtRefNotFound(CgtRef("cgtRef"), Set.empty)) shouldBe Json
          .obj("state" -> "CgtRefNotFound", "properties" -> Json.obj("cgtRef" -> "cgtRef", "basket" -> JsArray()))
        Json
          .parse("""{"state":"CgtRefNotFound", "properties": {"cgtRef": "cgtRef", "basket": []}}""")
          .as[State] shouldBe CgtRefNotFound(CgtRef("cgtRef"), Set.empty)
      }

      "SelectClientType" in {
        Json.toJson(SelectClientType(Set.empty)) shouldBe Json
          .obj("state" -> "SelectClientType", "properties" -> Json.obj("basket" -> JsArray()))
        Json
          .parse("""{"state":"SelectClientType", "properties": {"basket": []}}""")
          .as[State] shouldBe SelectClientType(Set.empty)
        Json
          .parse("""{"state":"SelectClientType", "properties": {"basket": []}}""")
          .as[State] shouldBe SelectClientType(Set.empty)
      }

      "SelectPersonalService" in {
        Json.toJson(SelectPersonalService(Set(HMRCPIR, HMRCMTDIT, HMRCMTDVAT), Set.empty)) shouldBe Json.obj(
          "state" -> "SelectPersonalService",
          "properties" -> Json
            .obj("basket" -> JsArray(), "services" -> Json.arr("PERSONAL-INCOME-RECORD", "HMRC-MTD-IT", "HMRC-MTD-VAT"))
        )
        Json
          .parse(
            """{"state":"SelectPersonalService", "properties": {"basket": [], "services": ["PERSONAL-INCOME-RECORD", "HMRC-MTD-IT", "HMRC-MTD-VAT"]}}""")
          .as[State] shouldBe SelectPersonalService(Set(HMRCPIR, HMRCMTDIT, HMRCMTDVAT), Set.empty)
      }

      "SelectBusinessService" in {
        Json.toJson(SelectBusinessService) shouldBe Json
          .obj("state" -> "SelectBusinessService")
        Json
          .parse("""{"state":"SelectBusinessService"}""")
          .as[State] shouldBe SelectBusinessService
      }

      "SelectTrustService" in {
        Json.toJson(SelectTrustService(Set(TAXABLETRUST, HMRCCGTPD), Set.empty)) shouldBe Json
          .obj(
            "state" -> "SelectTrustService",
            "properties" -> Json
              .obj("services" -> Json.arr("HMRC-TERS-ORG", "HMRC-CGT-PD"), "basket" -> JsArray()))
        Json
          .parse("""{"state":"SelectTrustService", "properties": {"basket": [], "services": ["HMRC-TERS-ORG", "HMRC-CGT-PD"]}}""")
          .as[State] shouldBe SelectTrustService(Set(TAXABLETRUST, HMRCCGTPD), Set.empty)
      }

      "IdentifyPersonalClient" in {
        Json.toJson(IdentifyPersonalClient(HMRCMTDIT, Set.empty)) shouldBe Json.obj(
          "state"      -> "IdentifyPersonalClient",
          "properties" -> Json.obj("service" -> "HMRC-MTD-IT", "basket" -> JsArray())
        )
        Json
          .parse("""{"state":"IdentifyPersonalClient", "properties": {"basket": [], "service": "HMRC-MTD-IT"}}""")
          .as[State] shouldBe IdentifyPersonalClient(HMRCMTDIT, Set.empty)
      }

      "IdentifyBusinessClient" in {
        Json.toJson(IdentifyBusinessClient) shouldBe Json.obj("state" -> "IdentifyBusinessClient")
        Json
          .parse("""{"state":"IdentifyBusinessClient"}""")
          .as[State] shouldBe IdentifyBusinessClient
      }

      "IdentifyTrustClient" in {
        Json.toJson(IdentifyTrustClient(TAXABLETRUST, Set.empty)) shouldBe Json
          .obj(
            "state"      -> "IdentifyTrustClient",
            "properties" -> Json.obj("service" -> "HMRC-TERS-ORG", "basket" -> JsArray())
          )
        Json
          .parse("""{"state":"IdentifyTrustClient", "properties": {"basket": [], "service": "HMRC-TERS-ORG"}}""")
          .as[State] shouldBe IdentifyTrustClient(TAXABLETRUST, Set.empty)
      }

      "ConfirmClientItsa" in {
        val state = ConfirmClientItsa(AuthorisationRequest("Sylvia Plath", ItsaInvitation(Nino("AB123456A")), itemId = "ABC"), Set.empty)
        val json = Json.parse(
          """{"state":"ConfirmClientItsa","properties":{"request":{"clientName":"Sylvia Plath","invitation":{"type":"ItsaInvitation","data":{"clientType":"personal","service":"HMRC-MTD-IT","clientIdentifier":"AB123456A","clientIdentifierType":"ni"}},"state":"New","itemId":"ABC"},"basket":[]}}""")
        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ConfirmClientPersonalVat" in {
        val state =
          ConfirmClientPersonalVat(AuthorisationRequest("Sylvia Plath", VatInvitation(Some(personal), Vrn("123456")), itemId = "ABC"), Set.empty)
        val json = Json.parse(
          """{"state":"ConfirmClientPersonalVat","properties":{"request":{"clientName":"Sylvia Plath","invitation":{"type":"VatInvitation","data":{"clientType":"personal","service":"HMRC-MTD-VAT","clientIdentifier":"123456","clientIdentifierType":"vrn"}},"state":"New","itemId":"ABC"},"basket":[]}}""")
        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ConfirmClientBusinessVat" in {
        val state = ConfirmClientBusinessVat(AuthorisationRequest("Sylvia Plath", VatInvitation(Some(business), Vrn("123456")), itemId = "ABC"))
        val json = Json.parse(
          """{"state":"ConfirmClientBusinessVat","properties":{"request":{"clientName":"Sylvia Plath","invitation":{"type":"VatInvitation","data":{"clientType":"business","service":"HMRC-MTD-VAT","clientIdentifier":"123456","clientIdentifierType":"vrn"}},"state":"New","itemId":"ABC"}}}""")
        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ConfirmClientTrust" in {
        val state =
          ConfirmClientTrust(
            AuthorisationRequest("Sylvia Plath", TrustInvitation(Utr("4937455253")), itemId = "ABC"),
            Set.empty
          )
        val json =
          Json.parse("""{
                       |"state":"ConfirmClientTrust",
                       |"properties":
                       |  {
                       |   "request":
                       |    {
                       |     "clientName":"Sylvia Plath",
                       |     "invitation":
                       |      {"type":"TrustInvitation",
                       |       "data":
                       |        {"clientType":"business","service":"HMRC-TERS-ORG","clientIdentifier":"4937455253","clientIdentifierType":"utr"}
                       |      },
                       |      "state":"New","itemId":"ABC"
                       |    },
                       |    "basket": []
                       |}}""".stripMargin)
        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ConfirmPostcodeCgt" in {
        val state = ConfirmPostcodeCgt(CgtRef("123456"), personal, Set.empty, Some("BN13 1FN"), "firstName lastName")
        val json = Json.parse(
          """{"state":"ConfirmPostcodeCgt","properties":{"cgtRef":"123456","clientType":"personal","basket":[], "postcode": "BN13 1FN", "clientName": "firstName lastName"}}""")
        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ConfirmCountryCodeCgt" in {
        val state = ConfirmCountryCodeCgt(CgtRef("123456"), personal, Set.empty, "IN", "firstName lastName")
        val json = Json.parse(
          """{"state":"ConfirmCountryCodeCgt","properties":{"cgtRef":"123456","clientType":"personal","basket":[], "countryCode": "IN", "clientName": "firstName lastName"}}""")
        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ReviewAuthorisationsPersonal" in {
        Json.toJson(ReviewAuthorisationsPersonal(Set(HMRCPIR, HMRCMTDIT, HMRCMTDVAT), Set.empty)) shouldBe Json.obj(
          "state" -> "ReviewAuthorisationsPersonal",
          "properties" -> Json
            .obj("basket" -> JsArray(), "services" -> Json.arr("PERSONAL-INCOME-RECORD", "HMRC-MTD-IT", "HMRC-MTD-VAT"))
        )
        Json
          .parse(
            """{"state":"ReviewAuthorisationsPersonal", "properties": {"basket": [], "services": ["PERSONAL-INCOME-RECORD", "HMRC-MTD-IT", "HMRC-MTD-VAT"]}}""")
          .as[State] shouldBe ReviewAuthorisationsPersonal(Set(HMRCPIR, HMRCMTDIT, HMRCMTDVAT), Set.empty)
      }

      "ReviewAuthorisationsTrust" in {
        Json.toJson(ReviewAuthorisationsTrust(Set(TAXABLETRUST, HMRCCGTPD), Set.empty)) shouldBe Json.obj(
          "state" -> "ReviewAuthorisationsTrust",
          "properties" -> Json
            .obj("basket" -> JsArray(), "services" -> Json.arr("HMRC-TERS-ORG", "HMRC-CGT-PD"))
        )
        Json
          .parse("""{"state":"ReviewAuthorisationsTrust", "properties": {"basket": [], "services": ["HMRC-TERS-ORG", "HMRC-CGT-PD"]}}""")
          .as[State] shouldBe ReviewAuthorisationsTrust(Set(TAXABLETRUST, HMRCCGTPD), Set.empty)
      }

      "InvitationSentPersonal" in {
        Json.toJson(InvitationSentPersonal("invitation/link", Some("continue/url"), "abc@xyz.com", Set(HMRCMTDIT, HMRCPIR))) shouldBe Json.obj(
          "state" -> "InvitationSentPersonal",
          "properties" -> Json
            .obj(
              "invitationLink" -> "invitation/link",
              "continueUrl"    -> "continue/url",
              "agencyEmail"    -> "abc@xyz.com",
              "services"       -> Json.arr("HMRC-MTD-IT", "PERSONAL-INCOME-RECORD")
            )
        )
        Json
          .parse(
            """{"state":"InvitationSentPersonal", "properties": {"invitationLink": "invitation/link", "continueUrl": "continue/url", "agencyEmail": "abc@xyz.com", "services": ["HMRC-MTD-IT", "PERSONAL-INCOME-RECORD"]}}""")
          .as[State] shouldBe InvitationSentPersonal("invitation/link", Some("continue/url"), "abc@xyz.com", Set(HMRCMTDIT, HMRCPIR))
      }

      "InvitationSentBusiness" in {
        Json.toJson(InvitationSentBusiness("invitation/link", Some("continue/url"), "abc@xyz.com", Set(HMRCMTDVAT))) shouldBe Json
          .obj(
            "state" -> "InvitationSentBusiness",
            "properties" -> Json
              .obj(
                "invitationLink" -> "invitation/link",
                "continueUrl"    -> "continue/url",
                "agencyEmail"    -> "abc@xyz.com",
                "services"       -> Json.arr("HMRC-MTD-VAT"))
          )
        Json
          .parse(
            """{"state":"InvitationSentBusiness", "properties": {"invitationLink": "invitation/link", "continueUrl": "continue/url", "agencyEmail": "abc@xyz.com", "services": ["HMRC-MTD-VAT"]}}""")
          .as[State] shouldBe InvitationSentBusiness("invitation/link", Some("continue/url"), "abc@xyz.com", Set("HMRC-MTD-VAT"))
      }

      "KnownFactNotMatched" in {
        Json.toJson(KnownFactNotMatched(Set.empty)) shouldBe Json.obj(
          "state"      -> "KnownFactNotMatched",
          "properties" -> Json.obj("basket" -> JsArray())
        )
        Json
          .parse("""{"state":"KnownFactNotMatched", "properties": {"basket": []}}""")
          .as[State] shouldBe KnownFactNotMatched(Set.empty)
      }

      "TrustNotFound" in {
        Json.toJson(TrustNotFound(Set.empty)) shouldBe Json
          .obj("state" -> "TrustNotFound", "properties" -> Json.obj("basket" -> JsArray()))
        Json
          .parse("""{"state":"TrustNotFound", "properties": {"basket": []}}""")
          .as[State] shouldBe TrustNotFound(Set.empty)
      }

      "CannotCreateRequest" in {
        Json.toJson(CannotCreateRequest(Set.empty)) shouldBe Json.obj(
          "state"      -> "CannotCreateRequest",
          "properties" -> Json.obj("basket" -> JsArray())
        )
        Json
          .parse("""{"state":"CannotCreateRequest", "properties": {"basket": []}}""")
          .as[State] shouldBe CannotCreateRequest(Set.empty)
      }

      "SomeAuthorisationsFailed" in {
        val state = SomeAuthorisationsFailed("invitation/link", None, "abc@xyz.com", Set.empty)
        val json = Json.parse(
          """{"state":"SomeAuthorisationsFailed","properties":{"invitationLink":"invitation/link", "agencyEmail": "abc@xyz.com", "basket": []}}""")

        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "AllAuthorisationsFailed" in {
        Json.toJson(AllAuthorisationsFailed(Set.empty)) shouldBe Json.obj(
          "state"      -> "AllAuthorisationsFailed",
          "properties" -> Json.obj("basket" -> JsArray())
        )
        Json
          .parse("""{"state":"AllAuthorisationsFailed", "properties": {"basket": []}}""")
          .as[State] shouldBe AllAuthorisationsFailed(Set.empty)
      }

      "DeleteAuthorisationsPersonal" in {
        val state =
          DeleteAuthorisationRequestPersonal(AuthorisationRequest("Sylvia Plath", ItsaInvitation(Nino("AB123456A")), itemId = "ABC"), Set.empty)
        val json = Json.parse(
          """{"state":"DeleteAuthorisationRequestPersonal","properties":{"authorisationRequest":{"clientName":"Sylvia Plath","invitation":{"type":"ItsaInvitation","data":{"clientType":"personal","service":"HMRC-MTD-IT","clientIdentifier":"AB123456A","clientIdentifierType":"ni"}},"state":"New","itemId":"ABC"},"basket":[]}}""")

        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "AllAuthorisationsRemoved" in {
        val state = AllAuthorisationsRemoved
        val json = Json.parse("""{"state":"AllAuthorisationsRemoved"}""")

        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ClientNotSignedUp" in {
        val state = ClientNotSignedUp(HMRCMTDIT, Set.empty)
        val json = Json.parse("""{"state":"ClientNotSignedUp","properties":{"service": "HMRC-MTD-IT", "basket": []}}""")

        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "PendingInvitationExists" in {
        val state = PendingInvitationExists(personal, Set.empty)
        val json =
          Json.parse("""{"state":"PendingInvitationExists","properties":{"clientType": "personal", "basket": []}}""")

        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "ActiveAuthorisationExists" in {
        val state = ActiveAuthorisationExists(personal, HMRCMTDIT, Set.empty)
        val json =
          Json.parse("""{"state":"ActiveAuthorisationExists","properties":{"clientType": "personal", "service": "HMRC-MTD-IT", "basket": []}}""")

        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }

      "AgentSuspended" in {
        val state = AgentSuspended(HMRCMTDIT, Set.empty)
        val json =
          Json.parse("""{"state":"AgentSuspended","properties":{"suspendedService": "HMRC-MTD-IT", "basket": []}}""")

        Json.toJson(state) shouldBe json
        json.as[State] shouldBe state
      }
    }
  }
}
