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

import little.io.FileExt

import scamper.http.types.MediaType

/** Defines attachment. */
trait Attachment:
  /** Gets name. */
  def name: String

  /** Gets kind. */
  def kind: String

  /** Gets data. */
  def data: Array[Byte]

  /** Gets size. */
  def size: Int = data.size

/** Provides attachment factory. */
object Attachment:
  /** Creates attachment. */
  def apply(name: String, kind: String, data: Array[Byte]): Attachment =
    AttachmentImpl(name, kind, data)

  /** Creates attachment from file. */
  def apply(file: File): Attachment =
    AttachmentImpl(file.getName(), kind(file), file.getBytes())

  private def kind(file: File): String =
    MediaType
      .forFile(file)
      .getOrElse(MediaType.octetStream)
      .fullName

private case class AttachmentImpl(name: String, kind: String, data: Array[Byte]) extends Attachment:
  override lazy val toString = s"Attachment($name,$kind,size=$size)"
