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
 * @since 13.04.13 14:50
 */
class FilteredCounty(val self: County, fun: String => String) extends ProxyCounty {

  override def apply(names: CounterKey*) = {
    //maps the first segment to a list of children
    //select children c, where seg == fun(c.name)
    val mappedKeys = names.flatMap(mapKey)
    if (mappedKeys.isEmpty) {
      County.newEmptyCounty(names.map(n => path / n): _*)
    } else {
      super.apply(mappedKeys: _*)
    }
  }

  override def children = super.children.map(c => fun(c)).toList.distinct.sorted

  private def mapKey(key: CounterKey) = {
    key.headSegment.flatMap(findRealChildren) match {
      case Nil => None
      case list => Some(CounterKey(List(list) ::: key.tail.path))
    }
  }

  private def findRealChildren(seg: String) = {
    super.children.filter(c => fun(c) == seg)
  }

  override def remove(names: CounterKey*) {
    val mappedKeys = names.flatMap(mapKey)
    if (!mappedKeys.isEmpty) {
      super.remove(mappedKeys: _*)
    }
  }
}
