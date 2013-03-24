package org.eknet.county.blueprints

import com.tinkerpop.blueprints.{Graph, Vertex}
import org.eknet.county.{Granularity, TimeKey, CounterBase}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 14:16
 */
class VertexCounter(gran: Granularity, v: Vertex, graph: Graph) extends CounterBase {
  import scala.collection.JavaConversions.asScalaSet

  implicit private val g = graph

  private val lastReset = "county_lastReset"
  private val lastAccessProp = "county_lastAccess"

  private val timePrefix = "county_time_"

  private def timeKeys = v.getPropertyKeys.filter(n => n.startsWith(timePrefix))

  def add(when: TimeKey, value: Long) {
    val normed = gran.keyFor(when.timestamp)
    withTx {
      val key = timePrefix + normed.timestamp
      val cur = Option(v.getProperty(key)).map(_.asInstanceOf[Long]).getOrElse(0L)
      v.setProperty(key, cur + value)
      v.setProperty(lastAccessProp, System.currentTimeMillis())
    }
  }

  def totalCount = withTx {
    val list = timeKeys map { p => v.getProperty(p).asInstanceOf[Long] }
    if (list.isEmpty) 0 else list.reduce(_ + _)
  }

  def countAt(key: TimeKey) = {
    val list = keys.filter(tk => tk >= key.interval._1 && tk <= key.interval._2)
      .map { k => withTx(v.getProperty(timePrefix+k).asInstanceOf[Long]) }

    if (list.isEmpty) 0 else list.reduce(_ + _)
  }

  def reset() {
    withTx {
      v.setProperty(lastReset, System.currentTimeMillis())
      timeKeys map { p => v.removeProperty(p) }
    }
  }

  def resetTime = withTx {
    Option(v.getProperty(lastReset)).map(_.asInstanceOf[Long]).getOrElse(0L)
  }

  def lastAccess = withTx {
    Option(v.getProperty(lastAccessProp)).map(_.asInstanceOf[Long]).getOrElse(0L)
  }

  def keys = withTx {
    val list = timeKeys map { p => p.substring(timePrefix.length) }
    list.map(m => TimeKey(m.toLong)).toList.sorted
  }
}
