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
package internal.processor

import freestyle.internal.ScalametaUtil

import scala.collection.immutable.Seq
import scala.meta.Defn.{Class, Object, Trait}
import scala.meta._
import errors._

object processorImpl {

  def processor(defn: Any): Stat = defn match {
    case Term.Block(Seq(cls: Trait, companion: Object)) =>
      processorManager(cls, companion)
    case Term.Block(Seq(cls: Class, companion: Object)) if ScalametaUtil.isAbstract(cls) =>
      processorManager(cls, companion)
    case _ =>
      abort(s"$invalid. $abstractOnly")
  }

  def processorManager(alg: Defn, companion: Object): Term.Block = {
    val processorAlg = ProcessorAlg(alg)
    Term.Block(Seq(alg, enrich(processorAlg, companion)))
  }

  def enrich(processorAlg: ProcessorAlg, companion: Object): Object = companion match {
    case q"..$mods object $ename extends $template" =>
      template match {
        case template"{ ..$earlyInit } with ..$inits { $self => ..$stats }" =>
          val enrichedTemplate =
            template"{ ..$earlyInit } with ..$inits { $self => ..${enrich(processorAlg, stats)} }"
          q"..$mods object $ename extends $enrichedTemplate"
      }
  }

  def enrich(processorAlg: ProcessorAlg, members: Seq[Stat]): Seq[Stat] =
    members ++ Seq(processorAlg.publisher, processorAlg.publisherInstance)

}

case class ProcessorAlg(defn: Defn) {

  val (algName, template) = defn match {
    case c: Class => (c.name, c.templ)
    case t: Trait => (t.name, t.templ)
  }

  val wartSuppress =
    mod"""@_root_.java.lang.SuppressWarnings(_root_.scala.Array("org.wartremover.warts.DefaultArguments"))"""

  val publisherName: Type.Name = Type.Name("Publisher")

  private[this] def paramTpe(param: Term.Param): Type = {
    val Term.Param(_, paramname, Some(ptpe), _) = param
    val targ"${tpe: Type}"                      = ptpe
    tpe
  }

  // format: OFF
  val modes: List[KafkaPublisher] = template.stats.toList.flatten.collect {
    case q"@publisher($s) def $name[..$tparams]($request): FS[$response]" =>
      KafkaPublisher(algName, s, name, paramTpe(request), response)
    case e => throw new MatchError("Unmatched freestyle-kafka method: " + e.toString())
  }
  // format: ON

  private[internal] def getTypedArg(s: Term.Arg): String = s match {
    case q"$value" => value.toString()
  }

  val publisher: Class = {
    val publisherDefs: Seq[Defn.Def] = modes.map(_.publisherDef)
    q"""
       $wartSuppress
       class $publisherName[M[_]](topic: String) {
          ..$publisherDefs
       }
     """
  }

  val publisherInstance: Defn.Def = {
    q"""
       $wartSuppress
       def publisher[M[_]](topic: String): $publisherName[M] =
             new ${publisherName.ctorRef(Ctor.Name(publisherName.value))}[M](topic)
     """
  }

}

private[internal] case class KafkaPublisher(
    algName: Type.Name,
    topicName: Term.Arg,
    name: Term.Name,
    requestType: Type,
    responseType: Type) {

  val wartSuppress =
    mod"""@_root_.java.lang.SuppressWarnings(_root_.scala.Array("org.wartremover.warts.Null"))"""

  val descriptorName: Term.Name = name.copy(value = name.value + "MethodDescriptor")

  val publisherDef: Defn.Def =
    q"""
         def $name(b: $requestType)(implicit A: Applicative[M]): M[$responseType] = {
           println($topicName)
           A.pure(println(s"######## testing publisherDef"))
         }
       """
}

private[internal] object errors {
  val invalid = "Invalid use of `@processor`"
  val abstractOnly =
    "`@processor` can only annotate a trait or abstract class already annotated with @free"
}
