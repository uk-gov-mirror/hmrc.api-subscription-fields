/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.apisubscriptionfields.controller

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsValue, Json, JsSuccess, JsError}
import play.api.mvc._
import uk.gov.hmrc.apisubscriptionfields.model._
import uk.gov.hmrc.apisubscriptionfields.service.FieldsDefinitionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Try,Success,Failure}
import play.api.Logger
import java.util.UUID

@Singleton
class FieldsDefinitionController @Inject() (cc: ControllerComponents, service: FieldsDefinitionService) extends CommonController {

  import JsonFormatters._

  private def badRequestWithTag(fn: (UUID) => String): Result = {
    val errorTag = java.util.UUID.randomUUID()
    Logger.warn(fn(errorTag))
    BadRequest(s"""{"tag": "$errorTag"}""")
  }

  private def notFoundResponse(apiContext: ApiContext, rawApiVersion: String) =
    NotFound(JsErrorResponse(ErrorCode.NOT_FOUND_CODE, s"Fields definition not found for (${apiContext.value}, $rawApiVersion)"))

  def validateFieldsDefinition(): Action[JsValue] = Action(parse.json) { request =>
    Try(request.body.validate[FieldsDefinitionRequest]) match {
      case Success(JsSuccess(payload, _)) => Ok("")
      case Success(JsError(errs)) => {
        badRequestWithTag( (tag:UUID) => s"A JSON error occurred: [${tag.toString}] ${Json.prettyPrint(JsError.toJson(errs))}")
      }
      case Failure(e) => {
        badRequestWithTag{ (tag:UUID) => s"An error occurred during JSON validation: [${tag.toString}] ${e.getMessage}" }
      }
    }
  }

  def upsertFieldsDefinition(apiContext: ApiContext, rawApiVersion: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[FieldsDefinitionRequest] { payload =>
      service.upsert(apiContext, ApiVersion(rawApiVersion), payload.fieldDefinitions) map {
        case (response, true) => Created(Json.toJson(response))
        case (response, false) => Ok(Json.toJson(response))
      }
    } recover recovery
  }

  def getAllFieldsDefinitions: Action[AnyContent] = Action.async {  _ =>
    service.getAll map (defs => Ok(Json.toJson(defs))) recover recovery
  }

  def getFieldsDefinition(apiContext: ApiContext, rawApiVersion: String): Action[AnyContent] = Action.async { _ =>
    val eventualMaybeResponse = service.get(apiContext, ApiVersion(rawApiVersion))
    asActionResult(eventualMaybeResponse, apiContext, rawApiVersion)
  }

  def deleteFieldsDefinition(apiContext: ApiContext, rawApiVersion: String): Action[AnyContent] = Action.async { _ =>
    service.delete(apiContext, ApiVersion(rawApiVersion)) map {
      case true => NoContent
      case false => notFoundResponse(apiContext, rawApiVersion)
    } recover recovery
  }

  private def asActionResult(eventualMaybeResponse: Future[Option[FieldsDefinitionResponse]], apiContext: ApiContext, rawApiVersion: String) = {
    eventualMaybeResponse map {
      case Some(subscriptionFields) => Ok(Json.toJson(subscriptionFields))
      case None => notFoundResponse(apiContext, rawApiVersion)
    } recover recovery
  }

  override protected def controllerComponents: ControllerComponents = cc
}
