package org.eknet.county

import com.tinkerpop.blueprints.{TransactionalGraph, Graph}
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 24.03.13 14:18
 */
package object blueprints {

  def withTx[A](body: => A)(implicit g: Graph): A = {
    try {
      val r = body
      commit(g, Conclusion.SUCCESS)
      r
    } catch {
      case e: Exception => {
        try {
          commit(g, Conclusion.FAILURE)
        } catch {
          case e2: Exception => e.addSuppressed(e2)
        }
        throw e
      }
    }
  }

  def commit(g: Graph, concl: Conclusion) {
    g match {
      case txg: TransactionalGraph => txg.stopTransaction(concl)
      case _ =>
    }
  }
}
