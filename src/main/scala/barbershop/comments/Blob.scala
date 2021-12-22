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

import java.security.MessageDigest

/** Defines blob. */
private trait Blob:
  /** Gets key. */
  def key: String

  /** Gets data. */
  def data: Array[Byte]

  /** Gets size. */
  def size: Int =
    data.size

/** Provides blob factory. */
private object Blob:
  private val digest = MessageDigest.getInstance("MD5")

  /** Creates blob using supplied data. */
  def apply(data: Array[Byte]): Blob =
    BlobImpl(generateKey(data), data)

  private def generateKey(data: Array[Byte]): String =
    digest.synchronized {
      digest.reset()
      digest.digest(data).map(byte => f"$byte%02x").mkString
    }

private case class BlobImpl(key: String, data: Array[Byte]) extends Blob:
  override lazy val toString = s"Blob($key,size=${data.size})"
