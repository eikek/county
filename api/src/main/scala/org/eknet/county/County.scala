package org.eknet.county

import collection.mutable.ListBuffer
import annotation.tailrec

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 18:07
 */
trait County extends Counter {

  def path: CounterKey

  def apply(name: CounterKey): County

  def filterKey(fun: String => String): County

  def transformKey(fun: String => String): County

  def chilren: Iterable[String]
}

object County {

  def create() = new Root


  class Root extends ProxyCounty {

    var counterFactories: List[(String, CounterPool)] = List("**" -> new BasicCounterPool)

    private def factory(path: CounterKey) = createCounter(counterFactories)(path)

    val self: County = new Inner(CounterKey(List()), newInnerNode("_root_"), factory)
  }

  private def createCounter(list: List[(String, CounterPool)])(path: CounterKey) = {
    val pool = list.find(t => Glob(t._1, '.').matches(path.asString))
      .getOrElse(sys.error("No counter pool avvailable"))
      ._2
    pool.getOrCreate(path.asString)
  }

  private def newInnerNode(name: String) = {
    val children = ListBuffer[Node]()
    Node(name, new NodeCounter(children), children, true)
  }

  private class Inner(val path: CounterKey, val node: Node, val factory: CounterKey => Counter) extends ApplyCounty with ProxyCounter {
    require(node.inner, "wrong node type")

    val self = node.counter
    val nodes = List(node)

    @tailrec
    private def createChild(node: Node, name: CounterKey, factory: () => Counter): Node = {
      if (name.size == 1) {
        val next = Node(name.headSegment, factory())
        node.addChild(next)
      } else {
        val next = newInnerNode(name.headSegment)
        createChild(node.addChild(next), name.tail, factory)
      }
    }

    def create(name: CounterKey): County = {
      def counterFactory = () => factory(path / name)
      val next = createChild(node, name, counterFactory)
      if (next.inner) {
        new Inner(path / name, next, factory)
      } else {
        new Leaf(path / name, next)
      }
    }
  }

  private class Leaf(val path: CounterKey, val node: Node) extends County with ProxyCounter {
    require(!node.inner, "wrong node type")
    def this(path: CounterKey, counter: Counter) = this(path, Node(path.lastSegment, counter))

    val self = node.counter
    val nodes = List(node)

    def apply(name: CounterKey) = sys.error("Cannot go beyond leaf nodes")
    def filterKey(fun: (String) => String) = sys.error("Cannot go beyond leaf nodes")
    def transformKey(fun: (String) => String) = sys.error("Cannot go beyond leaf nodes")
    def chilren = List[String]()
  }

  private class Multi(val path: CounterKey, val nodes: List[Node], val factory: CounterKey => Counter) extends CounterBase with ApplyCounty with CompositeCounter {
    val counters = nodes.map(_.counter)

    def create(name: CounterKey) = sys.error("Cannot go further from node collections.")

  }

  private trait ApplyCounty extends County {
    def nodes: List[Node]

    def create(name: CounterKey): County

    def factory: CounterKey => Counter

    def apply(name: CounterKey) = {
      search(nodes, name) match {
        case Nil => create(name)
        case n::Nil => if (n.inner) new Inner(path / name, n, factory) else new Leaf(path / name, n)
        case all@n::ns => new Multi(path / name, all, factory)
      }
    }

    def chilren = nodes.flatMap(_.children).map(_.name)

    def filterKey(fun: (String) => String) = new FilterKeyCounty(this, nodes, fun)

    def transformKey(fun: (String) => String) = new TransformKeyCounty(this, nodes, fun)
  }

  private class NodeCounter(nodes: Iterable[Node]) extends CounterBase with CompositeCounter {
    def counters = nodes.map(_.counter)
  }


  private case class Node(name: String, counter: Counter, children: ListBuffer[Node] = ListBuffer(), inner: Boolean = false) {

    def addChild(node: Node) = {
      findChild(node.name) getOrElse {
        children += node
        node
      }
    }

    def findChild(name: String) = children.find(_.name == name)
  }

  private def search(nodes: List[Node], path: CounterKey): List[Node] = {
    def nextNodes(node: Node, name: String) = {
      val glob = Glob(name, '.')
      node.children.filter(n => glob.matches(n.name)).toList
    }
    if (path.empty) {
      nodes
    } else {
      nodes flatMap { node =>
        search(nextNodes(node, path.headSegment), path.tail)
      }
    }
  }

  private class FilterKeyCounty(val self: ApplyCounty, val nodes:List[Node], fun: String => String) extends ProxyCounty {

    private def next(name: String) = {
      val next = nodes.flatMap(_.children).filter(n => fun(n.name) == name)
      new Multi(path / name, next.toList, self.factory)
    }

    override def apply(name: CounterKey) = {
      val multi = next(name.headSegment)
      if (name.size == 1) {
        multi
      } else {
        multi(name.tail)
      }
    }

    override def chilren = {
      self.chilren.map(n => fun(n)).toSet
    }
  }

  private class TransformKeyCounty(val self: County, val nodes: List[Node], fun: String => String) extends ProxyCounty {

    override def apply(name: CounterKey) = {
      val fn = CounterKey(fun(name.headSegment) :: name.path.tail)
      self.apply(fn)
    }

  }
}