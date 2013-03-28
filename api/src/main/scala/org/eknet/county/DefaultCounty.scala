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

import collection.mutable.ListBuffer
import annotation.tailrec
import org.eknet.county.DefaultCounty.Tree
import java.util.concurrent.locks.ReentrantReadWriteLock
import reflect.BeanProperty

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 17:50
 */
class DefaultCounty extends County with ProxyCounty {

  val self: County = new Tree(CounterKey.empty, ListBuffer(), factory)

  @BeanProperty
  var counterFactories: List[(String, CounterPool)] = List("**" -> new BasicCounterPool)

  private def factory(path: CounterKey) = createCounter(counterFactories)(path)

  private def createCounter(list: List[(String, CounterPool)])(path: CounterKey) = {
    val pool = list.find(t => Glob(t._1, '.').matches(path.asString))
      .getOrElse(sys.error("No counter pool avvailable"))
      ._2
    pool.getOrCreate(path.asString)
  }

}

object DefaultCounty {

  private[county] class Tree(val path: CounterKey, val childList: ListBuffer[Tree], factory: CounterKey => Counter) extends County {

    var self: Counter = new TreeCounter(childList)
    private val counterLock = new ReentrantReadWriteLock()
    private val childLock = new ReentrantReadWriteLock()
    val name = if (path.empty) "_root_" else path.lastSegment

    private def isLeaf = childList.isEmpty && !self.isInstanceOf[TreeCounter]

    private def setupCounter() {
      implicit val rl = counterLock.readLock()
      implicit val wl = counterLock.writeLock()
      lockRead {
        if (self.isInstanceOf[TreeCounter] && childList.isEmpty) {
          upgradeLock {
            this.self = factory(path)
          }
        }
      }
    }

    private def nextChildren(name: String) = childList.filter { c =>
      Glob(name, '.').matches(c.name)
    }.toList

    private def resolveNext(name: CounterKey): County = {
      require(!isLeaf, "Cannot go beyond leaf nodes.")
      implicit val rl = childLock.readLock()
      implicit val wl = childLock.writeLock()
      lockRead {
        val nextSegs = nextChildren(name.headSegment) match {
          case Nil => {
            if (name.hasWildcard) {
              //return an empty counter
              List(new EmptyCounty(path / name))
            } else {
              upgradeLock {
                List(addChild(new Tree(path / name.head, ListBuffer(), factory)))
              }
            }
          }
          case list => list
        }
        val nextCounty = if (nextSegs.size == 1) nextSegs(0) else new BasicCompositeCounty(nextSegs)
        if (name.size == 1) {
          nextCounty
        } else {
          nextCounty(name.tail)
        }
      }
    }

    def apply(name: CounterKey*): County = {
      val next = name.map(resolveNext)
      if (next.size == 1) next(0) else {
        if (name.size == 1)
          new BasicCompositeCounty(name(0), next)
        else
          new BasicCompositeCounty(next)
      }
    }

    def findChild(name: String) = childList.find(c => c.name == name)
    def addChild(node: Tree) = childList.find(c => c.name == node.name) getOrElse {
      childList += node
      node
    }

    @tailrec
    private def create(name: CounterKey): County = {
      val t = new Tree(path / name.head, ListBuffer(), factory)
      if (name.size == 1) {
        addChild(t)
      } else {
        addChild(t).create(name.tail)
      }
    }

    def add(when: TimeKey, value: Long) {
      setupCounter()
      self.add(when, value)
    }

    def increment() {
      setupCounter()
      self.increment()
    }

    def decrement() {
      setupCounter()
      self.decrement()
    }

    def add(value: Long) {
      setupCounter()
      self.add(value)
    }

    def totalCount = wrapLock(counterLock.readLock()) {
      self.totalCount
    }

    def countIn(range: (TimeKey, TimeKey)) = wrapLock(counterLock.readLock()) {
      self.countIn(range)
    }

    def reset() {
      wrapLock(counterLock.writeLock()) {
        self.reset()
      }
    }

    def resetTime = wrapLock(counterLock.readLock())(self.resetTime)
    def lastAccess = wrapLock(counterLock.readLock())(self.lastAccess)
    def keys = wrapLock(counterLock.readLock())(self.keys)

    def filterKey(fun: (String) => String): County = new FilterKeyCounty(this, List(this), fun)

    def transformKey(fun: (String) => String): County = new TransformKeyCounty(this, List(this), fun)

    def children = wrapLock(counterLock.readLock()) {
      this.childList.map(_.name).toList.sorted
    }

    override def toString = if (isLeaf) {
      self.toString
    } else {
      "Tree["+path+"]"
    }
  }

  private class EmptyCounty(val path: CounterKey) extends County {
    def add(when: TimeKey, value: Long) {}
    def increment() {}
    def decrement() {}
    def add(value: Long) {}
    def totalCount = 0L
    def countIn(range: (TimeKey, TimeKey)) = 0L
    def reset() {}
    def resetTime = 0L
    def lastAccess = 0L
    def keys = List[TimeKey]()
    def apply(name: CounterKey*) = if (name.size == 1) new EmptyCounty(path/name.head) else new EmptyCounty(path/name.head/"**")
    def filterKey(fun: (String) => String) = this
    def transformKey(fun: (String) => String) = this
    def children = List[String]()
  }

  private class TreeCounter(nodes: Iterable[Tree]) extends CounterBase with CompositeCounter {
    def counters = nodes.map(_.self)
  }

  private[county] class FilterKeyCounty(val self: County, val nodes:List[Tree], fun: String => String) extends ProxyCounty {

    private def next(name: String) = {
      val next = nodes.flatMap(_.childList).filter(n => fun(n.name) == name)
      if (next.size == 1) next(0) else new BasicCompositeCounty(next)
    }

    override def apply(names: CounterKey*) = {
      val multi = names.map(n => next(n.headSegment))
      val multiCounty = if (multi.size == 1) multi(0) else new BasicCompositeCounty(multi)
      if (names.size == 1) {
        multiCounty
      } else {
        multiCounty(names.map(_.tail): _*)
      }
    }

    override def children = {
      self.children.map(n => fun(n)).toSet
    }
  }

  private[county] class TransformKeyCounty(val self: County, val nodes: List[Tree], fun: String => String) extends ProxyCounty {

    override def apply(name: CounterKey*) = {
      val fn = name.map(n => CounterKey(fun(n.headSegment) :: n.path.tail))
      self.apply(fn: _*)
    }
  }

  /**
   * Finds a consolidated key for a list of keys. It compares elemnts of same
   * index and creates a new key by mapping each element to either itself, if
   * it is equal in all keys, or the `*` wildcard, if it's not equal in all keys.
   *
   * @param names
   * @param fin
   * @return
   */
  @tailrec
  private[county] def nextPath(names: List[CounterKey], fin: CounterKey): CounterKey = names match {
    case a::Nil => a
    case a::as if (a.empty) => fin
    case a::as => {
      val equal = as.map(ck => ck.head == a.head).reduce(_ && _)
      if (equal) {
        nextPath(names.map(_.tail), fin / a.head)
      } else {
        nextPath(names.map(_.tail), fin / "*")
      }
    }
  }
}
