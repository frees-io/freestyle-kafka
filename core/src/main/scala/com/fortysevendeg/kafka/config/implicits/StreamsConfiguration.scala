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

package com.fortysevendeg.kafka.config.implicits

import classy._
import classy.config._
import com.typesafe.config.Config

trait StreamsConfiguration extends ClassyInstances {

  implicit val highPriorityStreamsConfig: Decoder[Config, Map[String, _]] =
    ConfigValueDecoder[java.util.List[String]]("bootstrap.servers")
      .join(ConfigValueDecoder[String]("application.id").optional)
      .join(ConfigValueDecoder[Int]("replication.factor").optional)
      .join(ConfigValueDecoder[String]("state.dir").optional)

  implicit val mediumPriorityStreamsConfig: Decoder[Config, Map[String, _]] =
    ConfigValueDecoder[Long]("cache.max.bytes.buffering").optional
      .join(ConfigValueDecoder[String]("client.id").optional)
      .join(ConfigValueDecoder[String]("default.key.serde").optional)
      .join(ConfigValueDecoder[String]("default.timestamp.extractor").optional)
      .join(ConfigValueDecoder[String]("default.value.serde").optional)
      .join(ConfigValueDecoder[Int]("num.standby.replicas").optional)
      .join(ConfigValueDecoder[Int]("num.stream.threads").optional)
      .join(ConfigValueDecoder[String]("processing.guarantee").optional)
      .join(ConfigValueDecoder[String]("security.protocol").optional)

  implicit val lowPriorityStreamsConfig: Decoder[Config, Map[String, _]] =
    ConfigValueDecoder[String]("application.server").optional
      .join(ConfigValueDecoder[Int]("buffered.records.per.partition").optional)
      .join(ConfigValueDecoder[Long]("commit.interval.ms").optional)
      .join(ConfigValueDecoder[Long]("connections.max.idle.ms").optional)
      .join(ConfigValueDecoder[String]("key.serde").optional)
      .join(ConfigValueDecoder[Long]("metadata.max.age.ms").optional)
      .join(ConfigValueDecoder[java.util.List[String]]("metric.reporters").optional)
      .join(ConfigValueDecoder[Int]("metrics.num.samples").optional)
      .join(ConfigValueDecoder[String]("metrics.recording.level").optional)
      .join(ConfigValueDecoder[Long]("metrics.sample.window.ms").optional)
      .join(ConfigValueDecoder[String]("partition.grouper").optional)
      .join(ConfigValueDecoder[Long]("poll.ms").optional)
      .join(ConfigValueDecoder[Int]("receive.buffer.bytes").optional)
      .join(ConfigValueDecoder[Long]("reconnect.backoff.max.ms").optional)
      .join(ConfigValueDecoder[Long]("reconnect.backoff.ms").optional)
      .join(ConfigValueDecoder[Int]("request.timeout.ms").optional)
      .join(ConfigValueDecoder[Long]("retry.backoff.ms").optional)
      .join(ConfigValueDecoder[String]("rocksdb.config.setter").optional)
      .join(ConfigValueDecoder[Int]("send.buffer.bytes").optional)
      .join(ConfigValueDecoder[Long]("state.cleanup.delay.ms").optional)
      .join(ConfigValueDecoder[String]("timestamp.extractor").optional)
      .join(ConfigValueDecoder[String]("value.serde").optional)
      .join(ConfigValueDecoder[Long]("windowstore.changelog.additional.retention.ms").optional)
      .join(ConfigValueDecoder[String]("zookeeper.connect").optional)

  implicit val streamsConfig: Decoder[Config, Map[String, AnyRef]] = {
    for {
      highConf        <- highPriorityStreamsConfig
      mediumConf      <- mediumPriorityStreamsConfig
      lowPriorityConf <- lowPriorityStreamsConfig
    } yield highConf ++ mediumConf ++ lowPriorityConf
  }.map(_.asInstanceOf[Map[String, AnyRef]])
}
