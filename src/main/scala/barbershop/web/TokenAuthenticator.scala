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

import scamper.http.{ HttpMessage, HttpRequest }
import scamper.http.ResponseStatus.Registry.{ Forbidden, Unauthorized }
import scamper.http.auth.Authorization
import scamper.http.server.RequestHandler

/**
 * Defines token authenticator.
 *
 * The handler uses Bearer token supplied in Authorization header to authorize
 * requests.
 *
 * @constructor Creates token authenticator.
 *
 * @param token authorization token
 */
class TokenAuthenticator(token: String) extends RequestHandler:
  if token == null then
    throw NullPointerException("token")

  if token.size == 0 then
    throw IllegalArgumentException("token")

  /**
   * Authorizes request using Bearer token supplied in Authorization header.
   *
   * @param req request
   *
   * @return request if authorized; 401 (Unauthorized) response if Bearer token
   * not supplied; 403 (Forbidden) response if Bearer token not authorized
   */
  def apply(req: HttpRequest): HttpMessage =
    try
      req.getBearer
        .map(bearer => if bearer.token == token then req else Forbidden())
        .getOrElse(Unauthorized())
    catch case _: IllegalArgumentException =>
      Unauthorized()
