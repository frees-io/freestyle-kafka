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

import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import org.apache.kafka.common.{Metric, MetricName, PartitionInfo}

import scala.concurrent.duration.Duration

@free
trait KafkaProducer {

  def close(): FS[Unit]

  def closeWaitingFor(timeout: Duration): FS[Unit]

  def flush(): FS[Unit]

  def metrics: FS[Map[MetricName, Metric]]

  def partitionsFor(topic: Topic): FS[List[PartitionInfo]]

  def send[K, V](record: ProducerRecord[K, V]): FS[RecordMetadata]

  def sendMany[K, V](records: List[ProducerRecord[K, V]]): FS[List[RecordMetadata]]

}
