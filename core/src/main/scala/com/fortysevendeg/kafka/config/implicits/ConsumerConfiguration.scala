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

trait ConsumerConfiguration extends ClassyInstances {

  implicit val highPriorityConsumerConfig: Decoder[Config, Map[String, Any]] =
    ConfigValueDecoder[java.util.List[String]]("bootstrap.servers")
      .join(ConfigValueDecoder[Int]("fetch.min.bytes").optional)
      .join(ConfigValueDecoder[String]("group.id").optional)
      .join(ConfigValueDecoder[Int]("heartbeat.interval.ms").optional)
      .join(ConfigValueDecoder[Int]("max.partition.fetch.bytes").optional)
      .join(ConfigValueDecoder[Int]("session.timeout.ms").optional)
      .join(ConfigValueDecoder[String]("ssl.key.password").optional)
      .join(ConfigValueDecoder[String]("ssl.keystore.location").optional)
      .join(ConfigValueDecoder[String]("ssl.keystore.password").optional)
      .join(ConfigValueDecoder[String]("ssl.truststore.location").optional)
      .join(ConfigValueDecoder[String]("ssl.truststore.password").optional)

  implicit val mediumPriorityConsumerConfig: Decoder[Config, Map[String, Any]] =
    ConfigValueDecoder[String]("auto.offset.reset").optional
      .join(ConfigValueDecoder[Long]("connections.max.idle.ms").optional)
      .join(ConfigValueDecoder[Boolean]("enable.auto.commit").optional)
      .join(ConfigValueDecoder[Boolean]("exclude.internal.topics").optional)
      .join(ConfigValueDecoder[Int]("fetch.max.bytes").optional)
      .join(ConfigValueDecoder[String]("isolation.level").optional)
      .join(ConfigValueDecoder[Int]("max.poll.interval.ms").optional)
      .join(ConfigValueDecoder[Int]("max.poll.records").optional)
      .join(ConfigValueDecoder[java.util.List[String]]("partition.assignment.strategy").optional)
      .join(ConfigValueDecoder[Int]("receive.buffer.bytes").optional)
      .join(ConfigValueDecoder[Int]("request.timeout.ms").optional)
      .join(ConfigValueDecoder[String]("sasl.jaas.config").optional)
      .join(ConfigValueDecoder[String]("sasl.kerberos.service.name").optional)
      .join(ConfigValueDecoder[String]("sasl.mechanism").optional)
      .join(ConfigValueDecoder[String]("security.protocol").optional)
      .join(ConfigValueDecoder[Int]("send.buffer.bytes").optional)
      .join(ConfigValueDecoder[java.util.List[String]]("ssl.enabled.protocols").optional)
      .join(ConfigValueDecoder[String]("ssl.keystore.type").optional)
      .join(ConfigValueDecoder[String]("ssl.protocol").optional)
      .join(ConfigValueDecoder[String]("ssl.provider").optional)
      .join(ConfigValueDecoder[String]("ssl.truststore.type").optional)

  implicit val lowPriorityConsumerConfig: Decoder[Config, Map[String, Any]] =
    ConfigValueDecoder[Int]("auto.commit.interval.ms").optional
      .join(ConfigValueDecoder[Boolean]("check.crcs").optional)
      .join(ConfigValueDecoder[String]("client.id").optional)
      .join(ConfigValueDecoder[Int]("fetch.max.wait.ms").optional)
      .join(ConfigValueDecoder[java.util.List[String]]("interceptor.classes").optional)
      .join(ConfigValueDecoder[Long]("metadata.max.age.ms").optional)
      .join(ConfigValueDecoder[java.util.List[String]]("metric.reporters").optional)
      .join(ConfigValueDecoder[Int]("metrics.num.samples").optional)
      .join(ConfigValueDecoder[String]("metrics.recording.level").optional)
      .join(ConfigValueDecoder[Long]("metrics.sample.window.ms").optional)
      .join(ConfigValueDecoder[Long]("reconnect.backoff.max.ms").optional)
      .join(ConfigValueDecoder[Long]("reconnect.backoff.ms").optional)
      .join(ConfigValueDecoder[Long]("retry.backoff.ms").optional)
      .join(ConfigValueDecoder[String]("sasl.kerberos.kinit.cmd").optional)
      .join(ConfigValueDecoder[Long]("sasl.kerberos.min.time.before.relogin").optional)
      .join(ConfigValueDecoder[Double]("sasl.kerberos.ticket.renew.jitter").optional)
      .join(ConfigValueDecoder[Double]("sasl.kerberos.ticket.renew.window.factor").optional)
      .join(ConfigValueDecoder[java.util.List[String]]("ssl.cipher.suites").optional)
      .join(ConfigValueDecoder[String]("ssl.endpoint.identification.algorithm").optional)
      .join(ConfigValueDecoder[String]("ssl.keymanager.algorithm").optional)
      .join(ConfigValueDecoder[String]("ssl.secure.random.implementation").optional)
      .join(ConfigValueDecoder[String]("ssl.trustmanager.algorithm").optional)

  implicit val consumerConfig: Decoder[Config, Map[String, AnyRef]] = {
    for {
      highConf        <- highPriorityConsumerConfig
      mediumConf      <- mediumPriorityConsumerConfig
      lowPriorityConf <- lowPriorityConsumerConfig
    } yield highConf ++ mediumConf ++ lowPriorityConf
  }.map(_.asInstanceOf[Map[String, AnyRef]])
}
