package org.eknet.county.blueprints

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import java.nio.file.Files
import org.eknet.county.Granularity

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 00:23
 */
class VertexCounterSuite extends FunSuite with ShouldMatchers {

  test ("simple tests") {
    GraphUtil.withGraph { g =>
      var counter = new VertexCounter(Granularity.Millis, g.addVertex(null), g)
      counter.increment()
      counter.increment()
      counter.totalCount should be (2)
      counter.vertex.getPropertyKeys.size() should be (3)
      counter.lastAccess should be > 0L
      counter.resetTime should be (0L)

      counter = new VertexCounter(Granularity.Second, g.addVertex(null), g)
      counter.increment()
      counter.increment()
      counter.increment()
      counter.increment()
      counter.totalCount should be (4)
      counter.vertex.getPropertyKeys.size() should be (2)

      counter.reset()
      counter.resetTime should be > 0L
      counter.totalCount should be (0)
      counter.vertex.getPropertyKeys.size() should be (2)
    }
  }
}
