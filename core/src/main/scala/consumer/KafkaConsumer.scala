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

import org.apache.kafka.clients.consumer.{
  ConsumerRecords,
  KafkaConsumer,
  OffsetAndMetadata,
  OffsetAndTimestamp
}
import org.apache.kafka.common.{Metric, MetricName, PartitionInfo, TopicPartition}

import scala.concurrent.duration.Duration
import scala.util.matching.Regex

case class PartitionRebalancedInfo(revoked: List[TopicPartition], assigned: List[TopicPartition])

object consumer {

  final class KafkaConsumerProvider[K, V] {

    @free
    trait KafkaConsumer {

      def assignment: FS[Set[TopicPartition]]

      def subscription: FS[Set[Topic]]

      def subscribe(topics: Iterable[Topic]): FS[Unit]

      def subscribeWithPartitionInfo(topics: Iterable[Topic]): FS[PartitionRebalancedInfo]

      def assign(partitions: Iterable[TopicPartition]): FS[Unit]

      def subscribeWithPattern(regex: Regex): FS[PartitionRebalancedInfo]

      def unsubscribe(): FS[Unit]

      def poll(timeout: Duration): FS[ConsumerRecords[K, V]]

      def commitSync(): FS[Unit]

      def commitSyncWithOffsets(offsets: Map[TopicPartition, OffsetAndMetadata]): FS[Unit]

      def commitAsync(): FS[Unit]

      def commitAsyncWithResult(): FS[Either[Throwable, Map[TopicPartition, OffsetAndMetadata]]]

      def commitAsyncWithOffsets(offsets: Map[TopicPartition, OffsetAndMetadata]): FS[
        Either[Throwable, Map[TopicPartition, OffsetAndMetadata]]]

      def seek(partition: TopicPartition, offset: Long): FS[Unit]

      def seekToBeginning(partitions: Iterable[TopicPartition]): FS[Unit]

      def seekToEnd(partitions: Iterable[TopicPartition]): FS[Unit]

      def position(partition: TopicPartition): FS[Long]

      def committed(partition: TopicPartition): FS[OffsetAndMetadata]

      def metrics: FS[Map[MetricName, Metric]]

      def partitionsFor(topic: Topic): FS[List[PartitionInfo]]

      def listTopics: FS[Map[Topic, List[PartitionInfo]]]

      def paused: FS[Set[TopicPartition]]

      def pause(partitions: Iterable[TopicPartition]): FS[Unit]

      def resume(partitions: Iterable[TopicPartition]): FS[Unit]

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Long]): FS[
        Map[TopicPartition, OffsetAndTimestamp]]

      def beginningOffsets(partitions: Iterable[TopicPartition]): FS[Map[TopicPartition, Long]]

      def endOffsets(partitions: Iterable[TopicPartition]): FS[Map[TopicPartition, Long]]

      def close(): FS[Unit]

      def closeWaitingFor(timeout: Duration): FS[Unit]

      def wakeup(): FS[Unit]

    }

  }

  def apply[K, V] = new KafkaConsumerProvider[K, V]

}

trait UnderlyingKafkaConsumer[K, V] {
  def consumer: KafkaConsumer[K, V]
}
