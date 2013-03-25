/*
 * Copyright 2013 Eike Kettner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eknet.county.blueprints

import com.tinkerpop.blueprints.{Graph, Vertex}
import org.eknet.county.{Granularity, TimeKey, CounterBase}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 14:16
 */
class VertexCounter(gran: Granularity, val vertex: Vertex, graph: Graph) extends CounterBase {
  import scala.collection.JavaConversions.asScalaSet

  implicit private val g = graph

  private val lastReset = "county_lastReset"
  private val lastAccessProp = "county_lastAccess"

  private val timePrefix = "county_time_"

  private def timeKeys = vertex.getPropertyKeys.filter(n => n.startsWith(timePrefix)).toList

  def add(when: TimeKey, value: Long) {
    val normed = gran.keyFor(when.timestamp)
    withTx {
      val key = timePrefix + normed.timestamp
      val cur = Option(vertex.getProperty(key).asInstanceOf[Long]).getOrElse(0L)
      vertex.setProperty(key, cur + value)
      vertex.setProperty(lastAccessProp, System.currentTimeMillis())
    }
  }

  def totalCount = withTx {
    val list = timeKeys map { p => vertex.getProperty(p).asInstanceOf[Long] }
    if (list.isEmpty) 0 else list.reduce(_ + _)
  }

  def countIn(range: (TimeKey, TimeKey)) = {
    val list = keys.filter(tk => tk >= range._1 && tk <= range._2)
      .map { k => withTx(vertex.getProperty(timePrefix+k).asInstanceOf[Long]) }

    if (list.isEmpty) 0 else list.reduce(_ + _)
  }

  def reset() {
    withTx {
      vertex.setProperty(lastReset, System.currentTimeMillis())
      timeKeys map { p => vertex.removeProperty(p).asInstanceOf[Long] }
    }
  }

  def resetTime = withTx {
    Option(vertex.getProperty(lastReset).asInstanceOf[Long]).getOrElse(0L)
  }

  def lastAccess = withTx {
    Option(vertex.getProperty(lastAccessProp).asInstanceOf[Long]).getOrElse(0L)
  }

  def keys = withTx {
    val list = timeKeys map { p => p.substring(timePrefix.length) }
    list.map(m => TimeKey(m.toLong)).toList.sorted
  }
}
