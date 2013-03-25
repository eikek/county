package org.eknet.county.blueprints

import com.tinkerpop.blueprints.impls.orient.OrientGraph
import java.nio.file.Files

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 09:41
 * 
 */
object GraphUtil {

  def withGraph[A](body: OrientGraph => A): A = {
    val dir = Files.createTempDirectory("testdb")
    val g = new OrientGraph("local:"+ dir.toAbsolutePath.toString)
    try {
      body(g)
    } finally {
      g.shutdown()
    }
  }

}
