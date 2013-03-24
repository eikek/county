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

   def countAt(key: TimeKey) = {
     if (counters.isEmpty) 0 else counters.map(_.countAt(key)).reduce(_ + _)
   }

  def add(when: TimeKey, value: Long) {
    counters.foreach(_.add(when, value))
  }

  def resetTime = if (counters.isEmpty) 0 else counters.map(_.resetTime).min

  def lastAccess = if (counters.isEmpty) 0 else counters.map(_.lastAccess).max

  def keys = counters.flatMap(_.keys).toList.sorted
}

class BasicCompositeCounter(val counters: Iterable[Counter]) extends CounterBase with CompositeCounter