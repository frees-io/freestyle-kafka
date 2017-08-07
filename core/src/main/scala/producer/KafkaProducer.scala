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

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import cats.MonadError
import cats.implicits._
import freestyle.async._
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.{Metric, MetricName, PartitionInfo, TopicPartition}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

object producer {

  final class KafkaProducerProvider[K, V] {

    @free
    trait Producer {

      def isClosed: FS[Boolean]

      def close(): FS[Unit]

      def closeWaitingFor(timeout: Duration): FS[Unit]

      def flush(): FS[Unit]

      def metrics: FS[Map[MetricName, Metric]]

      def partitionsFor(topic: Topic): FS[List[PartitionInfo]]

      def send(record: ProducerRecord[K, V]): FS[RecordMetadata]

      def sendToTopic(topic: Topic, record: (K, V)): FS[RecordMetadata]

      def sendMany(records: List[ProducerRecord[K, V]]): FS[List[RecordMetadata]]

      def sendManyToTopic(topic: Topic, records: List[(K, V)]): FS[List[RecordMetadata]]

      def initTransactions(): FS[Unit]

      def beginTransaction(): FS[Unit]

      def sendOffsetsToTransaction(
          offsets: Map[TopicPartition, OffsetAndMetadata],
          consumerGroupId: String): FS[Unit]

      def commitTransaction(): FS[Unit]

      def abortTransaction(): FS[Unit]

    }

    class KafkaProducerHandler[M[_]](
        implicit underlying: UnderlyingKafkaProducer[K, V],
        ctx: AsyncContext[M],
        ME: MonadError[M, Throwable])
        extends Producer.Handler[M] {

      private val atomicProducer: AtomicReference[KafkaProducer[K, V]] =
        new AtomicReference(underlying.producer)

      private val producerClosedState: AtomicReference[Boolean] =
        new AtomicReference(false)

      def producer: KafkaProducer[K, V] = {
        if (producerClosedState.get()) {
          atomicProducer.set(underlying.producer)
          producerClosedState.set(false)
        }
        atomicProducer.get()
      }

      override protected[this] def isClosed: M[Boolean] =
        producerClosedState.get().pure[M]

      override protected[this] def close: M[Unit] = {
        ME.catchNonFatal(producer.close()).map { _ =>
          producerClosedState.set(true)
          ()
        }
      }

      override protected[this] def closeWaitingFor(timeout: Duration): M[Unit] = {
        ME.catchNonFatal(producer.close(timeout.toNanos, TimeUnit.NANOSECONDS)).map { _ =>
          producerClosedState.set(true)
          ()
        }
      }

      override protected[this] def flush: M[Unit] = ME.catchNonFatal(producer.flush())

      override protected[this] def metrics: M[Map[MetricName, Metric]] =
        ME.catchNonFatal(producer.metrics().asScala.toMap[MetricName, Metric])

      override protected[this] def partitionsFor(topic: Topic): M[List[PartitionInfo]] =
        ME.catchNonFatal(producer.partitionsFor(topic).asScala.toList)

      override protected[this] def send(record: ProducerRecord[K, V]): M[RecordMetadata] = {
        ctx.runAsync { cb =>
          producer
            .send(
              record,
              new Callback {
                override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
                  if (exception == null) cb(Right(metadata))
                  else cb(Left(exception))
                }
              }
            )
            .get()
        }
      }

      override protected[this] def sendToTopic(topic: Topic, record: (K, V)): M[RecordMetadata] =
        send(record.producerRecord(topic))

      override protected[this] def sendManyToTopic(
          topic: Topic,
          records: List[(K, V)]): M[List[RecordMetadata]] =
        sendMany(records.map(_.producerRecord(topic)))

      override protected[this] def sendMany(
          records: List[ProducerRecord[K, V]]): M[List[RecordMetadata]] =
        records.traverse(send)

      override protected[this] def initTransactions: M[Unit] =
        ME.catchNonFatal(producer.initTransactions())

      override protected[this] def beginTransaction: M[Unit] =
        ME.catchNonFatal(producer.beginTransaction())

      override protected[this] def sendOffsetsToTransaction(
          offsets: Map[TopicPartition, OffsetAndMetadata],
          consumerGroupId: String): M[Unit] =
        ME.catchNonFatal(producer.sendOffsetsToTransaction(offsets.asJava, consumerGroupId))

      override protected[this] def commitTransaction: M[Unit] =
        ME.catchNonFatal(producer.commitTransaction())

      override protected[this] def abortTransaction: M[Unit] =
        ME.catchNonFatal(producer.abortTransaction())
    }

    trait Implicits {

      implicit def defaultKafkaProducerHandler[M[_]](
          implicit underlying: UnderlyingKafkaProducer[K, V],
          ctx: AsyncContext[M],
          ME: MonadError[M, Throwable]): KafkaProducerHandler[M] = new KafkaProducerHandler[M]

    }

    object implicits extends Implicits

  }

  def apply[K, V] = new KafkaProducerProvider[K, V]

}

trait UnderlyingKafkaProducer[K, V] {
  def producer: KafkaProducer[K, V]
}
