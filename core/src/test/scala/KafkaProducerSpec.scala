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

import cats.implicits._
import classy.Decoder
import com.typesafe.config.{Config, ConfigFactory}
import freestyle.async.implicits._
import org.apache.kafka.common.serialization.StringSerializer
import org.scalatest.{AsyncWordSpec, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class KafkaProducerSpec extends AsyncWordSpec with Matchers with FSKafkaAlgebraSpec {

  override implicit def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  "runs with embedded kafka" in {

    withProducer[String, String, Future].inProgram { producer =>
      FreeS.pure(())
    }.futureValue shouldBe (())

  }

  "Producer can be reused after closed" in {

    withProducer[String, String, Future].inProgram { producer =>
      for {
        _                 <- producer.close()
        isClosed          <- producer.isClosed
        _                 <- producer.metrics
        isClosedAfterUsed <- producer.isClosed
      } yield (isClosed, isClosedAfterUsed)
    }.futureValue shouldBe ((true, false))

  }

}
