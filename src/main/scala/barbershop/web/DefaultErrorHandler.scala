/*
 * Copyright 2021 Carlos Conyers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package barbershop
package web

import barbershop.logging.Logger
import barbershop.web.Implicits.*

import grapple.json.{ Json, JsonObjectBuilder, JsonOutput }

import scala.util.Try

import scamper.http.*
import scamper.http.server.*

import ResponseStatus.Registry.*

/** Provides default error handler. */
object DefaultErrorHandler extends ErrorHandler:
  private val logger = Logger("barbershop.web.DefaultErrorHandler")

  /** Creates response for request. */
  def apply(req: HttpRequest): PartialFunction[Throwable, HttpResponse] =
    case err: ParameterNotFound       => BadRequest(Json.toJson(err))
    case err: ParameterNotConvertible => BadRequest(Json.toJson(err))
    case err: CannotReadComment       => BadRequest(Json.toJson(err))
    case err: ReadLimitExceeded       => PayloadTooLarge(Json.toJson(err))
    case err: EntityTooLarge          => PayloadTooLarge(Json.toJson(err))
    case err =>
      logger.error(s"Internal server error (correlate=${correlate(req)})", err)
      InternalServerError()

  private def correlate(req: HttpRequest): String =
    Try(req.correlate).getOrElse("<Not Available>")

  private given JsonOutput[ParameterNotFound] =
    err => JsonObjectBuilder()
      .add("type", "ParameterNotFound")
      .add("name", err.name)
      .add("message", s"Parameter not found: ${err.name}")
      .build()

  private given JsonOutput[ParameterNotConvertible] =
    err => JsonObjectBuilder()
      .add("type", "ParameterNotConvertible")
      .add("name", err.name)
      .add("message", s"Parameter not convertible: ${err.value}")
      .build()

  private given JsonOutput[ReadLimitExceeded] =
    err => JsonObjectBuilder()
      .add("type", "CommentTooBig")
      .add("message", s"Comment exceeds ${err.limit} bytes")
      .build()

  private given JsonOutput[EntityTooLarge] =
    err => JsonObjectBuilder()
      .add("type", "CommentTooBig")
      .add("message", s"Comment exceeds ${err.maxLength} bytes")
      .build()

  private given JsonOutput[CannotReadComment] =
    err => JsonObjectBuilder()
      .add("type", "CannotReadComment")
      .add("message", err.message)
      .build()
