/*
 * Copyright 2017-2018 47 Degrees, LLC. <http://www.47deg.com>
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

import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.WordSpec

import scala.concurrent.duration._
import cats.implicits._
import org.apache.kafka.clients.consumer.ConsumerRecords
import scala.collection.JavaConverters._

class KafkaConsumerSpec extends WordSpec with FSKafkaAlgebraSpec {

  "Consumer can be reused after closed" in {
    withConsumer[String].apply { consumer =>
      for {
        _                 <- consumer.close()
        isClosed          <- consumer.isClosed
        _                 <- consumer.metrics
        isClosedAfterUsed <- consumer.isClosed
      } yield (isClosed, isClosedAfterUsed)
    } shouldBe Right((true, false))
  }

  "Consumer can be reused after closed with a timeout" in {
    withConsumer[String].apply { consumer =>
      for {
        _                 <- consumer.closeWaitingFor(5.seconds)
        isClosed          <- consumer.isClosed
        _                 <- consumer.metrics
        isClosedAfterUsed <- consumer.isClosed
      } yield (isClosed, isClosedAfterUsed)
    } shouldBe Right((true, false))
  }

  "Consumer can subscribe to topics" in {
    val topics = "topicsubscription" :: Nil
    createCustomTopic(topics.head)
    withConsumer[String].apply { consumer =>
      for {
        _      <- consumer.subscribe(topics)
        topics <- consumer.subscription
      } yield topics
    } shouldBe Right(topics)
  }

  "Consumer can read a message from a topic" in {
    val topic   = "mytopic"
    val key     = "key"
    val message = "mymessage"
    withProducerAndConsumer[String].apply { (producer, consumer) =>
      for {
        _       <- producer.sendToTopic(topic, (key, message))
        _       <- producer.flush()
        _       <- consumer.subscribe(topic :: Nil)
        _       <- consumer.commitSync()
        records <- consumer.poll(10.seconds)
        message = records.records(topic).asScala.toList.headOption.map(_.value)
      } yield message
    } shouldBe Right(Some("mymessage"))
  }

  "Consumer can obtain metrics" in {
    withProducer[String].apply { _.metrics }.isRight shouldBe true
  }

}
