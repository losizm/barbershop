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

import java.util.concurrent.atomic.AtomicLong

import scamper.http.types.MediaType

private object FileNameFactory:
  def create(kind: MediaType): String =
    f"untitled-$sequence%03d.${suffix(kind)}"

  private def sequence: Long =
    nameCount.getAndIncrement % 1000 + 1

  private def suffix(kind: MediaType): String =
    suffixes.getOrElse(kind.fullName, "bin")

  private val nameCount = AtomicLong(0)
  private val suffixes  = Map(
    "application/java"          -> "class",
    "application/java-archive"  -> "jar",
    "application/json"          -> "json",
    "application/octet-stream"  -> "bin",
    "application/pdf"           -> "pdf",
    "application/postscript"    -> "ps",
    "application/rtf"           -> "rtf",
    "application/msword"        -> "doc",
    "application/vnd.ms-excel"  -> "xls",
    "application/vnd.ms-powerpoint" -> "ppt",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx",
    "application/x-compress"    -> "z",
    "application/x-gzip"        -> "gz",
    "application/xhtml+xml"     -> "xhtml",
    "application/xml"           -> "xml",
    "application/xml-dtd"       -> "dtd",
    "application/xslt+xml"      -> "xslt",
    "application/zip"           -> "zip",
    "audio/basic"               -> "au",
    "audio/x-aiff"              -> "aiff",
    "audio/x-midi"              -> "midi",
    "audio/x-mpeg"              -> "mp3",
    "audio/x-wav"               -> "wav",
    "font/otf"                  -> "otf",
    "font/ttf"                  -> "ttf",
    "font/woff"                 -> "woff",
    "font/woff2"                -> "woff2",
    "image/bmp"                 -> "bmp",
    "image/gif"                 -> "gif",
    "image/jpeg"                -> "jpg",
    "image/pict"                -> "pic",
    "image/png"                 -> "png",
    "image/svg+xml"             -> "svg",
    "image/tiff"                -> "tiff",
    "image/vnd.wap.wbmp"        -> "wbmp",
    "image/x-photoshop"         -> "psd",
    "text/css"                  -> "css",
    "text/csv"                  -> "csv",
    "text/html"                 -> "html",
    "text/javascript"           -> "js",
    "text/markdown"             -> "md",
    "text/plain"                -> "txt",
    "text/tab-separated-values" -> "tsv",
    "video/mp4"                 -> "mp4",
    "video/mpeg"                -> "mpg",
    "video/quicktime"           -> "mov",
    "video/x-dv"                -> "dv",
    "video/x-ms-asf"            -> "asf",
    "video/x-ms-wmv"            -> "wmv",
    "video/x-msvideo"           -> "avi"
  )
