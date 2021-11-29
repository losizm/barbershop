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

import grapple.json.*
import grapple.json.Implicits.jsonValueToCollection

import java.time.Instant

import scala.util.Try

import scamper.http.{ BodyParser, Entity, HttpResponse, QueryString, ResponseStatus }
import scamper.http.headers.ContentType
import scamper.http.server.ParameterNotConvertible
import scamper.http.types.MediaType

/** Provides implicit values. */
object Implicits:
  private val jsonType = MediaType("application/json")

  /** Converts JSON value to comment. */
  given jsonValueToComment: JsonInput[Comment] =
    case json: JsonObject =>
      Comment(
        json.getLong("id"),
        json.getString("text"),
        json("time") match
          case JsonString(value) => Instant.parse(value)
          case JsonNumber(value) => Instant.ofEpochMilli(value.toLong)
          case _                 => throw IllegalArgumentException("Expected JSON object")
      )

    case _ => throw IllegalArgumentException("Expected JSON object")

  /** Converts comment to JSON value. */
  given commentToJsonValue: JsonOutput[Comment] =
    comment => JsonObjectBuilder()
      .add("id",   comment.id)
      .add("text", comment.text)
      .add("time", comment.time.toString)
      .build()

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

  extension (query: QueryString)
    /**
     * Gets value from query string and if present converts it to time instant.
     *
     * @param name parameter name
     */
    def getInstant(name: String): Option[Instant] =
      Try(query.get(name).map(toInstant))
        .getOrElse(throw ParameterNotConvertible(name, "Invalid time format"))

    /**
     * Gets value from query string and if present converts it to time instant;
     * otherwise, returns default.
     *
     * @param name parameter name
     */
    def getInstantOrElse(name: String, default: => Instant): Instant =
      getInstant(name).getOrElse(default)

    private def toInstant(time: String): Instant =
      Try(Instant.parse(time))
        .orElse(Try(Instant.ofEpochMilli(time.toLong)))
        .get
