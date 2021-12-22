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

import grapple.json.{ Json, JsonGenerator, JsonParser }
import JsonParser.Event as ParserEvent

import java.io.{ InputStream, OutputStream }
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.zip.{ ZipEntry, ZipInputStream, ZipOutputStream }

import little.io.{ InputStreamExt, PathExt }

import scala.collection.mutable.HashMap as MutableMap

private class CommentStoreBundle:
  val comments    = MutableMap[Long, Comment]()
  val descriptors = MutableMap[Long, FileDescriptor]()
  val blobs       = MutableMap[String, Blob]()
  val attachments = MutableMap[Long, String]()

  def read(file: Path): Unit =
    file.withInputStream() { in =>
      val zip = ZipInputStream(in, Charset.forName("UTF-8"))

      try
        val blobEntry = """blobs/(\w+)""".r
        var entry: ZipEntry = null

        while { entry = zip.getNextEntry(); entry != null } do
          entry.getName match
            case "comments.json"    => readComments(zip)
            case "attachments.json" => readAttachments(zip)
            case blobEntry(key)     => readBlob(zip, key)
            case name               => // Ignore

          zip.closeEntry()
      finally
        zip.close()
    }

  def write(file: Path): Unit =
    file.withOutputStream() { out =>
      val zip = ZipOutputStream(out, Charset.forName("UTF-8"))

      try
        zip.putNextEntry(ZipEntry("comments.json"))
        writeComments(zip)
        zip.closeEntry()

        zip.putNextEntry(ZipEntry("attachments.json"))
        writeAttachments(zip)
        zip.closeEntry()

        blobs.values.foreach { blob =>
          zip.putNextEntry(ZipEntry(s"blobs/${blob.key}"))
          zip.write(blob.data)
          zip.closeEntry()
        }

        zip.finish()
      finally
        zip.close()
    }

  private def readComments(in: InputStream): Unit =
    val parser = JsonParser(in)

    if parser.next() != ParserEvent.StartArray then
      throw CannotReadComment(s"Invalid start to comments")

    while parser.next() != ParserEvent.EndArray do
      val comment = parser.getObject().as[Comment]
      comments += comment.id -> comment
      comment.attachments.foreach(file => descriptors += file.id -> file)

  private def readAttachments(in: InputStream): Unit =
    val parser = JsonParser(in)

    if parser.next() != ParserEvent.StartArray then
      throw CannotReadComment(s"Invalid start to attachments")

    while parser.next() != ParserEvent.EndArray do
      val entry = parser.getArray()
      attachments += entry.getLong(0) -> entry.getString(1)

  private def readBlob(in: InputStream, key: String): Unit =
    val blob = Blob(in.getBytes())

    if blob.key == key then
      blobs += key -> blob

  private def writeComments(out: OutputStream): Unit =
    val generator = JsonGenerator(out, "  ")

    generator.writeStartArray()
    comments.values.foreach(comment => generator.write(Json.toJson(comment)))
    generator.writeEnd()
    generator.flush()

  private def writeAttachments(out: OutputStream): Unit =
    val generator = JsonGenerator(out, "  ")

    generator.writeStartArray()
    attachments.foreach { (id, key) =>
      generator.writeStartArray()
      generator.write(id)
      generator.write(key)
      generator.writeEnd()
    }
    generator.writeEnd()
    generator.flush()
