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

import org.eknet.county._
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.04.13 23:09
 */
class TreeCounty (val node: Tree[Counter], factory: CounterKey => CounterPool) extends County {
  private val counterLock = new ReentrantReadWriteLock()
  import org.eknet.county.util.locks._
  import TreeCounter.nodeToCounter

  def path = node.getPath

  def apply(names: CounterKey*) = {
    names.toList.flatMap(node.select) match {
      case Nil => County.newEmptyCounty(names.map(n => path / n): _*)
      case a::Nil => if (a == node) this else new TreeCounty(a, factory)
      case list => new BasicCompositeCounty(list.map(n => new TreeCounty(n, factory)))
    }
  }

  def remove(names: CounterKey*) {
    //must first reset all affected counters
    //todo or better remove counter from pool?
    names.toList.flatMap(node.select) foreach { _.reset() }
    //remove the nodes
    names.foreach(node.remove)
    //reset node data (the counter)
    wrapLock(counterLock.writeLock()) {
      node.data = None
    }
  }

  def filterKey(fun: (String) => String) = new FilteredCounty(this, fun)

  def transformKey(fun: (String) => String) = new TransformedCounty(this, fun)

  def children = node.getChildren.map(_.name)

  private def setupCounter() {
    implicit val rl = counterLock.readLock()
    implicit val wl = counterLock.writeLock()
    if (!node.hasChildren) {
      lockRead {
        if (node.data.isEmpty) {
          upgradeLock {
            node.data = Some(factory(path).getOrCreate(path.asString))
          }
        }
      }
    }
  }

  def add(when: TimeKey, value: Long) {
    setupCounter()
    node.add(when, value)
  }

  def increment() {
    setupCounter()
    node.increment()
  }

  def decrement() {
    setupCounter()
    node.decrement()
  }

  def add(value: Long) {
    setupCounter()
    node.add(value)
  }

  def totalCount = wrapLock(counterLock.readLock()) {
    node.totalCount
  }

  def countIn(range: (TimeKey, TimeKey)) = wrapLock(counterLock.readLock()) {
    node.countIn(range)
  }

  def reset() {
    wrapLock(counterLock.readLock()) {
      node.reset()
    }
  }

  def resetTime = wrapLock(counterLock.readLock()) { node.resetTime }

  def lastAccess = wrapLock(counterLock.readLock()) { node.lastAccess }

  def keys = wrapLock(counterLock.readLock()) { node.keys }
}

object TreeCounty {

  def findTree(county: County) = county match {
    case tc: TreeCounty => Some(tc.node)
    case _ => None
  }
}