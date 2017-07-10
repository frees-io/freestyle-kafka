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

trait ProducerConfiguration extends ClassyInstances {

  implicit val highPriorityProducerConfig: Decoder[Config, Map[String, _]] =
    ConfigValueDecoder[java.util.List[String]]("bootstrap.servers")
      .join(ConfigValueDecoder[String]("acks").optional)
      .join(ConfigValueDecoder[Long]("buffer.memory").optional)
      .join(ConfigValueDecoder[String]("compression.type").optional)
      .join(ConfigValueDecoder[Int]("retries").optional)
      .join(ConfigValueDecoder[String]("ssl.key.password").optional)
      .join(ConfigValueDecoder[String]("ssl.keystore.location").optional)
      .join(ConfigValueDecoder[String]("ssl.keystore.password").optional)
      .join(ConfigValueDecoder[String]("ssl.truststore.location").optional)
      .join(ConfigValueDecoder[String]("ssl.truststore.password").optional)

  implicit val mediumPriorityProducerConfig: Decoder[Config, Map[String, _]] =
    ConfigValueDecoder[Int]("batch.size").optional
      .join(ConfigValueDecoder[String]("client.id").optional)
      .join(ConfigValueDecoder[Long]("connections.max.idle.ms").optional)
      .join(ConfigValueDecoder[Long]("linger.ms").optional)
      .join(ConfigValueDecoder[Long]("max.block.ms").optional)
      .join(ConfigValueDecoder[Int]("max.request.size").optional)
      .join(ConfigValueDecoder[String]("partitioner.class").optional)
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

  implicit val lowPriorityProducerConfig: Decoder[Config, Map[String, _]] =
    ConfigValueDecoder[Boolean]("enable.idempotence").optional
      .join(ConfigValueDecoder[java.util.List[String]]("interceptor.classes").optional)
      .join(ConfigValueDecoder[Int]("max.in.flight.requests.per.connection").optional)
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
      .join(ConfigValueDecoder[Int]("transaction.timeout.ms").optional)
      .join(ConfigValueDecoder[String]("transactional.id").optional)

  implicit val producerConfig: Decoder[Config, Map[String, AnyRef]] = {
    for {
      highConf        <- highPriorityProducerConfig
      mediumConf      <- mediumPriorityProducerConfig
      lowPriorityConf <- lowPriorityProducerConfig
    } yield highConf ++ mediumConf ++ lowPriorityConf
  }.map(_.asInstanceOf[Map[String, AnyRef]])
}
