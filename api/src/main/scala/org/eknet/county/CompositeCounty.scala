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

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 18:10
 */
trait CompositeCounty extends County with ProxyCounter {

  def counties: Iterable[County]

  def self = new BasicCompositeCounter(counties)

  def apply(names: CounterKey*) = {
    val next = names.flatMap(n => counties.map(c => c(n)))
    val nextPath = if (names.size == 1) names.head else CounterKey("**")
    if (next.size == 1) {
      next(0)
    } else {
      new BasicCompositeCounty(next)
    }
  }

  def filterKey(fun: (String) => String) = {
    new BasicCompositeCounty(path, counties.map(_.filterKey(fun)))
  }

  def transformKey(fun: (String) => String) = {
    new BasicCompositeCounty(path, counties.map(_.transformKey(fun)))
  }

  def children = counties.flatMap(_.children)

  override def toString() = getClass.getSimpleName+"["+path+"]"
}

class BasicCompositeCounty(val path: CounterKey, val counties: Iterable[County]) extends CompositeCounty {

  def this(c: Iterable[County]) = this(DefaultCounty.nextPath(c.map(_.path).toList, CounterKey.empty), c)

  require(!counties.isEmpty, "counties argument must not be empty")
}
