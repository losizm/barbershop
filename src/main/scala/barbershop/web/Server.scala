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

import com.typesafe.config.Config

import little.config.given

import scala.language.implicitConversions
import scala.util.Try

import scamper.http.ResponseStatus.Registry.Ok
import scamper.http.server.{ HttpServer, ServerApplication }

/**
 * Defines web server.
 *
 * @constructor Creates web server
 *
 * @param config configuration
 */
class Server(config: Config): //
  /** Creates web server with default configuration. */
  def this() = this(config.getConfig("barbershop"))

  private var server = null : HttpServer

  /** Gets server URL. */
  def url: String =
    server match
      case null =>
        throw IllegalStateException("Server is not running")

      case server =>
        StringBuilder()
          .append(if server.isSecure then "https" else "http")
          .append("://")
          .append(server.host.getHostAddress)
          .append(':')
          .append(server.port)
          .toString

  /** Tests whether web server is running. */
  def isRunning: Boolean =
    server != null

  /** Starts web server. */
  def start(): Unit = synchronized {
    if server == null then
      server = ServerApplication()
        .configureLogger()
        .configureBacklogSize()
        .configurePoolSize()
        .configureQueueSize()
        .configureBufferSize()
        .configureReadTimeout()
        .configureHeaderLimit()
        .configureKeepAlive()
        .configureSecure()
        .configureHandlers()
        .createServer()
  }

  /** Stops web server. */
  def stop(): Unit = synchronized {
    if server != null then
      try server.close() finally server = null
  }

  extension (app: ServerApplication)
    private def configureLogger(): ServerApplication =
      app.logger(Logger("barbershop.web.Server"))

    private def configureBacklogSize(): ServerApplication =
      app.backlogSize(config.getInt("server.backlogSize"))

    private def configurePoolSize(): ServerApplication =
      app.poolSize(config.getInt("server.poolSize"))

    private def configureQueueSize(): ServerApplication =
      app.queueSize(config.getInt("server.queueSize"))

    private def configureBufferSize(): ServerApplication =
      app.bufferSize(config.getMemorySizeInt("server.bufferSize"))

    private def configureReadTimeout(): ServerApplication =
      app.readTimeout(config.getInt("server.readTimeout"))

    private def configureHeaderLimit(): ServerApplication =
      app.headerLimit(config.getInt("server.headerLimit"))

    private def configureKeepAlive(): ServerApplication =
      if config.hasPathAny("server.keepAlive.timeout", "server.keepAlive.max") then
        app.keepAlive(
          config.getInt("server.keepAlive.timeout"),
          config.getInt("server.keepAlive.max")
        )
      else
        app

    private def configureSecure(): ServerApplication =
      if config.hasPathAny("server.keystore.file", "server.keystore.password", "server.keystore.type") then
        app.secure(
          config.getFile("server.keystore.file"),
          config.getString("server.keystore.password"),
          config.getOrElse("server.keystore.type", "jks")
        )
      else if config.hasPathAny("server.key", "server.certificate") then
        app.secure(
          config.getFile("server.key"),
          config.getFile("server.certificate")
        )
      else
        app

    private def configureHandlers(): ServerApplication =
      app.incoming(RequestLogger)
        .get(config.getString("server.readyPath"))(_ => Ok())
        .get(config.getString("server.alivePath"))(_ => Ok())
        .route(config.getString("api.mountPath"))(Api(config.getConfig("api")))
        .files(config.getString("ui.mountPath"), config.getFile("ui.sourceDirectory"), "index.html")
        .outgoing(ContentEncodingFilter)
        .outgoing(ResponseLogger)
        .recover(DefaultErrorHandler)

    private def createServer(): HttpServer =
      app.create(
        config.getString("server.host"),
        config.getInt("server.port")
      )
