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

import scala.util.Try

/** Provides utility to get process identifier. */
object GetProcessId:
  /** Tries to get process identifier. */
  def apply(): Try[Int] =
    System.getProperty("os.name").contains("Windows") match
      case true  => Try(windowsProcessId)
      case false => Try(unixProcessId)

  private def unixProcessId: Int =
    ShellProcess("echo $PPID")
      .!!
      .trim
      .toInt

  private def windowsProcessId: Int =
    val candidates = windowsCandidates
    val comparison = windowsCandidates

    val answer = diff(candidates, comparison)
    val verify = diff(comparison, candidates)

    (answer.size == 1 && answer == verify) match
      case true  => answer.head
      case false => throw IOException("Unable to determine process identifier")

  /** Gets Windows parent processes that have single child process as Map[PPID, PID]. */
  private def windowsCandidates: Map[Int, Int] =
    windowsProcesses
      .groupBy(_._2)
      .filter(_._2.size == 1)
      .map((ppid, pids) => ppid -> pids.keys.head)

  /** Gets Windows processes as Map[PID, PPID]. */
  private def windowsProcesses: Map[Int, Int] =
    val process = """\s*(\d+)\s+(\d+)\s*""".r

    ShellProcess("wmic process get ParentProcessId,ProcessId")
      .!!
      .split("\r\n|\r|\n")
      .collect { case process(ppid, pid) => pid.toInt -> ppid.toInt }
      .toMap

  /** Gets keys in `left` whose values are different from corresponding values in `right`. */
  private def diff(left: Map[Int, Int], right: Map[Int, Int]): Seq[Int] =
    left.flatMap { (ppid, pid) =>
      right.get(ppid).filter(_ != pid).map(_ => ppid)
    }.toSeq
