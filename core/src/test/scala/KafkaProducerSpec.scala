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

import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.WordSpec

import scala.concurrent.duration._

class KafkaProducerSpec extends WordSpec with FSKafkaAlgebraSpec {

  "Producer can be reused after closed" in {
    withProducer[String].inProgram { producer =>
      for {
        _                 <- producer.close()
        isClosed          <- producer.isClosed
        _                 <- producer.metrics
        isClosedAfterUsed <- producer.isClosed
      } yield (isClosed, isClosedAfterUsed)
    } shouldBe Right((true, false))
  }

  "Producer can be reused after closed with a timeout" in {
    withProducer[String].inProgram { producer =>
      for {
        _                 <- producer.closeWaitingFor(5.seconds)
        isClosed          <- producer.isClosed
        _                 <- producer.metrics
        isClosedAfterUsed <- producer.isClosed
      } yield (isClosed, isClosedAfterUsed)
    } shouldBe Right((true, false))
  }

  "Producer can send messages to a topic" in {
    withProducer[String].inProgram { producer =>
      for {
        _       <- producer.sendToTopic("mytopic", ("key", "mymessage"))
        message <- FreeS.pure(EmbeddedKafka.consumeFirstStringMessageFrom("mytopic", true))
      } yield message
    } shouldBe Right("mymessage")
  }

}
