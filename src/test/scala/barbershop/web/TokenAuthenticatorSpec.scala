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

import scala.language.implicitConversions

import scamper.http.{ RequestMethod, ResponseStatus, stringToUri, stringToHeader }
import RequestMethod.Registry.Get
import ResponseStatus.Registry.{ Forbidden, Unauthorized }

class TokenAuthenticatorSpec extends org.scalatest.flatspec.AnyFlatSpec:
  private val authenticator = TokenAuthenticator("letmein")

  it should "authenticate request with authorized token" in {
    val req = Get("/api").addHeaders("Authorization: Bearer letmein")
    assert(authenticator(req) == req)
  }

  it should "not authenticate request without token" in {
    val req1 = Get("/api")
    assert(authenticator(req1) == Unauthorized())

    val req2 = Get("/api").addHeaders("Authorization: Bearer")
    assert(authenticator(req2) == Unauthorized())
  }

  it should "not authenticate request with unauthorized token" in {
    val req = Get("/api").addHeaders("Authorization: Bearer secret")
    assert(authenticator(req) == Forbidden())
  }

  it should "not create authenticator with invalid token" in {
    assertThrows[NullPointerException](TokenAuthenticator(null))
    assertThrows[IllegalArgumentException](TokenAuthenticator(""))
  }
