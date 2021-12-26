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

import barbershop.comments.{ *, given }
import barbershop.logging.Logger

import com.typesafe.config.Config

import grapple.json.{ Json, JsonObjectBuilder, iterableToJsonArray }

import java.nio.file.{ Files, Path }

import little.config.{ ConfigExt, stringDelegate }
import little.io.FileExt

import scamper.http.{ BodyParser, HttpRequest, Uri }
import scamper.http.ResponseStatus.Registry.*
import scamper.http.headers.{ ContentType, Location }
import scamper.http.multipart.{ *, given }
import scamper.http.server.{ *, given }
import scamper.http.types.MediaType

/**
 * Defines web API.
 *
 * @constructor Creates web API
 *
 * @param config configuration
 */
class Api(config: Config) extends RouterApplication:
  /** Creates web API with default configuration. */
  def this() = this(config.getConfig("barbershop.api"))

  private val logger   = Logger("barbershop.web.Api")
  private val comments = CommentStore()
  private val file     = config.getOption[Path]("comment.file")

  private val textMaxLength  = config.getMemorySizeInt("comment.textMaxLength")
  private val totalMaxLength = config.getMemorySizeInt("comment.totalMaxLength")

  private val settings = JsonObjectBuilder()
    .add("textMaxLength", textMaxLength)
    .add("totalMaxLength", totalMaxLength)
    .build()

  private given BodyParser[String]    = BodyParser.string(textMaxLength)
  private given BodyParser[Multipart] = Multipart.getBodyParser(maxLength = totalMaxLength)

  /** Applies API to supplied router. */
  def apply(router: Router): Unit =
    config.getOption[String]("token")
      .map(TokenAuthenticator(_))
      .foreach(router.incoming)

    router.trigger {
      case LifecycleEvent.Start(server) =>
        try
          file.foreach { path =>
            Files.exists(path) match
              case true  => comments.load(path)
              case false => logger.warn(s"Comment file does not exist: ${path.normalize().toAbsolutePath}")
          }
        catch case err: Exception =>
          logger.error("Cannot load comments", err)

      case LifecycleEvent.Stop(server) =>
        try
          file.foreach(comments.save)
        catch case err: Exception =>
          logger.error("Cannot save comments", err)
    }

    router.get("/comments/settings") { req =>
      logger.debug(s"Settings retrieved")
      Ok(settings)
    }

    router.get("/comments") { req =>
      val list = comments.list(req.query)
      logger.debug(s"Comments listed: query=${req.query}")
      Ok(Json.toJson(list))
    }

    router.post("/comments") { req =>
      val id = getContent(req)
        match
          case text: String     => comments.add(text)
          case parts: Multipart => comments.add(getText(parts), getAttachments(parts))

      logger.debug(s"Comment added: id=$id")
      Created().setLocation(Uri(router.toAbsolutePath(s"/comments/$id")))
    }

    router.get("/comments/:id") { req =>
      val id = req.params.getLong("id")

      comments.get(id)
        match
          case Some(comment) =>
            logger.debug(s"Comment listed: id=$id")
            Ok(Json.toJson(comment))

          case None =>
            logger.debug(s"Comment not listed: id=$id (Does Not Exist)")
            NotFound()
    }

    router.put("/comments/:id") { implicit req =>
      val id = req.params.getLong("id")

      getContent(req)
        match
          case text: String     => comments.update(id, text)
          case parts: Multipart => comments.update(id, getText(parts), getAttachments(parts))
        match
          case true =>
            logger.debug(s"Comment updated: id=$id")
            NoContent()

          case false =>
            logger.debug(s"Comment not updated: id=$id (Does Not Exist)")
            NotFound()
    }

    router.delete("/comments/:id") { req =>
      val id = req.params.getLong("id")

      comments.remove(id)
        match
          case true =>
            logger.debug(s"Comment deleted: id=$id")
            NoContent()

          case false =>
            logger.debug(s"Comment not deleted: id=$id (Does Not Exist)")
            NotFound()
    }

    router.get("/attachments/:id") { req =>
      val id = req.params.getLong("id")

      comments.getAttachment(id)
        match
          case Some(file) =>
            logger.debug(s"Attachment found: id=$id")
            Ok(file.data.toArray).setContentType(MediaType(file.kind))

          case None =>
            logger.debug(s"Attachment not found: id=$id (Does Not Exist)")
            NotFound()
    }

  private def getContent(req: HttpRequest): String | Multipart =
    getContentType(req)
      match
        case "text/plain"          => req.continue(); req.as[String]
        case "multipart/form-data" => req.continue(); req.as[Multipart]
        case _                     => throw CannotReadComment("Cannot read comment")

  private def getContentType(req: HttpRequest): String =
    req.getContentType.map(_.fullName).getOrElse("text/plain")

  private def getText(multipart: Multipart): String =
    multipart.getString("text")
      .getOrElse(throw ParameterNotFound("text"))

  private def getAttachments(multipart: Multipart): Seq[Attachment] =
    multipart.getParts("attachment")
      .take(maxAttachmentCount)
      .map { part =>
        Attachment(
          part.fileName.getOrElse(FileNameFactory.create(part.contentType)),
          part.contentType.fullName,
          part.getBytes()
        )
      }
