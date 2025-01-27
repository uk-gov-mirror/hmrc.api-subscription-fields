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

package uk.gov.hmrc.apisubscriptionfields.model

import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import eu.timepit.refined.boolean._

object Types {
  type RegexExpr = String Refined Regex

  type FieldNameRegex = MatchesRegex[W.`"^[a-zA-Z]+$"`.T]
  type FieldName = Refined[String,FieldNameRegex]

  type FieldValue = String

  type Fields = Map[FieldName, FieldValue]

  type ErrorMessage = String

  type FieldError = (FieldName, ErrorMessage)

  type FieldErrorMap = Map[FieldName, ErrorMessage]
  object FieldErrorMap {
    val empty = Map.empty[FieldName, ErrorMessage]
  }

  type IsInsert = Boolean

}
