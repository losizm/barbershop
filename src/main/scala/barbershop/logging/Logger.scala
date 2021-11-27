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
package logging

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap

/** Defines logger. */
trait Logger extends scamper.logging.Logger:
  /** Logger name. */
  def name: String

  /**
   * Logs trace message.
   *
   * @param message log message
   */
  def trace(message: String): Unit

  /**
   * Logs trace message.
   *
   * @param message log message
   * @param args message arguments
   */
  def trace(message: String, args: Any*): Unit

  /**
   * Logs trace message.
   *
   * @param message log message
   * @param cause Throwable whose stack trace to log
   */
  def trace(message: String, cause: Throwable): Unit

  /**
   * Logs debug message.
   *
   * @param message log message
   */
  def debug(message: String): Unit

  /**
   * Logs debug message.
   *
   * @param message log message
   * @param args message arguments
   */
  def debug(message: String, args: Any*): Unit

  /**
   * Logs debug message.
   *
   * @param message log message
   * @param cause Throwable whose stack trace to log
   */
  def debug(message: String, cause: Throwable): Unit

  /**
   * Logs information message.
   *
   * @param message log message
   */
  def info(message: String): Unit

  /**
   * Logs information message.
   *
   * @param message log message
   * @param args message arguments
   */
  def info(message: String, args: Any*): Unit

  /**
   * Logs information message.
   *
   * @param message log message
   * @param cause Throwable whose stack trace to log
   */
  def info(message: String, cause: Throwable): Unit

  /**
   * Logs warning message.
   *
   * @param message log message
   */
  def warn(message: String): Unit

  /**
   * Logs warning message.
   *
   * @param message log message
   * @param args message arguments
   */
  def warn(message: String, args: Any*): Unit

  /**
   * Logs warning message.
   *
   * @param message log message
   * @param cause Throwable whose stack trace to log
   */
  def warn(message: String, cause: Throwable): Unit

  /**
   * Logs error message.
   *
   * @param message log message
   */
  def error(message: String): Unit

  /**
   * Logs error message.
   *
   * @param message log message
   * @param args message arguments
   */
  def error(message: String, args: Any*): Unit

  /**
   * Logs error message.
   *
   * @param message log message
   * @param cause Throwable whose stack trace to log
   */
  def error(message: String, cause: Throwable): Unit

/** Provides logger factory. */
object Logger:
  /**
   * Gets logger with given name.
   *
   * @param name logger name
   */
  def apply(name: String): Logger =
    loggers.getOrElseUpdate(name, LoggerImpl(name))

  private val loggers = TrieMap[String, Logger]()

  private class LoggerImpl(val name: String) extends Logger:
    private val logger = LoggerFactory.getLogger(name)

    override val toString = s"""Logger("$name")"""

    def trace(message: String) =
      if logger.isTraceEnabled then logger.trace(message)

    def trace(message: String, args: Any*) =
      if logger.isTraceEnabled then logger.trace(message.format(args*))

    def trace(message: String, cause: Throwable) =
      if logger.isTraceEnabled then logger.trace(message, cause)

    def debug(message: String) =
      if logger.isDebugEnabled then logger.debug(message)

    def debug(message: String, args: Any*) =
      if logger.isDebugEnabled then logger.debug(message.format(args*))

    def debug(message: String, cause: Throwable) =
      if logger.isDebugEnabled then logger.debug(message, cause)

    def info(message: String) =
      if logger.isInfoEnabled then logger.info(message)

    def info(message: String, args: Any*) =
      if logger.isInfoEnabled then logger.info(message.format(args*))

    def info(message: String, cause: Throwable) =
      if logger.isInfoEnabled then logger.info(message, cause)

    def warn(message: String) =
      if logger.isWarnEnabled then logger.warn(message)

    def warn(message: String, args: Any*) =
      if logger.isWarnEnabled then logger.warn(message.format(args*))

    def warn(message: String, cause: Throwable) =
      if logger.isWarnEnabled then logger.warn(message, cause)

    def error(message: String) =
      if logger.isErrorEnabled then logger.error(message)

    def error(message: String, args: Any*) =
      if logger.isErrorEnabled then logger.error(message.format(args*))

    def error(message: String, cause: Throwable) =
      if logger.isErrorEnabled then logger.error(message, cause)
