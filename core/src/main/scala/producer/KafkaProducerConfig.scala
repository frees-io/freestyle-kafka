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
package kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serializer
import collection.JavaConverters._

case class KafkaProducerConfig[K, V](
    configs: Map[String, AnyRef],
    keyValueSerializers: Option[(Serializer[K], Serializer[V])])

object KafkaProducerConfig {

  def producerFromConfig[K, V](config: KafkaProducerConfig[K, V]): KafkaProducer[K, V] =
    config.keyValueSerializers.fold(new KafkaProducer[K, V](config.configs.asJava)) {
      case (ks, vs) =>
        new KafkaProducer[K, V](config.configs.asJava, ks, vs)
    }

}