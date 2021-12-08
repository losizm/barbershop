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

import grapple.json.{ jsonValueToCollection, * }
import JsonParser.Event as ParserEvent

import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

import scala.collection.concurrent.TrieMap
import scala.math.Ordered.orderingToOrdered

import scamper.http.QueryString

import Implicits.{ *, given }

/** Defines comment store. */
class CommentStore:
  private val comments = TrieMap[Long, Comment]()
  private val lastId   = AtomicLong(0)

  /** Gets current store size. */
  def size: Int = comments.size

  /**
   * Loads comments from specified file.
   *
   * @param file input file
   *
   * @note Comments are cleared before loading.
   */
  def load(file: Path): Unit =
    val parser = JsonParser(file)
    try
      if parser.next() != ParserEvent.StartArray then
        throw CannotReadComment(s"Invalid start to comments: $file")

      lastId.set(0)
      comments.clear()

      while parser.next() != ParserEvent.EndArray do
        val comment = parser.getObject().as[Comment]
        comments += comment.id -> comment
        lastId.updateAndGet(_.max(comment.id))
    finally
      parser.close()

  /**
   * Saves comments to specified file.
   *
   * @param file output file
   */
  def save(file: Path): Unit =
    val generator = JsonGenerator(file, "  ")
    try
      generator.writeStartArray()
      comments.values.foreach(comment => generator.write(Json.toJson(comment)))
      generator.writeEnd()
    finally
      generator.close()

  /**
   * List comments.
   *
   * The following query parameters are accepted:
   *
   * - `minId` &ndash; lower bound of comment identifier
   * - `maxId` &ndash; upper bound of comment identifier
   * - `minTime` &ndash; lower bound of comment time; supplied as Epoch
   *   milliseconds or timestamp formatted as `yyyy-MM-ddTmm:hh:ssZ`
   * - `maxTime` &ndash; upper bound of comment time; supplied as Epoch
   *   milliseconds or timestamp formatted as `yyyy-MM-ddTmm:hh:ssZ`
   * - `offset` &ndash; number of leading comments to drop
   * - `limit` &ndash; maximum number of comments to list
   *
   * @param query query parameters
   */
  def list(query: QueryString): Seq[Comment] =
    val minId   = query.getLongOrElse("minId", 0)
    val maxId   = query.getLongOrElse("maxId", Long.MaxValue)
    val minTime = query.getInstantOrElse("minTime", Instant.MIN)
    val maxTime = query.getInstantOrElse("maxTime", Instant.MAX)
    val offset  = query.getIntOrElse("offset", 0)
    val limit   = query.getIntOrElse("limit", Int.MaxValue)

    comments.values
      .filter(comment => comment.id.between(minId, maxId) && comment.time.between(minTime, maxTime))
      .toSeq
      .sortBy(_._1)
      .drop(offset)
      .take(limit)

  /**
   * Adds comment.
   *
   * @return identifer
   */
  def add(text: String): Long =
    val id = lastId.incrementAndGet()
    comments += id -> Comment(id, text)
    id

  /** Gets comment. */
  def get(id: Long): Option[Comment] =
    comments.get(id)

  /**
   * Updates comment.
   *
   * @return `true` if comment was updated; otherwise, `false`
   */
  def update(id: Long, text: String): Boolean =
    comments.replace(id, Comment(id, text)).isDefined

  /**
   * Deletes comment.
   *
   * @return `true` if comment was deleted; otherwise, `false`
   */
  def remove(id: Long): Boolean =
    comments.remove(id).isDefined

  /** Clears all comments. */
  def clear(): Unit =
    comments.clear()

  extension [T](value: T)
    private def between(min: T, max: T)(using ord: Ordering[T]) =
      value >= min && value <= max
