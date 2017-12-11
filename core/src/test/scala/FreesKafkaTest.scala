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

package freestyle.kafka

import freestyle._
import freestyle.kafka.Utils.processor.FreesKafkaProcessor
import org.scalatest._

class FreesKafkaTests extends WordSpec with Matchers {

  import freestyle.kafka.Utils.implicits._

  "frees-kafka publisher" should {

    import freestyle.kafka.implicits._

    "macro defined function works" in {

      def clientProgram[M[_]](implicit APP: FreesKafkaProcessor[M]): FreeS[M, Unit] =
        APP.objectDispatcher(true)

      clientProgram[FreesKafkaProcessor.Op].runF shouldBe (())

    }
  }

  "frees-kafka subscriber" should {

    import freestyle.kafka.implicits._

    "macro defined function works" in {

      def clientProgram[M[_]](implicit APP: FreesKafkaProcessor[M]): FreeS[M, Unit] =
        APP.objectFetcher(true)

      clientProgram[FreesKafkaProcessor.Op].runF shouldBe (())

    }
  }

}
