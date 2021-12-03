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

import com.typesafe.config.{ Config, ConfigFactory }

private val config = ConfigFactory.load()

extension (config: Config)
  private def getMemorySizeInt(path: String): Int =
    config.getMemorySize(path)
      .toBytes
      .min(Int.MaxValue)
      .toInt

  private def hasPathAny(path: String, more: String*): Boolean =
    config.hasPath(path) || more.exists(config.hasPath)