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

import cats.MonadError
import classy.Decoder
import com.typesafe.config.{Config, ConfigFactory}
import freestyle.async.AsyncContext
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.Serializer
import org.scalatest.{ParallelTestExecution, Suite}
import org.scalatest.concurrent.{ScalaFutures, Waiters}

trait FSKafkaAlgebraSpec extends EmbeddedKafka with ScalaFutures with Waiters with Implicits {
  self: Suite =>

  class withProducer[K, V, M[_]](
      implicit ME: MonadError[M, Throwable],
      AM: AsyncContext[M],
      KD: Serializer[K],
      VS: Serializer[V]) {

    type ProducerType = producer.KafkaProducerProvider[K, V]#Producer[p.Producer.Op]

    implicit val producerDecoderconfig: Decoder[Config, KafkaProducerConfig[K, V]] =
      freestyleKafkaProducerConfig[K, V]

    lazy val producerConfig = ConfigFactory.load().getConfig("kafka.producer")

    implicit val kafkaProducerConfig: KafkaProducerConfig[K, V] =
      Decoder[Config, KafkaProducerConfig[K, V]]
        .decode(producerConfig)
        .fold(e => throw new RuntimeException(e.toPrettyString), { t =>
          t
        })

    val p    = producer[K, V]
    val prod = p.Producer[p.Producer.Op]

    def inProgram[A](body: (ProducerType) => FreeS[p.Producer.Op, A]): M[A] = {
      withRunningKafka {
        val program = body(prod)
        import freestyle._
        program.interpret[M](ME, p.implicits.defaultKafkaProducerHandler[M])
      }
    }

  }

  object withProducer {
    def apply[K, V, M[_]](
        implicit ME: MonadError[M, Throwable],
        AM: AsyncContext[M],
        KD: Serializer[K],
        VS: Serializer[V]) = new withProducer[K, V, M]()
  }

}
