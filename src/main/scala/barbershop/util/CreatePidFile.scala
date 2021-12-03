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
package util

import java.io.IOException
import java.nio.file.{ Files, Path, Paths }

import little.io.PathExt

/** Provides utility to create PID file. */
object CreatePidFile:
  /** Creates PID file. */
  def apply(fileName: String): Path =
    apply(Paths.get(fileName))

  /** Creates PID file. */
  def apply(fileName: Path): Path =
    GetProcessId()
      .map(id => Files.createFile(fileName) << id.toString)
      .recover { case error: Exception => fail(fileName, error) }
      .get

  private def fail(fileName: Path, error: Exception): Nothing =
    throw IOException(s"Cannot create PID file: ${fileName.toAbsolutePath}", error)