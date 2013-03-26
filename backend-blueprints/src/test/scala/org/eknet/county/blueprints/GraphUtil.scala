package org.eknet.county.blueprints

import com.tinkerpop.blueprints.impls.orient.OrientGraph
import java.nio.file.{Path, Files}

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 09:41
 * 
 */
object GraphUtil {

  def createGraphDir = Files.createTempDirectory("testdb")

  def createGraph(dir: Path) = {
    new OrientGraph("local:"+ dir.toAbsolutePath.toString)
  }

  def withGraph[A](dir: Path)(body: OrientGraph => A): A = {
    val g = createGraph(dir)
    try {
      body(g)
    } finally {
      g.shutdown()
    }
  }

}
