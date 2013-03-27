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

import com.tinkerpop.blueprints.{Direction, Graph, Vertex}
import org.eknet.county.{Granularity, TimeKey, CounterBase}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 14:16
 */
class VertexCounter(gran: Granularity, val vertex: Vertex, graph: Graph) extends CounterBase {
  import scala.collection.JavaConversions._

  implicit private val g = graph

  private val lastReset = "county_lastReset"
  private val lastAccessProp = "county_lastAccess"

  private val timePrefix = "county_time_"

  private val counterValue = "countervalue"
  private val timeKey = "timekey"

  withTx {
    vertex.setProperty(lastReset, System.currentTimeMillis())
    vertex.setProperty(lastAccessProp, 0L)
  }

  def add(when: TimeKey, value: Long) {
    if (value != 0) {
      val normed = gran.keyFor(when.timestamp).timestamp
      withTx {
        vertex.getVertices(Direction.OUT, normed+"").toList match {
          case a::Nil => {
            val cur = a.getProperty[Long](counterValue)
            a.setProperty(timeKey, normed)
            if (cur == null) {
              a.setProperty(counterValue, value)
            } else {
              a.setProperty(counterValue, cur + value)
            }
          }
          case Nil => {
            val a = graph.addVertex(null)
            graph.addEdge(null, vertex, a, normed+"")
            a.setProperty(counterValue, value)
            a.setProperty(timeKey, normed)
          }
          case _ => sys.error("Too many nodes.")
        }
        vertex.setProperty(lastAccessProp, System.currentTimeMillis())
      }
    }
  }

  def totalCount = withTx {
    val list = vertex.getVertices(Direction.OUT).map(v => v.getProperty[Long](counterValue))
    if (list.isEmpty) 0 else list.reduce(_ + _)
  }

  def countIn(range: (TimeKey, TimeKey)) = {
    val list = vertex.getVertices(Direction.OUT)
      .withFilter(v => v.getProperty[Long](timeKey) >= range._1.timestamp && v.getProperty[Long](timeKey) <= range._2.timestamp)
      .map(v => v.getProperty[Long](counterValue))
    if (list.isEmpty) 0 else list.reduce(_ + _)
  }

  def reset() {
    withTx {
      val list = vertex.getVertices(Direction.OUT)
      list.foreach(v => graph.removeVertex(v))
      vertex.setProperty(lastReset, System.currentTimeMillis())
    }
  }

  def resetTime = withTx {
    Option(vertex.getProperty(lastReset).asInstanceOf[Long]).getOrElse(0L)
  }

  def lastAccess = withTx {
    Option(vertex.getProperty(lastAccessProp).asInstanceOf[Long]).getOrElse(0L)
  }

  def keys = withTx {
    vertex.getVertices(Direction.OUT)
      .map(v => TimeKey(v.getProperty[Long](timeKey)))
  }
}
