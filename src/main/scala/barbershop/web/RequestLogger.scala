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

import scamper.http.HttpRequest
import scamper.http.server.{ RequestHandler, ServerHttpMessage }

/** Provides request handler to log request. */
object RequestLogger extends RequestHandler:
  private val eol    = System.getProperty("line.separator")
  private val logger = Logger("barbershop.web.RequestLogger")

  /**
   * Logs request line and headers.
   *
   * @param req request
   *
   * @return request
   */
  def apply(req: HttpRequest): HttpRequest =
    logger.info("%s:%d - Incoming request (correlate=%s)%n%s%n%s%n",
      req.server.host.getHostName,
      req.server.port,
      req.correlate,
      req.startLine,
      req.headers.mkString(eol)
    )
    req
