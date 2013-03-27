package org.eknet.county.blueprints

import org.eknet.county.{Granularity, CounterPool}
import com.tinkerpop.blueprints.{Edge, Vertex, KeyIndexableGraph}
import org.eknet.county.backend.Digest

/**
 * Persists all counters in the given graph.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 14:12
 */
class BlueprintsPool(val graph: KeyIndexableGraph, val granularity: Granularity) extends CounterPool {

  import BlueprintsPool.nameIdProp
  import BlueprintsPool.timestampProp
  import BlueprintsPool.nameProp

  implicit private val g = graph

  initializeIndexes()

  protected def initializeIndexes() {
    import scala.collection.JavaConversions.asScalaSet
    graph.getIndexedKeys(classOf[Vertex]).filter(k => k == nameIdProp) match {
      case set if (set.isEmpty) => graph.createKeyIndex(nameIdProp, classOf[Vertex])
      case _ =>
    }
    graph.getIndexedKeys(classOf[Edge]).filter(k => k == timestampProp) match {
      case set if (set.isEmpty) => graph.createKeyIndex(timestampProp, classOf[Edge])
      case _ =>
    }
  }

  protected def findVertex(name: String): Option[Vertex] = {
    val id = createNodeId(name)
    val iter = graph.getVertices(nameIdProp, id).iterator()
    if (iter.hasNext) {
      val r = Some(iter.next())
      if (iter.hasNext) sys.error("More than one vertex for name "+ name) else r
    } else {
      None
    }
  }

  def getOrCreate(name: String) = {
    find(name) getOrElse {
      withTx {
        val v = graph.addVertex(null)
        v.setProperty(nameIdProp, createNodeId(name))
        v.setProperty(nameProp, name)
        createCounter(name, v)
      }
    }
  }

  def find(name: String) = withTx {
    findVertex(name).map(v => createCounter(name, v))
  }

  def remove(name: String) = withTx {
    findVertex(name).map(v => {
      graph.removeVertex(v)
      createCounter(name, v)
    }).isDefined
  }

  protected def createNodeId(name: String) = Digest.digest(name)

  protected def createCounter(name: String, v: Vertex) = {
    new VertexCounter(granularity, v, graph)
  }
}

object BlueprintsPool {
  val nameIdProp = "nameid"
  val timestampProp = "timestampid"
  val nameProp = "name"
}