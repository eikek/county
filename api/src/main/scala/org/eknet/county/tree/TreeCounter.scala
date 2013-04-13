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

package org.eknet.county.tree

import org.eknet.county.{CompositeCounter, CounterBase, Counter}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.04.13 23:21
 */
class TreeCounter (node: Tree[Counter]) extends CounterBase with CompositeCounter {
  def counters = node.data.map(c => List(c)).getOrElse(node.getChildren.map(n => new TreeCounter(n)))
}

object TreeCounter {

  implicit def nodeToCounter(node: Tree[Counter]) = new TreeCounter(node)

}
