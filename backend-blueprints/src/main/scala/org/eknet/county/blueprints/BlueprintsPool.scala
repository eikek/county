package org.eknet.county.blueprints

import org.eknet.county.{Granularity, CounterPool}
import com.tinkerpop.blueprints.{Vertex, KeyIndexableGraph}
import java.security.{MessageDigest, DigestOutputStream}
import java.io.ByteArrayOutputStream
import javax.xml.bind.DatatypeConverter

/**
 * Persists all counters in the given graph.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 14:12
 */
class BlueprintsPool(val graph: KeyIndexableGraph, val granularity: Granularity) extends CounterPool {

  val nameIdProp = "name-id"
  val nameProp = "name"

  initializeIndexes()

  implicit private val g = graph

  protected def initializeIndexes() {
    import scala.collection.JavaConversions.asScalaSet
    val set = graph.getIndexedKeys(classOf[Vertex]).filter(k => k == nameIdProp)
    if (!set.contains(nameIdProp)) {
      graph.createKeyIndex(nameIdProp, classOf[Vertex])
    }
  }

  protected def findVertex(name: String): Option[Vertex] = {
    val id = digest(name)
    val iter = graph.getVertices(nameIdProp, id).iterator()
    if (iter.hasNext) {
      val r = Some(iter.next())
      if (iter.hasNext) sys.error("More than one vertex for name "+ name) else r
    } else {
      None
    }
  }

  protected def digest(str: String): String = {
    val bout = new ByteArrayOutputStream()
    val out = new DigestOutputStream(bout, MessageDigest.getInstance("MD5"))
    out.write(str.getBytes("UTF-8"))
    out.close()
    DatatypeConverter.printHexBinary(out.getMessageDigest.digest())
//    val bytes = out.getMessageDigest.digest()
//    val bi = new BigInteger(1, bytes)
//    String.format("%032x", bi)
  }

  def getOrCreate(name: String) = {
    find(name) getOrElse {
      withTx {
        val v = graph.addVertex(null)
        v.setProperty(nameIdProp, digest(name))
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

  protected def createCounter(name: String, v: Vertex) = {
    new VertexCounter(granularity, v, graph)
  }
}
