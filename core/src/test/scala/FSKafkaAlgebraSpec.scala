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
import cats.implicits._
import freestyle.async.{AsyncContext, Proc}
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.scalatest._
import org.scalatest.concurrent.{ScalaFutures, Waiters}
import freestyle._
import freestyle.implicits._

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

  implicit def underlyingKafkaProducer[V](
      implicit VS: Serializer[V]): UnderlyingKafkaProducer[String, V] =
    new UnderlyingKafkaProducer[String, V] {
      override def producer: KafkaProducer[String, V] =
        aKafkaProducer.thatSerializesValuesWith(VS.getClass)
    }

  implicit def underlyingKafkaConsumer[V](
      implicit VS: Deserializer[V]): UnderlyingKafkaConsumer[String, V] =
    new UnderlyingKafkaConsumer[String, V] {
      override def consumer: KafkaConsumer[String, V] =
        kafkaConsumer[String, V]
    }

  class withProducer[V](implicit VS: Serializer[V]) {

    val p: producer.KafkaProducerProvider[String, V] = producer[String, V]
    import p.implicits._
    val prod: p.Producer[p.Producer.Op] = p.Producer[p.Producer.Op]
    type ProducerType = producer.KafkaProducerProvider[String, V]#Producer[p.Producer.Op]

    def apply[A](body: (ProducerType) => FreeS[p.Producer.Op, A]): Target[A] = {
      val program = body(prod)
      program.interpret[Target]
    }
  }

  object withProducer {
    def apply[V](implicit VD: Serializer[V]) = new withProducer[V]()
  }

  class withConsumer[V](implicit VS: Deserializer[V]) {

    val c: consumer.KafkaConsumerProvider[String, V] = consumer[String, V]
    import c.implicits._
    val cons: c.Consumer[c.Consumer.Op] = c.Consumer[c.Consumer.Op]
    type ConsumerType = consumer.KafkaConsumerProvider[String, V]#Consumer[c.Consumer.Op]

    def apply[A](body: (ConsumerType) => FreeS[c.Consumer.Op, A]): Target[A] = {
      val program = body(cons)
      program.interpret[Target]
    }
  }

  object withConsumer {
    def apply[V](implicit VD: Deserializer[V]) = new withConsumer[V]()
  }

  class withProducerAndConsumer[V](implicit VS: Serializer[V], VD: Deserializer[V]) {

    val c: consumer.KafkaConsumerProvider[String, V] = consumer[String, V]
    val p: producer.KafkaProducerProvider[String, V] = producer[String, V]

    import c.implicits._
    import p.implicits._

    @module
    trait ConsumerAndProducer {
      val consumer: c.Consumer
      val producer: p.Producer
    }

    type ConsumerType = consumer.KafkaConsumerProvider[String, V]#Consumer[ConsumerAndProducer.Op]
    type ProducerType = producer.KafkaProducerProvider[String, V]#Producer[ConsumerAndProducer.Op]
    val cp = ConsumerAndProducer[ConsumerAndProducer.Op]

    def apply[A](
        body: (ProducerType, ConsumerType) => FreeS[ConsumerAndProducer.Op, A]): Target[A] = {
      val program = body(cp.producer, cp.consumer)
      program.interpret[Target]
    }
  }

  object withProducerAndConsumer {
    def apply[V](implicit VS: Serializer[V], VD: Deserializer[V]) =
      new withProducerAndConsumer[V]()
  }

}
