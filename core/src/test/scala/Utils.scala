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

import cats.Applicative
import freestyle.free
import freestyle.kafka.Utils.clientProgram.MyKafkaPublisher
import freestyle.kafka.Utils.processor.FreesKafkaProcessor
import freestyle.kafka.protocol.{processor, publisher}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object Utils {

  object processor {

    @free
    @processor
    trait FreesKafkaProcessor {

      @publisher("test") def objectDispatcher(b: Boolean): FS[Unit]

      //@subscriber("test") def objectFetcher(a: Boolean): FS[Unit]
    }

  }

  object handlers {

    object processor {

      class FreesKafkaProcessorPublisherHandler[F[_]: Applicative](
          implicit publisher: FreesKafkaProcessor.Publisher[F])
          extends MyKafkaPublisher.Handler[F] {

        override protected[this] def objectDispatcher(b: Boolean): F[Unit] =
          publisher.objectDispatcher(b)

      }

    }

  }

  object clientProgram {

    @free
    trait MyKafkaPublisher {
      def objectDispatcher(b: Boolean): FS[Unit]
    }

  }

  trait FreesRuntime {

    import cats.implicits._
    import freestyle._
    import handlers.processor._
    import processor._

    implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

    implicit val freesKafkaPublisher: FreesKafkaProcessor.Publisher[Future] =
      FreesKafkaProcessor.publisher[Future]("test")

    implicit val freesKafkaPublisherHandler: FreesKafkaProcessorPublisherHandler[Future] =
      new FreesKafkaProcessorPublisherHandler[Future]

    implicit class InterpreterOps[F[_], A](fs: FreeS[F, A])(implicit H: FSHandler[F, Future]) {

      def runF: A = Await.result(fs.interpret[Future], Duration.Inf)

    }

  }

  object implicits extends FreesRuntime

}
