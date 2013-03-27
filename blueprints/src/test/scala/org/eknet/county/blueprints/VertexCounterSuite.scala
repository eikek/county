package org.eknet.county.blueprints

import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import java.nio.file.{Path, Files}
import org.eknet.county.{FileUtils, AbstractCounterSuite, Granularity}
import com.tinkerpop.blueprints.Direction

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 00:23
 */
class VertexCounterSuite extends AbstractCounterSuite with FileUtils with BeforeAndAfter {

  private var graphdir: Path = null

  before {
    graphdir = GraphUtil.createGraphDir
  }

  after {
    removeDir(graphdir)
  }

  def createCounter(gran: Granularity) = {
    val pool = new BlueprintsPool(GraphUtil.createGraph(graphdir), gran)
    pool.getOrCreate("test-counter")
  }

  test ("granularity tests 1") {
    import collection.JavaConversions._
    val counter = createCounter(Granularity.Millis)
    counter.increment()
    counter.increment()
    counter.totalCount should be (2)
    counter.vertex.getEdges(Direction.OUT).toList should have size (2)
  }

  test ("granularity 2") {
    import collection.JavaConversions._
    val counter = createCounter(Granularity.Second)
    counter.increment()
    counter.increment()
    counter.increment()
    counter.increment()
    counter.totalCount should be (4)
    counter.vertex.getEdges(Direction.OUT).toList should have size (1)

    counter.reset()
    counter.resetTime should be > 0L
    counter.totalCount should be (0)
    counter.vertex.getEdges(Direction.OUT).toList should have size (0)
  }
}
