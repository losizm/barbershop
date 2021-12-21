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
package comments

import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import scala.language.implicitConversions

import scamper.http.QueryString

class CommentStoreSpec extends org.scalatest.flatspec.AnyFlatSpec:
  it should "create and communicate with API server" in {
    implicit val commentStore = CommentStore()

    val time1 = AtomicReference[Instant]()
    val time2 = AtomicReference[Instant]()
    val time3 = AtomicReference[Instant]()

    info("add first comment")
    assert(commentStore.add("Hello, barbershop!") == 1)
    Thread.sleep(10)

    info("verify first comment")
    time1.set(verifyComment(1, "Hello, barbershop!"))

    info("verify all comments")
    verifyComments(
      QueryString.empty,
      1L -> "Hello, barbershop!"
    )

    info("add second comment")
    assert(commentStore.add("This is a new comment.") == 2)
    Thread.sleep(10)

    info("verify second comment")
    time2.set(verifyComment(2, "This is a new comment."))

    info("verify all comments")
    verifyComments(
      QueryString.empty,
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment."
    )

    info("add third comment")
    assert(commentStore.add("This is another new comment.") == 3)
    Thread.sleep(10)

    info("verify third comment")
    time3.set(verifyComment(3, "This is another new comment."))

    info("verify all comments")
    verifyComments(
      QueryString.empty,
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment.",
      3L -> "This is another new comment."
    )

    info("verify comments in id range (includes first two comments)")
    verifyComments(
      QueryString("minId=1&maxId=2"),
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment."
    )

    info("verify comments in id range (includes last two comments)")
    verifyComments(
      QueryString("minId=2&maxId=3"),
      2L -> "This is a new comment.",
      3L -> "This is another new comment."
    )

    info("verify comments in time range (includes first two comments)")
    verifyComments(
      QueryString(s"minTime=${time1.get.toEpochMilli}&maxTime=${time2.get.toEpochMilli}"),
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment."
    )

    info("verify comments in time range (includes last two comments)")
    verifyComments(
      QueryString(s"minTime=${time2.get.toEpochMilli}&maxTime=${time3.get.toEpochMilli}"),
      2L -> "This is a new comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with offset (skips first comment)")
    verifyComments(
      QueryString("offset=1"),
      2L -> "This is a new comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with limit (includes first two comments)")
    verifyComments(
      QueryString("limit=2"),
      1L -> "Hello, barbershop!",
      2L -> "This is a new comment."
    )

    info("update second comment")
    assert(commentStore.update(2, "This is an updated comment."))

    info("verify second comment is updated")
    verifyComment(2, "This is an updated comment.")

    info("verify all comments")
    verifyComments(
      QueryString.empty,
      1L -> "Hello, barbershop!",
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )

    info("verify comments in time range (includes first and last comments)")
    verifyComments(
      QueryString(s"minTime=${time1.get.toEpochMilli}&maxTime=${time3.get.toEpochMilli}"),
      1L -> "Hello, barbershop!",
      3L -> "This is another new comment."
    )

    info("verify comments in time range (includes last two comments)")
    verifyComments(
      QueryString(s"minTime=${time3.get.toEpochMilli}"),
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with offset (skips first comment)")
    verifyComments(
      QueryString("offset=1"),
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )

    info("verify comments with limit (includes first two comments)")
    verifyComments(
      QueryString("limit=2"),
      1L -> "Hello, barbershop!",
      2L -> "This is an updated comment."
    )

    info("delete first comment")
    assert(commentStore.remove(1))

    info("verify first comment is deleted")
    assert(!commentStore.remove(1))

    info("verify all comments")
    verifyComments(
      QueryString.empty,
      2L -> "This is an updated comment.",
      3L -> "This is another new comment."
    )
  }

  it should "save and load comments from file" in withCommentFile { file =>
    val comments = Seq(
      Comment(1, "This is comment #1."),
      Comment(2, "This is comment #2."),
      Comment(3, "This is comment #3."),
      Comment(4, "This is comment #4.")
    )


    val commentStore = CommentStore()

    info("adding comments")
    comments.foreach { comment =>
      assert(commentStore.add(comment.text) == comment.id)
    }
    assert(commentStore.size == 4)

    info("saving comments")
    commentStore.save(file.toPath)

    info("clearing comments")
    commentStore.clear()
    assert(commentStore.isEmpty)

    info("loading comments")
    commentStore.load(file.toPath)
    assert(commentStore.size == 4)

    info("verifying comments")
    comments.foreach { comment =>
      assert(commentStore.get(comment.id).exists(_.text == comment.text))
    }
  }

  private def withCommentFile[T](f: File => T): T =
    val file = File.createTempFile("comments-", ".json")
    try f(file)
    finally file.delete()

  private def verifyComment(id: Long, text: String)(using commentStore: CommentStore): Instant =
    commentStore.get(id).map { comment =>
      assert(comment.id == id)
      assert(comment.text == text)

      comment.time
    }.get

  private def verifyComments(query: QueryString, comments: (Long, String)*)(using commentStore: CommentStore): Unit =
    val result = commentStore.list(query)
    assert(result.size == comments.size)

    result.zip(comments).foreach { (left, right) =>
      assert(left.id == right._1)
      assert(left.text == right._2)
    }
