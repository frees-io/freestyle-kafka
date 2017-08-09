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
import cats.instances.either._
import freestyle.async.{AsyncContext, Proc}
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.scalatest._
import org.scalatest.concurrent.{ScalaFutures, Waiters}

trait FSKafkaAlgebraSpec
    extends EmbeddedKafka
    with Waiters
    with Matchers
    with Implicits
    with BeforeAndAfterAll { self: Suite =>

  type Target[A] = Either[Throwable, A]

  abstract override def run(testName: Option[String], args: Args) = {
    EmbeddedKafka.start()
    val result = super.run(testName, args)
    EmbeddedKafka.stop()
    result
  }

  implicit val eitherTestAsyncContext: AsyncContext[Target] = new AsyncContext[Target] {
    override def runAsync[A](fa: Proc[A]): Target[A] = {
      var result: Target[A] = Left(new IllegalStateException("callback did not return"))
      fa(_.fold(e => result = Left(e), a => result = Right(a)))
      result
    }
  }

  class withProducer[V](implicit VS: Serializer[V]) {

    val p: producer.KafkaProducerProvider[String, V] = producer[String, V]
    val prod: p.Producer[p.Producer.Op]              = p.Producer[p.Producer.Op]
    type ProducerType = producer.KafkaProducerProvider[String, V]#Producer[p.Producer.Op]

    def inProgram[A](body: (ProducerType) => FreeS[p.Producer.Op, A]): Target[A] = {
      val program = body(prod)
      import freestyle._
      implicit def kafkaProducer: UnderlyingKafkaProducer[String, V] =
        new UnderlyingKafkaProducer[String, V] {
          override def producer: KafkaProducer[String, V] =
            aKafkaProducer.thatSerializesValuesWith(VS.getClass)
        }

      val f = program.interpret[Target](
        MonadError[Target, Throwable],
        p.implicits.defaultKafkaProducerHandler[Target])
      f
    }
  }

  object withProducer {
    def apply[V](implicit VD: Serializer[V]) = new withProducer[V]()
  }

  class withConsumer[V](implicit VS: Deserializer[V]) {

    val p: consumer.KafkaConsumerProvider[String, V] = consumer[String, V]
    val prod: p.Consumer[p.Consumer.Op]              = p.Consumer[p.Consumer.Op]
    type ConsumerType = consumer.KafkaConsumerProvider[String, V]#Consumer[p.Consumer.Op]

    def inProgram[A](body: (ConsumerType) => FreeS[p.Consumer.Op, A]): Target[A] = {
      val program = body(prod)
      import freestyle._
      implicit def underlyingKafkaConsumer: UnderlyingKafkaConsumer[String, V] =
        new UnderlyingKafkaConsumer[String, V] {
          override def consumer: KafkaConsumer[String, V] =
            kafkaConsumer[String, V]
        }

      val f = program.interpret[Target](
        MonadError[Target, Throwable],
        p.implicits.defaultKafkaConsumerHandler[Target])
      f
    }
  }

  object withConsumer {
    def apply[V](implicit VD: Deserializer[V]) = new withConsumer[V]()
  }

}
