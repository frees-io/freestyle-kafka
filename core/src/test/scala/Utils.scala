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
import freestyle.kafka.Utils.processor.FreesKafkaProcessor
import freestyle.kafka.protocol.{processor, publisher, subscriber}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object Utils {

  object processor {

    @free
    @processor
    trait FreesKafkaProcessor {

      @publisher("test publisher") def objectDispatcher(b: Boolean): FS[Unit]

      @subscriber("test subscriber") def objectFetcher(a: Boolean): FS[Unit]
    }

  }

  object handlers {

    object processor {

      class FreesKafkaProcessorHandler[F[_]: Applicative](
          implicit
          publisher: FreesKafkaProcessor.Publisher[F],
          subscriber: FreesKafkaProcessor.Subscriber[F])
          extends FreesKafkaProcessor.Handler[F] {

        override protected[this] def objectDispatcher(b: Boolean): F[Unit] =
          publisher.objectDispatcher(b)

        override protected[this] def objectFetcher(b: Boolean): F[Unit] =
          subscriber.objectFetcher(b)

      }
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

    implicit val freesKafkaSubscriber: FreesKafkaProcessor.Subscriber[Future] =
      FreesKafkaProcessor.subscriber[Future]("test")

    implicit val freesKafkaPublisherHandler: FreesKafkaProcessorHandler[Future] =
      new FreesKafkaProcessorHandler[Future]

    implicit class InterpreterOps[F[_], A](fs: FreeS[F, A])(implicit H: FSHandler[F, Future]) {

      def runF: A = Await.result(fs.interpret[Future], Duration.Inf)

    }

  }

  object implicits extends FreesRuntime

}
