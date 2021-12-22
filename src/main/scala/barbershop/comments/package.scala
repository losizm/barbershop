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
package comments

import grapple.json.{ iterableToJsonArray, jsonValueToCollection, * }

import java.time.Instant

import scala.util.Try

import scamper.http.QueryString

/** Converts JSON value to file descriptor. */
given jsonValueToFileDescriptor: JsonInput[FileDescriptor] =
  case json: JsonObject =>
    FileDescriptor(
      id   = json.getLong("id"),
      name = json.getString("name"),
      kind = json.getString("kind"),
      size = json.getLong("size")
    )

  case _ => throw IllegalArgumentException("Expected JSON object")

/** Converts file descriptor to JSON value. */
given fileDescriptorToJsonValue: JsonOutput[FileDescriptor] =
  descriptor => JsonObjectBuilder()
    .add("id",   descriptor.id)
    .add("name", descriptor.name)
    .add("kind", descriptor.kind)
    .add("size", descriptor.size)
    .build()

/** Converts JSON value to comment. */
given jsonValueToComment: JsonInput[Comment] =
  case json: JsonObject =>
    Comment(
      id          = json.getLong("id"),
      text        = json.getString("text"),
      attachments = json.getOrElse("attachments", Seq.empty[FileDescriptor]),
      time        = json("time") match
        case JsonString(value) => Instant.parse(value)
        case JsonNumber(value) => Instant.ofEpochMilli(value.toLong)
        case _                 => throw IllegalArgumentException("Expected JSON object")
    )

  case _ => throw IllegalArgumentException("Expected JSON object")

/** Converts comment to JSON value. */
given commentToJsonValue: JsonOutput[Comment] =
  comment => JsonObjectBuilder()
    .add("id",          comment.id)
    .add("text",        comment.text)
    .add("attachments", Json.toJson(comment.attachments))
    .add("time",        comment.time.toString)
    .build()

extension (query: QueryString)
  /**
   * Gets value from query string and if present converts it to time instant.
   *
   * @param name parameter name
   */
  def getInstant(name: String): Option[Instant] =
    Try(query.get(name).map(toInstant))
      .getOrElse(throw IllegalArgumentException(s"Invalid time parameter: $name"))

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
