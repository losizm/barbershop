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

import barbershop.web.Server as WebServer
import barbershop.logging.Logger
import barbershop.util.CreatePidFile

import java.nio.file.{ Files, Path, Paths }

import scala.collection.mutable.Stack
import scala.sys.ShutdownHookThread
import scala.util.Try

/** Provides entry point to application. */
object Init:
  private lazy val logger = Logger("barbershop.Init")

  /**
   * Starts application.
   *
   * @param args not used
   *
   * @note Blocks until application is shut down.
   */
  def main(args: Array[String]): Unit =
    apply()

  /**
   * Starts application.
   *
   * @note Blocks until application is shut down.
   */
  def apply(): Unit =
    val shutdown = Stack[() => Unit]()

    try
      val pidFile = createPidFile
      shutdown.push(() => deletePidFile(pidFile))

      val webServer = createWebServer
      shutdown.push(() => stopWebServer(webServer))

      webServer.start()
      waitForShutdown
    finally
      shutdown.foreach(function => Try(function()))

  private def createPidFile: Path =
    val fileName = Paths.get(config.getString("barbershop.pid.file"))
    logger.debug(s"""Creating PID file: $fileName""")
    CreatePidFile(fileName)

  private def deletePidFile(file: Path): Unit =
    try
      logger.debug(s"Deleting PID file: $file")
      Files.deleteIfExists(file)
    catch case e: Exception =>
      throw RuntimeException(s"Cannot delete PID file: $file", e)

  private def createWebServer: WebServer =
    logger.debug("Starting web server")
    WebServer(config.getConfig("barbershop"))

  private def stopWebServer(server: WebServer): Unit =
    logger.debug("Stopping web server")
    server.stop()

  private def waitForShutdown: Unit =
    val sleeper = Thread.currentThread

    ShutdownHookThread(
      if sleeper.isAlive then
        logger.debug("Executing shutdown hook")
        sleeper.interrupt()
        sleeper.join(5000)
    )

    try
      logger.debug("Waiting for shutdown signal")
      Thread.sleep(Long.MaxValue)
    catch case _: InterruptedException =>
      logger.debug("Received shutdown signal")
