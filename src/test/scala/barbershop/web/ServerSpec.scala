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

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import scala.language.implicitConversions

import scamper.http.{ ResponseStatus, given }
import scamper.http.client.HttpClient
import ResponseStatus.Registry.{ Created, NoContent, NotFound, Ok }

import Implicits.given

class ServerSpec extends org.scalatest.flatspec.AnyFlatSpec:
  it should "create and communicate with API server" in withServer { server =>
    val apiConfig = config.getConfig("barbershop.api")
    val apiPath   = apiConfig.getString("mountPath")
    val apiUrl    = server.url + apiPath

    info(s"API URL is $apiUrl")

    val time1 = AtomicReference[Instant]()
    val time2 = AtomicReference[Instant]()
    val time3 = AtomicReference[Instant]()

    implicit val client = HttpClient()

    info("add first comment")
    client.post(s"$apiUrl/comments", body = "Hello, barbershop!") { res =>
      assert(res.status == Created)
      assert(res.getHeaderValue("Location").contains(s"$apiPath/comments/1"))
    }

    info("verify first comment")
    time1.set(verifyComment(s"$apiUrl/comments/1", 1, "Hello, barbershop!"))

    info("verify all comments")
    verifyComments(s"$apiUrl/comments", 1L -> "Hello, barbershop!")

    info("add second comment")
    client.post(s"$apiUrl/comments", body = "This is a new comment.") { res =>
      assert(res.status == Created)
      assert(res.getHeaderValue("Location").contains(s"$apiPath/comments/2"))
    }

    info("verify second comment")
    time2.set(verifyComment(s"$apiUrl/comments/2", 2, "This is a new comment."))

    info("verify all comments")
    verifyComments(
      s"$apiUrl/comments",
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment."
    )

    info("add third comment")
    client.post(s"$apiUrl/comments", body = "This is another new comment.") { res =>
      assert(res.status == Created)
      assert(res.getHeaderValue("Location").contains(s"$apiPath/comments/3"))
    }

    info("verify third comment")
    time3.set(verifyComment(s"$apiUrl/comments/3", 3, "This is another new comment."))


    info("verify all comments")
    verifyComments(
      s"$apiUrl/comments",
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment.",
      3L -> "This is another new comment."
    )

    info("verify comments in range (includes first two comments)")
    verifyComments(
      s"$apiUrl/comments?minTime=${time1.get.toEpochMilli}&maxTime=${time2.get.toEpochMilli}",
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment."
    )

    info("verify comments in range (includes last two comments)")
    verifyComments(
      s"$apiUrl/comments?minTime=${time2.get.toEpochMilli}&maxTime=${time3.get.toEpochMilli}",
      2L -> "This is a new comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with offset (skips first comment)")
    verifyComments(
      s"$apiUrl/comments?offset=1",
      2L -> "This is a new comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with limit (includes first two comments)")
    verifyComments(
      s"$apiUrl/comments?limit=2",
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment."
    )

    info("update second comment")
    client.put(s"$apiUrl/comments/2", body = "This is an updated comment.") { res =>
      assert(res.status == NoContent)
    }

    info("verify second comment is updated")
    verifyComment(s"$apiUrl/comments/2", 2, "This is an updated comment.")

    info("verify all comments")
    verifyComments(
      s"$apiUrl/comments",
      1L -> "Hello, barbershop!",
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )

    info("verify comments in range (includes first and last comments)")
    verifyComments(
      s"$apiUrl/comments?minTime=${time1.get.toEpochMilli}&maxTime=${time3.get.toEpochMilli}",
      1L -> "Hello, barbershop!",
      3L -> "This is another new comment."
    )

    info("verify comments in range (includes last two comments)")
    verifyComments(
      s"$apiUrl/comments?minTime=${time3.get.toEpochMilli}",
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with offset (skips first comment)")
    verifyComments(
      s"$apiUrl/comments?offset=1",
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with limit (includes first two comments)")
    verifyComments(
      s"$apiUrl/comments?limit=2",
      1L -> "Hello, barbershop!",
      2L -> "This is an updated comment."
    )

    info("delete first comment")
    client.delete(s"$apiUrl/comments/1") { res =>
      assert(res.status == NoContent)
    }

    info("verify first comment is deleted")
    client.delete(s"$apiUrl/comments/1") { res =>
      assert(res.status == NotFound)
    }

    info("verify all comments")
    verifyComments(
      s"$apiUrl/comments",
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )
  }

  private def withServer[T](op: Server => T): T =
    val server = Server()
    try
      server.start()
      op(server)
    finally
      server.stop()

  private def verifyComment(url: String, id: Long, text: String)(using client: HttpClient): Instant =
    client.get(url) { res =>
      assert(res.status == Ok)

      val comment = res.as[Comment]
      assert(comment.id == id)
      assert(comment.text == text)

      comment.time
    }

  private def verifyComments(url: String, comments: (Long, String)*)(using client: HttpClient): Unit =
    client.get(url) { res =>
      assert(res.status == Ok)

      val result = res.as[List[Comment]]
      assert(result.size == comments.size)

      result.zip(comments).foreach { (left, right) =>
        assert(left.id == right._1)
        assert(left.text == right._2)
      }
    }
