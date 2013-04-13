/*
 * Copyright 2013 Eike Kettner
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

package org.eknet.county

import reflect.BeanProperty
import org.eknet.county.util.Glob
import org.eknet.county.tree.{TreeCounty, Tree}

/**
 * @param segmentDelimiter the delimiter used to separate segments of a path. the default
 *                         is used if not specified. Note, that if this changes, the other
 *                         CounterKey's parse methods must be used.
 *
 *
 *                         This is used here to match the path -> [[org.eknet.county.CounterPool]]
 *                         mappings.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 17:50
 */
class DefaultCounty(segmentDelimiter: Char = CounterKey.defaultSegmentDelimiter) extends County with ProxyCounty {

  val self: County = new TreeCounty(Tree.create(segmentDelimiter), factory)

  @BeanProperty
  var counterFactories: List[(String, CounterPool)] = List("**" -> new BasicCounterPool)

  private def factory(path: CounterKey) = createCounter(counterFactories)(path)

  private def createCounter(list: List[(String, CounterPool)])(path: CounterKey) = {
    list.find(t => Glob(t._1, segmentDelimiter).matches(path.asString))
      .getOrElse(sys.error("No counter pool avvailable"))
      ._2
  }

}
