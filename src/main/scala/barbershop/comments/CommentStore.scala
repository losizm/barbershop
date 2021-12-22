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

import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

import scala.math.Ordered.orderingToOrdered

import scamper.http.QueryString

/** Defines comment store. */
class CommentStore:
  private val bundle = CommentStoreBundle()
  private val lastId = AtomicLong(0)

  import bundle.{ comments, descriptors, blobs, attachments }

  /** Tests whether store is empty. */
  def isEmpty: Boolean = comments.isEmpty

  /** Gets current store size. */
  def size: Int = comments.size

  /**
   * Loads comments from specified file.
   *
   * @param file input file
   *
   * @return this comment store
   *
   * @note Comments are cleared before loading.
   */
  def load(file: Path): this.type = synchronized {
    clear()
    bundle.read(file)
    resetLastId()
    this
  }

  /**
   * Saves comments to specified file.
   *
   * @param file output file
   *
   * @return this comment store
   */
  def save(file: Path): this.type = synchronized {
    compact()
    bundle.write(file)
    this
  }

  /**
   * Compacts comment store.
   *
   * @return this comment store
   */
  def compact(): this.type = synchronized {
    val ids = comments.values.flatMap(_.attachments.map(_.id)).toSet
    descriptors.filterInPlace((id, _) => ids.contains(id))
    attachments.filterInPlace((id, _) => descriptors.contains(id))

    val keys = attachments.values.toSet
    blobs.filterInPlace((key, _) => keys.contains(key))

    this
  }

  /**
   * Clears all comments.
   *
   * @return this comment store
   */
  def clear(): this.type = synchronized {
    comments.clear()
    descriptors.clear()
    blobs.clear()
    attachments.clear()
    this
  }

  /**
   * List comments.
   *
   * The following query parameters are accepted:
   *
   * - `minId` &ndash; lower bound of comment identifier
   * - `maxId` &ndash; upper bound of comment identifier
   * - `minTime` &ndash; lower bound of comment time; supplied as Epoch
   *   milliseconds or timestamp formatted as `yyyy-MM-ddTmm:hh:ssZ`
   * - `maxTime` &ndash; upper bound of comment time; supplied as Epoch
   *   milliseconds or timestamp formatted as `yyyy-MM-ddTmm:hh:ssZ`
   * - `offset` &ndash; number of leading comments to drop
   * - `limit` &ndash; maximum number of comments to list
   *
   * @param query query parameters
   */
  def list(query: QueryString): Seq[Comment] =
    val minId   = query.getLongOrElse("minId", 0)
    val maxId   = query.getLongOrElse("maxId", Long.MaxValue)
    val minTime = query.getInstantOrElse("minTime", Instant.MIN)
    val maxTime = query.getInstantOrElse("maxTime", Instant.MAX)
    val offset  = query.getIntOrElse("offset", 0)
    val limit   = query.getIntOrElse("limit", Int.MaxValue)

    comments.values
      .filter(comment => comment.id.between(minId, maxId) && comment.time.between(minTime, maxTime))
      .toSeq
      .sortBy(_._1)
      .drop(offset)
      .take(limit)

  /**
   * Adds comment.
   *
   * @return identifer
   */
  def add(text: String): Long =
    add(text, Nil)

  /**
   * Adds comment with attachments.
   *
   * @return identifer
   */
  def add(text: String, files: Seq[Attachment]): Long = synchronized {
    val id          = lastId.incrementAndGet()
    val attachments = files.map(addAttachment)

    comments += id -> Comment(id, text, attachments)
    id
  }

  /**
   * Adds comment with attachments.
   *
   * @return identifer
   */
  def add(text: String, file: Attachment, more: Attachment*): Long =
    add(text, file +: more)

  /** Gets comment. */
  def get(id: Long): Option[Comment] =
    comments.get(id)

  /**
   * Updates comment.
   *
   * @return `true` if comment was updated; otherwise, `false`
   */
  def update(id: Long, text: String): Boolean =
    update(id, text, Nil)

  /**
   * Updates comment with attachments.
   *
   * @return `true` if comment was updated; otherwise, `false`
   */
  def update(id: Long, text: String, files: Seq[Attachment]): Boolean = synchronized {
    comments.remove(id)
      .map(old => old.attachments.foreach(removeAttachment))
      .map(_   => comments += id -> Comment(id, text, files.map(addAttachment)))
      .isDefined
  }

  /**
   * Updates comment with attachments.
   *
   * @return `true` if comment was updated; otherwise, `false`
   */
  def update(id: Long, text: String, file: Attachment, more: Attachment*): Boolean =
    update(id, text, file +: more)

  /**
   * Deletes comment.
   *
   * @return `true` if comment was deleted; otherwise, `false`
   */
  def remove(id: Long): Boolean =
    synchronized(comments.remove(id).isDefined)

  /** Gets attachment. */
  def getAttachment(id: Long): Option[Attachment] =
    for
      desc    <- descriptors.get(id)
      blobKey <- attachments.get(id)
      blob    <- blobs.get(blobKey)
    yield
      Attachment(desc.name, desc.kind, blob.data)

  extension [T](value: T)(using ord: Ordering[T])
    private def between(min: T, max: T) =
      value >= min && value <= max

  private def resetLastId(): Unit =
    val lastCommentId    = comments.keySet.maxOption.getOrElse(0L)
    val lastDescriptorId = descriptors.keySet.maxOption.getOrElse(0L)
    lastId.set(lastCommentId.max(lastDescriptorId))

  private def addAttachment(file: Attachment): FileDescriptor =
    val desc = FileDescriptor(
      id   = lastId.incrementAndGet(),
      name = file.name,
      kind = file.kind,
      size = file.size
    )
    val blob = Blob(file.data)

    descriptors += desc.id -> desc
    blobs       += blob.key -> blob
    attachments += desc.id -> blob.key
    desc

  private def removeAttachment(file: FileDescriptor): Unit =
    descriptors.remove(file.id)
    attachments.remove(file.id)
