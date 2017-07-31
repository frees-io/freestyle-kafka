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

      def close(): FS[Unit]

      def closeWaitingFor(timeout: Duration): FS[Unit]

      def flush(): FS[Unit]

      def metrics: FS[Map[MetricName, Metric]]

      def partitionsFor(topic: Topic): FS[List[PartitionInfo]]

      def send(record: ProducerRecord[K, V]): FS[RecordMetadata]

      def sendMany(records: List[ProducerRecord[K, V]]): FS[List[RecordMetadata]]

      def initTransaction(): FS[Unit]

      def beginTransaction(): FS[Unit]

      def sendOffsetsToTransaction(
          offsets: Map[TopicPartition, OffsetAndMetadata],
          consumerGroupId: String): FS[Unit]

      def commitTransaction(): FS[Unit]

      def abortTransaction(): FS[Unit]

    }

    class KafkaProducerHandler[M[_]](
        implicit config: KafkaProducerConfig[K, V],
        ctx: AsyncContext[M],
        ME: MonadError[M, Throwable])
        extends Producer.Handler[M] {

      private lazy val atomicProducer: AtomicReference[KafkaProducer[K, V]] =
        new AtomicReference(KafkaProducerConfig.producerFromConfig(config))

      private lazy val producerClosedState: AtomicReference[Boolean] =
        new AtomicReference(false)

      def producer: KafkaProducer[K, V] = {
        if (producerClosedState.get()) {
          atomicProducer.set(KafkaProducerConfig.producerFromConfig(config))
          producerClosedState.set(false)
        }
        atomicProducer.get()
      }

      override protected[this] def close: M[Unit] = {
        producerClosedState.set(false)
        ME.catchNonFatal(producer.close())
      }

      override protected[this] def closeWaitingFor(timeout: Duration): M[Unit] = {
        producerClosedState.set(false)
        ME.catchNonFatal(producer.close(timeout.toNanos, TimeUnit.NANOSECONDS))
      }

      override protected[this] def flush: M[Unit] = ME.catchNonFatal(producer.flush())

      override protected[this] def metrics: M[Map[MetricName, Metric]] =
        ME.catchNonFatal(producer.metrics().asScala.toMap[MetricName, Metric])

      override protected[this] def partitionsFor(topic: Topic): M[List[PartitionInfo]] =
        ME.catchNonFatal(producer.partitionsFor(topic).asScala.toList)

      override protected[this] def send(record: ProducerRecord[K, V]): M[RecordMetadata] =
        ctx.runAsync { cb =>
          producer.send(record, (metadata: RecordMetadata, exception: Exception) => {
            if (exception == null) cb(Right(metadata))
            else cb(Left(exception))
          })
        }

      override protected[this] def sendMany(
          records: List[ProducerRecord[K, V]]): M[List[RecordMetadata]] =
        records.traverse(send)

      override protected[this] def initTransaction: M[Unit] =
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

  }

  def apply[K, V] = new KafkaProducerProvider[K, V]

}