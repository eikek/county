package org.eknet.county

import collection.mutable.ListBuffer
import annotation.tailrec
import org.eknet.county.DefaultCounty.Tree
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 17:50
 */
class DefaultCounty extends County with ProxyCounty {

  val self: County = new Tree("_root_", ListBuffer(), factory)

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

    private def counterLockRead[A](body: => A): A = {
      counterLock.readLock().lock()
      try {
        body
      } finally {
        counterLock.readLock().unlock()
      }
    }

    private def setupCounter() {
      counterLock.readLock().lock()
      try {
        if (self.isInstanceOf[TreeCounter] && childList.isEmpty) {
          counterLock.readLock().unlock()
          counterLock.writeLock().lock()
          try {
            this.self = factory(path)
          } finally {
            counterLock.writeLock().unlock()
            counterLock.readLock().lock()
          }
        }
      } finally {
        counterLock.readLock().unlock()
      }
    }

    def apply(name: CounterKey) = {
      require(!isLeaf, "Cannot go beyond leaf nodes.")
      childLock.readLock().lock()
      try {
        search(List(this), name) match {
          case Nil => {
            childLock.readLock().unlock()
            childLock.writeLock().lock()
            try {
              create(name)
            } finally {
              childLock.writeLock().unlock()
              childLock.readLock().lock()
            }
          }
          case n::Nil => n
          case all@n::ns => new BasicCompositeCounty(path / name, all)
        }
      } finally {
        childLock.readLock().unlock()
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

    def totalCount = counterLockRead {
      self.totalCount
    }

    def countAt(key: TimeKey) = counterLockRead {
      self.countAt(key)
    }

    def reset() {
      counterLock.writeLock().lock()
      try {
        self.reset()
      } finally {
        counterLock.writeLock().unlock()
      }
    }

    def resetTime = counterLockRead(self.resetTime)
    def lastAccess = counterLockRead(self.lastAccess)
    def keys = counterLockRead(self.keys)

    def filterKey(fun: (String) => String): County = new FilterKeyCounty(this, List(this), fun)

    def transformKey(fun: (String) => String): County = new TransformKeyCounty(this, List(this), fun)

    def children = {
      childLock.readLock().lock()
      try {
        this.childList.map(_.name).toList.sorted
      } finally {
        childLock.readLock().unlock()
      }
    }
  }

  private class TreeCounter(nodes: Iterable[Tree]) extends CounterBase with CompositeCounter {
    def counters = nodes.map(_.self)
  }

  private def search(nodes: List[Tree], path: CounterKey): List[Tree] = {
    def nextTrees(node: Tree, name: String) = {
      val glob = Glob(name, '.')
      node.childList.filter(n => glob.matches(n.path.lastSegment)).toList
    }
    if (path.empty) {
      nodes
    } else {
      nodes flatMap { node =>
        search(nextTrees(node, path.headSegment), path.tail)
      }
    }
  }

  private class FilterKeyCounty(val self: County, val nodes:List[Tree], fun: String => String) extends ProxyCounty {

    private def next(name: String) = {
      val next = nodes.flatMap(_.childList).filter(n => fun(n.name) == name)
      new BasicCompositeCounty(path / name, next)
    }

    override def apply(name: CounterKey) = {
      val multi = next(name.headSegment)
      if (name.size == 1) {
        multi
      } else {
        multi(name.tail)
      }
    }

    override def children = {
      self.children.map(n => fun(n)).toSet
    }
  }

  private class TransformKeyCounty(val self: County, val nodes: List[Tree], fun: String => String) extends ProxyCounty {

    override def apply(name: CounterKey) = {
      val fn = CounterKey(fun(name.headSegment) :: name.path.tail)
      self.apply(fn)
    }
  }
}
