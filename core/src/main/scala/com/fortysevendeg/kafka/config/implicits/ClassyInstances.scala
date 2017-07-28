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

import classy.Decoder.Join
import classy.Decoder.Join.instance
import classy.Read
import com.typesafe.config.Config

import scala.collection.JavaConverters._

trait ClassyInstances {

  implicit def joinOpt2Map[K, A, B]: Join.Aux[(K, A), Option[(K, B)], Map[K, Any]] =
    instance((a, b) => List(a).toMap ++ b.toList.toMap)

  implicit def joinOptOpt2Map[K, A, B]: Join.Aux[Option[(K, A)], Option[(K, B)], Map[K, Any]] =
    instance((a, b) => a.toList.toMap ++ b.toList.toMap)

  implicit def joinMapOpt2Map[K, A]: Join.Aux[Map[K, _], Option[(K, A)], Map[K, Any]] =
    instance((a, b) => a ++ b.toList.toMap)

  implicit def javaListRead(
      implicit R: Read[Config, List[String]]): Read[Config, java.util.List[String]] =
    Read.instance(path => R.read(path).map(_.asJava))
}
