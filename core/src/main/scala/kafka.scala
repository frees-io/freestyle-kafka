/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package freestyle

import classy.Read
import classy.config.ConfigDecoder
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerRecord

package object kafka {

  type ConfigValue[T] = (String, T)
  type Topic          = String

  object ConfigValueDecoder {
    def apply[T](key: String)(implicit R: Read[Config, T]): ConfigDecoder[ConfigValue[T]] =
      R.read(key) map (value => (key, value))
  }

  implicit class TupleSyntax[K, V](val t: (K, V)) extends AnyVal {
    def producerRecord(topic: Topic): ProducerRecord[K, V] =
      new ProducerRecord[K, V](topic, t._1, t._2)
  }

  trait Implicits extends DefaultSerializers with DefaultDeserializers

  object implicits extends Implicits

}
