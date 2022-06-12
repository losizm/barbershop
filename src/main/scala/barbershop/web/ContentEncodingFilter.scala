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

import scamper.http.{ HttpRequest, HttpResponse }
import scamper.http.headers.{ AcceptEncoding, TransferEncoding }
import scamper.http.server.{ ResponseFilter, ServerHttpResponse }

/** Provides response filter to apply content encoding. */
object ContentEncodingFilter extends ResponseFilter:
  /**
   * Applies `gzip` or `deflate` content encoding if accepted.
   *
   * @param res response
   */
  def apply(res: HttpResponse): HttpResponse =
    Option.unless(res.body.isKnownEmpty)(res)
      .flatMap(addContentEncoding)
      .getOrElse(res)

  private def addContentEncoding(res: HttpResponse): Option[HttpResponse] =
    res.request.collect {
      case req if isGzipAccepted(req)    => res.setGzipContentEncoding()
      case req if isDeflateAccepted(req) => res.setDeflateContentEncoding()
    }

  private def isGzipAccepted(req: HttpRequest): Boolean =
    req.acceptEncoding.exists(enc => enc.isGzip && enc.weight > 0)

  private def isDeflateAccepted(req: HttpRequest): Boolean =
    req.acceptEncoding.exists(enc => enc.isDeflate && enc.weight > 0)
