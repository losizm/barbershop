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

import scamper.http.HttpResponse
import scamper.http.server.{ ResponseFilter, ServerHttpMessage }

/** Provides response filter to log response. */
object ResponseLogger extends ResponseFilter:
  private val eol    = System.getProperty("line.separator")
  private val logger = Logger("barbershop.web.ResponseLogger")

  /**
   * Logs status line and headers.
   *
   * @param res response
   *
   * @return response
   */
  def apply(res: HttpResponse): HttpResponse =
    logger.info("%s:%d - Outgoing response (correlate=%s)%n%s%n%s%n",
      res.server.host.getHostName,
      res.server.port,
      res.correlate,
      res.startLine,
      res.headers.mkString(eol)
    )
    res
