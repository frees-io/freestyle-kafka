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

import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import cats.MonadError
import cats.implicits._
import freestyle.async.AsyncContext
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.{Metric, MetricName, PartitionInfo, TopicPartition}

import scala.concurrent.duration.Duration
import scala.util.matching.Regex
import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

object consumer {

  final class KafkaConsumerProvider[K, V] {

    @free
    trait Consumer {

      def assignment: FS[List[TopicPartition]]

      def subscription: FS[List[Topic]]

      def subscribe(topics: List[Topic]): FS[Unit]

      def subscribeWithPartitionInfo(
          topics: List[Topic],
          onRevoked: (List[TopicPartition]) => Unit,
          onAssigned: (List[TopicPartition]) => Unit): FS[Unit]

      def assign(partitions: List[TopicPartition]): FS[Unit]

      def subscribeWithPattern(
          regex: Regex,
          onRevoked: (List[TopicPartition]) => Unit,
          onAssigned: (List[TopicPartition]) => Unit): FS[Unit]

      def unsubscribe(): FS[Unit]

      def poll(timeout: Duration): FS[ConsumerRecords[K, V]]

      def commitSync(): FS[Unit]

      def commitSyncWithOffsets(offsets: Map[TopicPartition, OffsetAndMetadata]): FS[Unit]

      def commitAsync(): FS[Unit]

      def commitAsyncWithResult(): FS[Map[TopicPartition, OffsetAndMetadata]]

      def commitAsyncWithOffsets(offsets: Map[TopicPartition, OffsetAndMetadata]): FS[
        Map[TopicPartition, OffsetAndMetadata]]

      def seek(partition: TopicPartition, offset: Long): FS[Unit]

      def seekToBeginning(partitions: List[TopicPartition]): FS[Unit]

      def seekToEnd(partitions: List[TopicPartition]): FS[Unit]

      def position(partition: TopicPartition): FS[Long]

      def committed(partition: TopicPartition): FS[OffsetAndMetadata]

      def metrics: FS[Map[MetricName, Metric]]

      def partitionsFor(topic: Topic): FS[List[PartitionInfo]]

      def listTopics: FS[Map[Topic, List[PartitionInfo]]]

      def paused: FS[List[TopicPartition]]

      def pause(partitions: List[TopicPartition]): FS[Unit]

      def resume(partitions: List[TopicPartition]): FS[Unit]

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Long]): FS[
        Map[TopicPartition, OffsetAndTimestamp]]

      def beginningOffsets(partitions: List[TopicPartition]): FS[Map[TopicPartition, Long]]

      def endOffsets(partitions: List[TopicPartition]): FS[Map[TopicPartition, Long]]

      def close(): FS[Unit]

      def isClosed(): FS[Boolean]

      def closeWaitingFor(timeout: Duration): FS[Unit]

      def wakeup(): FS[Unit]

    }

    class ConsumerRebalanceCallback(
        onRevoked: (List[TopicPartition]) => Unit,
        onAssigned: (List[TopicPartition]) => Unit)
        extends ConsumerRebalanceListener {
      override def onPartitionsRevoked(partitions: util.Collection[TopicPartition]): Unit =
        onRevoked(partitions.asScala.toList)

      override def onPartitionsAssigned(partitions: util.Collection[TopicPartition]): Unit =
        onRevoked(partitions.asScala.toList)
    }

    class AsyncOffsetCommitCallback(
        cb: Either[Throwable, Map[TopicPartition, OffsetAndMetadata]] => Unit)
        extends OffsetCommitCallback {
      override def onComplete(
          offsets: util.Map[TopicPartition, OffsetAndMetadata],
          exception: Exception): Unit = {
        if (exception == null) cb(Right(offsets.asScala.toMap))
        else cb(Left(exception))
      }
    }

    class KafkaConsumerHandler[M[_]](
        implicit underlying: UnderlyingKafkaConsumer[K, V],
        ctx: AsyncContext[M],
        ME: MonadError[M, Throwable])
        extends Consumer.Handler[M] {

      private[this] val cachedConsumer = underlying.consumer

      def consumer: KafkaConsumer[K, V] = {
        if (consumerClosedState.get()) {
          cachedConsumer.wakeup()
          consumerClosedState.set(false)
        }
        cachedConsumer
      }

      private val consumerClosedState: AtomicBoolean =
        new AtomicBoolean

      override protected[this] def assignment: M[List[TopicPartition]] =
        ME.catchNonFatal(consumer.assignment().asScala.toList)

      override protected[this] def subscription: M[List[Topic]] =
        ME.catchNonFatal(consumer.subscription().asScala.toList)

      override protected[this] def subscribe(topics: List[Topic]): M[Unit] =
        ME.catchNonFatal(consumer.subscribe(topics.asJava))

      override protected[this] def subscribeWithPartitionInfo(
          topics: List[Topic],
          onRevoked: (List[TopicPartition]) => Unit,
          onAssigned: (List[TopicPartition]) => Unit): M[Unit] =
        ME.catchNonFatal(
          consumer.subscribe(
            topics.asJava,
            new ConsumerRebalanceCallback(onRevoked, onAssigned)
          ))

      override protected[this] def assign(partitions: List[TopicPartition]): M[Unit] =
        ME.catchNonFatal(consumer.assign(partitions.asJava))

      override protected[this] def subscribeWithPattern(
          regex: Regex,
          onRevoked: (List[TopicPartition]) => Unit,
          onAssigned: (List[TopicPartition]) => Unit): M[Unit] =
        ME.catchNonFatal(
          consumer.subscribe(
            regex.pattern,
            new ConsumerRebalanceCallback(onRevoked, onAssigned)
          ))

      override protected[this] def unsubscribe: M[Unit] =
        ME.catchNonFatal(consumer.unsubscribe())

      override protected[this] def poll(timeout: Duration): M[ConsumerRecords[K, V]] =
        ME.catchNonFatal(consumer.poll(timeout.toMillis))

      override protected[this] def commitSync: M[Unit] =
        ME.catchNonFatal(consumer.commitSync())

      override protected[this] def commitSyncWithOffsets(
          offsets: Map[TopicPartition, OffsetAndMetadata]): M[Unit] =
        ME.catchNonFatal(consumer.commitSync(offsets.asJava))

      override protected[this] def commitAsync: M[Unit] =
        ME.catchNonFatal(consumer.commitAsync())

      override protected[this] def commitAsyncWithResult: M[Map[TopicPartition, OffsetAndMetadata]] =
        ctx.runAsync { cb =>
          consumer.commitAsync(new AsyncOffsetCommitCallback(cb))
        }

      override protected[this] def commitAsyncWithOffsets(
          offsets: Map[TopicPartition, OffsetAndMetadata]): M[
        Map[TopicPartition, OffsetAndMetadata]] =
        ctx.runAsync { cb =>
          consumer.commitAsync(new AsyncOffsetCommitCallback(cb))
        }

      override protected[this] def seek(partition: TopicPartition, offset: Long): M[Unit] =
        ME.catchNonFatal(consumer.seek(partition, offset))

      override protected[this] def seekToBeginning(partitions: List[TopicPartition]): M[Unit] =
        ME.catchNonFatal(consumer.seekToBeginning(partitions.asJava))

      override protected[this] def seekToEnd(partitions: List[TopicPartition]): M[Unit] =
        ME.catchNonFatal(consumer.seekToEnd(partitions.asJava))

      override protected[this] def position(partition: TopicPartition): M[Long] =
        ME.catchNonFatal(consumer.position(partition))

      override protected[this] def committed(partition: TopicPartition): M[OffsetAndMetadata] =
        ME.catchNonFatal(consumer.committed(partition))

      override protected[this] def metrics: M[Map[MetricName, Metric]] =
        ME.catchNonFatal(consumer.metrics.asScala.toMap)

      override protected[this] def partitionsFor(topic: Topic): M[List[PartitionInfo]] =
        ME.catchNonFatal(consumer.partitionsFor(topic).asScala.toList)

      override protected[this] def listTopics: M[Map[Topic, List[PartitionInfo]]] =
        ME.catchNonFatal(consumer.listTopics.asScala.toMap.mapValues(_.asScala.toList))

      override protected[this] def paused: M[List[TopicPartition]] =
        ME.catchNonFatal(consumer.paused.asScala.toList)

      override protected[this] def pause(partitions: List[TopicPartition]): M[Unit] =
        ME.catchNonFatal(consumer.pause(partitions.asJava))

      override protected[this] def resume(partitions: List[TopicPartition]): M[Unit] =
        ME.catchNonFatal(consumer.resume(partitions.asJava))

      override protected[this] def offsetsForTimes(
          timestampsToSearch: Map[TopicPartition, Long]): M[
        Map[TopicPartition, OffsetAndTimestamp]] =
        ME.catchNonFatal(
          consumer
            .offsetsForTimes(timestampsToSearch.mapValues(long2Long).asJava)
            .asScala
            .toMap)

      override protected[this] def beginningOffsets(
          partitions: List[TopicPartition]): M[Map[TopicPartition, Long]] =
        ME.catchNonFatal(
          consumer.beginningOffsets(partitions.asJava).asScala.toMap.mapValues(Long2long))

      override protected[this] def endOffsets(
          partitions: List[TopicPartition]): M[Map[TopicPartition, Long]] =
        ME.catchNonFatal(consumer.endOffsets(partitions.asJava).asScala.toMap.mapValues(Long2long))

      override protected[this] def close: M[Unit] =
        ME.catchNonFatal(consumer.close()).map { _ =>
          consumerClosedState.set(true)
          ()
        }

      override protected[this] def isClosed: M[Boolean] =
        consumerClosedState.get().pure[M]

      override protected[this] def closeWaitingFor(timeout: Duration): M[Unit] =
        ME.catchNonFatal(consumer.close(timeout.toNanos, TimeUnit.NANOSECONDS)).map { _ =>
          consumerClosedState.set(true)
          ()
        }

      override protected[this] def wakeup: M[Unit] =
        ME.catchNonFatal(consumer.wakeup())
    }

    trait Implicits {

      implicit def defaultKafkaConsumerHandler[M[_]](
          implicit underlying: UnderlyingKafkaConsumer[K, V],
          ctx: AsyncContext[M],
          ME: MonadError[M, Throwable]): KafkaConsumerHandler[M] = new KafkaConsumerHandler[M]

    }

    object implicits extends Implicits

  }

  def apply[K, V] = new KafkaConsumerProvider[K, V]

}

trait UnderlyingKafkaConsumer[K, V] {
  def consumer: KafkaConsumer[K, V]
}
