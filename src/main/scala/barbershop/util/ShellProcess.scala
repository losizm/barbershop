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

import java.nio.file.Path

import scala.sys.process.*

/** Provides utility to create shell process. */
object ShellProcess:
  private val unix    = Seq("sh", "-c")
  private val windows = Seq("cmd.exe", "/c")

  /** Creates shell process with supplied command. */
  def apply(command: String): ProcessBuilder =
    if command == null then
      throw NullPointerException()
    exec(command, None)

  /** Builds shell process with supplied command and working directory. */
  def apply(command: String, directory: Path): ProcessBuilder =
    if command == null || directory == null then
      throw NullPointerException()
    exec(command, Some(directory))

  private def exec(command: String, directory: Option[Path]): ProcessBuilder =
    System.getProperty("os.name").contains("Windows") match
      case true  => Process(windows :+ command, directory.map(_.toFile))
      case false => Process(unix :+ command, directory.map(_.toFile))
