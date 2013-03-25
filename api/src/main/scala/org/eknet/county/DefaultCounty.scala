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

  val self: County = new Tree("_root_", ListBuffer(), factory)

  @BeanProperty
  var counterFactories: List[(String, CounterPool)] = List("**" -> new BasicCounterPool)

  private def factory(path: CounterKey) = createCounter(counterFactories)(path)

  private def createCounter(list: List[(String, CounterPool)])(path: CounterKey) = {
    val pool = list.find(t => Glob(t._1, '.').matches(path.tail.asString))
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
    val name = path.lastSegment

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
        val nextCounty = if (nextSegs.size == 1) nextSegs(0) else new BasicCompositeCounty(path / name.head, nextSegs)
        if (name.size == 1) {
          nextCounty
        } else {
          nextCounty(name.tail)
        }
      }
    }

    def apply(name: CounterKey*): County = {
      val next = name.map(resolveNext)
      if (next.size == 1) next(0) else new BasicCompositeCounty(name.head / "**", next)
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

    def countAt(key: TimeKey) = wrapLock(counterLock.readLock()) {
      self.countAt(key)
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
  }

  private class EmptyCounty(val path: CounterKey) extends County {
    def add(when: TimeKey, value: Long) {}
    def increment() {}
    def decrement() {}
    def add(value: Long) {}
    def totalCount = 0L
    def countAt(key: TimeKey) = 0L
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

  private class FilterKeyCounty(val self: County, val nodes:List[Tree], fun: String => String) extends ProxyCounty {

    private def next(name: String) = {
      val next = nodes.flatMap(_.childList).filter(n => fun(n.name) == name)
      new BasicCompositeCounty(path / name, next)
    }

    override def apply(names: CounterKey*) = {
      val multi = names.map(n => next(n.headSegment))
      val nextPath = if (names.size == 1) names.head else names.head / "**"
      val multiCounty = if (multi.size == 1) multi(0) else new BasicCompositeCounty(path / nextPath, multi)
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

  private class TransformKeyCounty(val self: County, val nodes: List[Tree], fun: String => String) extends ProxyCounty {

    override def apply(name: CounterKey*) = {
      val fn = name.map(n => CounterKey(fun(n.headSegment) :: n.path.tail))
      self.apply(fn: _*)
    }
  }
}
