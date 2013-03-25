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

package org.eknet.county

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 02:13
 */
trait CompositeCounter extends Counter {

  def counters: Iterable[Counter]

  def totalCount = {
    if (counters.isEmpty) 0 else counters.map(_.totalCount).reduce(_ + _)
  }

  def reset() {
    counters.foreach(_.reset())
  }

  def countIn(range: (TimeKey, TimeKey)) = {
    if (counters.isEmpty) 0 else counters.map(_.countIn(range)).reduce(_ + _)
  }

  def add(when: TimeKey, value: Long) {
    counters.foreach(_.add(when, value))
  }

  def resetTime = if (counters.isEmpty) 0 else counters.map(_.resetTime).min

  def lastAccess = if (counters.isEmpty) 0 else counters.map(_.lastAccess).max

  def keys = counters.flatMap(_.keys).toList.distinct.sorted
}

class BasicCompositeCounter(val counters: Iterable[Counter]) extends CounterBase with CompositeCounter