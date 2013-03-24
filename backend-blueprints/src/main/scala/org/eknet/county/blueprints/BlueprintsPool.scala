package org.eknet.county.blueprints

import org.eknet.county.{Granularity, CounterPool}
import com.tinkerpop.blueprints.{Vertex, KeyIndexableGraph}
import java.security.{MessageDigest, DigestOutputStream}
import java.io.ByteArrayOutputStream

/**
 * Persists all counters in the given graph.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 14:12
 */
class BlueprintsPool(val graph: KeyIndexableGraph, val granularity: Granularity) extends CounterPool {

  private val nameIdProp = "name-id"
  private val nameProp = "name"

  initializeIndexes()

  implicit private val g = graph

  private def initializeIndexes() {
    import scala.collection.JavaConversions.asScalaSet
    val set = graph.getIndexedKeys(classOf[Vertex]).filter(k => k == nameIdProp)
    if (!set.contains(nameIdProp)) {
      graph.createKeyIndex(nameIdProp, classOf[Vertex])
    }
  }

  private def findVertex(name: String): Option[Vertex] = {
    val id = digest(name)
    val iter = graph.getVertices(nameIdProp, id).iterator()
    if (iter.hasNext) {
      if (iter.hasNext) sys.error("More than one vertex for name "+ name)
      else Some(iter.next())
    } else {
      None
    }
  }

  private def digest(str: String): String = {
    val bout = new ByteArrayOutputStream()
    val out = new DigestOutputStream(bout, MessageDigest.getInstance("MD5"))
    out.write(str.getBytes("UTF-8"))
    out.close()
    new String(out.getMessageDigest.digest(), "UTF-8")
  }

  def getOrCreate(name: String) = {
    find(name) getOrElse {
      withTx {
        val v = graph.addVertex(null)
        v.setProperty(nameIdProp, digest(name))
        v.setProperty(nameProp, name)
        createCounter(v)
      }
    }
  }

  def find(name: String) = withTx {
    findVertex(name).map(v => createCounter(v))
  }

  def remove(name: String) = withTx {
    findVertex(name).map(v => {
      graph.removeVertex(v)
      createCounter(v)
    })
  }

  def createCounter(v: Vertex) = {
    new VertexCounter(granularity, v, graph)
  }
}
