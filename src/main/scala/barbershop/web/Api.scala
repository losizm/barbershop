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
import barbershop.web.Implicits.{ *, given }

import com.typesafe.config.Config

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

import grapple.json.Json
import grapple.json.Implicits.given

import little.config.{ ConfigExt, stringDelegate }

import scala.collection.concurrent.TrieMap
import scala.language.implicitConversions
import scala.util.Try

import scamper.http.{ BodyParser, HttpRequest, QueryString }
import scamper.http.ResponseStatus.Registry.*
import scamper.http.headers.Location
import scamper.http.server.{ *, given }
import scamper.http.stringToUri

/**
 * Defines web API.
 *
 * @constructor Creates web API
 *
 * @param config configuration
 */
class Api(config: Config) extends RoutingApplication:
  /** Creates web API with default configuration. */
  def this() = this(config.getConfig("barbershop.api"))

  private val logger   = Logger("barbershop.web.Api")
  private val comments = TrieMap[Long, Comment]()
  private val count    = AtomicLong(0)

  private given BodyParser[String] = BodyParser.text(config.getMemorySizeInt("comment.maxLength"))

  /** Applies API to supplied router. */
  def apply(router: Router): Unit =
    config.getOption[String]("token")
      .map(TokenAuthenticator(_))
      .foreach(router.incoming)

    router.get("/comments") { req =>
      val comments = list(req.query)

      logger.debug(s"Comments listed: query=${req.query}")
      Ok(Json.toJson(comments))
    }

    router.post("/comments") { implicit req =>
      val id = add(text)
      logger.debug(s"Comment added: id=$id")
      Created().setLocation(router.toAbsolutePath(s"/comments/$id"))
    }

    router.get("/comments/:id") { req =>
      val id = req.params.getLong("id")
      get(id) match
        case Some(comment) =>
          logger.debug(s"Comment listed: id=$id")
          Ok(Json.toJson(comment))

        case None =>
          logger.debug(s"Comment not listed: id=$id (Does Not Exist)")
          NotFound()
    }

    router.put("/comments/:id") { implicit req =>
      val id = req.params.getLong("id")
      update(id, text) match
        case true =>
          logger.debug(s"Comment updated: id=$id")
          NoContent()

        case false =>
          logger.debug(s"Comment not updated: id=$id (Does Not Exist)")
          NotFound()
    }

    router.delete("/comments/:id") { req =>
      val id = req.params.getLong("id")
      remove(id) match
        case true =>
          logger.debug(s"Comment deleted: id=$id")
          NoContent()

        case false =>
          logger.debug(s"Comment not deleted: id=$id (Does Not Exist)")
          NotFound()
    }

  private def text(using req: HttpRequest): String =
    Try(req.as[String]).getOrElse(throw CannotReadComment("Cannot read comment"))

  private def show[T](time: Option[T]): String =
    time.map(_.toString).getOrElse("[]")

  private def list(query: QueryString): Seq[Comment] =
    import Ordered.orderingToOrdered

    val minTime = query.getInstantOrElse("minTime", Instant.MIN)
    val maxTime = query.getInstantOrElse("maxTime", Instant.MAX)
    val offset  = query.getIntOrElse("offset", 0)
    val limit   = query.getIntOrElse("limit", Int.MaxValue)

    comments.values
      .filter(comment => comment.time >= minTime && comment.time <= maxTime)
      .toSeq
      .sortBy(_._1)
      .drop(offset)
      .take(limit)

  private def add(text: String): Long =
    val id = count.incrementAndGet()
    comments += id -> Comment(id, text)
    id

  private def get(id: Long): Option[Comment] =
    comments.get(id)

  private def update(id: Long, text: String): Boolean =
    comments.replace(id, Comment(id, text)).isDefined

  private def remove(id: Long): Boolean =
    comments.remove(id).isDefined
