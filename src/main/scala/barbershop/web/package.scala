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

import barbershop.comments.{ Comment, given }

import grapple.json.{ jsonValueToCollection, * }

import scamper.http.{ BodyParser, Entity, HttpResponse, ResponseStatus }
import scamper.http.headers.ContentType
import scamper.http.types.MediaType

private val jsonType = MediaType("application/json")

/** Parses HTTP message body to string. */
given stringBodyParser: BodyParser[String] = BodyParser.string(Int.MaxValue)

/** Parses HTTP message body to JSON value. */
given jsonBodyParser: BodyParser[JsonValue] = msg => Json.parse(msg.as[String])

/** Parses HTTP message body to comment. */
given commentBodyParser: BodyParser[Comment] = msg => msg.as[JsonValue].as[Comment]

/** Parses HTTP message body to list of comments. */
given commentListBodyParser: BodyParser[List[Comment]] = msg => msg.as[JsonValue].as[List[Comment]]

extension (status: ResponseStatus)
  /**
   * Creates HTTP response using supplied JSON value.
   *
   * @note The Content-Type header is set accordingly.
   */
  def apply(json: JsonValue): HttpResponse =
    status(Entity(toPrettyPrint(json))).setContentType(jsonType)

  private def toPrettyPrint(json: JsonValue): String =
    json match
      case value: JsonStructure => Json.toPrettyPrint(value)
      case _                    => json.toString
