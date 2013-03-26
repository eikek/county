package org.eknet.county.blueprints

import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.matchers.ShouldMatchers
import org.eknet.county.{FileUtils, AbstractPoolSuite, Granularity}
import java.nio.file.Path

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 09:41
 * 
 */
class BlueprintsPoolSuite extends AbstractPoolSuite with FileUtils with BeforeAndAfter {

  private var graphdir: Path = null

  before {
    graphdir = GraphUtil.createGraphDir
  }

  after {
    removeDir(graphdir)
  }

  def createPool() = {
    val g = GraphUtil.createGraph(graphdir)
    new BlueprintsPool(g, Granularity.Second)
  }
}
