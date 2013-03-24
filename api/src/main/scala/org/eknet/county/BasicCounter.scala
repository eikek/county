package org.eknet.county

import java.util.concurrent.atomic.AtomicLong
import collection.JavaConverters._
import java.util.concurrent.ConcurrentSkipListMap
import java.io.{ObjectOutputStream, IOException, ObjectInputStream}

/**
 * A simple counter that keeps all numbers in memory using a map.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 22.03.13 20:34
 * 
 */
@SerialVersionUID(20130322L)
class BasicCounter(val granularity: Granularity) extends CounterBase with Serializable {

  @transient
  private var map = new ConcurrentSkipListMap[TimeKey, AtomicLong]()

  private val last = new AtomicLong(System.currentTimeMillis())
  private val resettime = new AtomicLong(System.currentTimeMillis())

  def add(when: TimeKey, value: Long) {
    val key = granularity.keyFor(when.timestamp)
    val ex = map.putIfAbsent(key, new AtomicLong(value))
    last.set(System.currentTimeMillis())
    if (ex != null) {
      ex.addAndGet(value)
    } else {
      value
    }
  }

  def totalCount = {
    if (map.isEmpty) 0
    else map.values().asScala.map(_.get()).reduce(_ + _)
  }

  def countAt(key: TimeKey) = {
    val counter = map.subMap(key.interval._1, true, key.interval._2, true).values().asScala
    if (counter.isEmpty) 0 else counter.map(_.get()).reduce(_ + _)
  }

  def reset() {
    map.clear()
    resettime.set(System.currentTimeMillis())
  }

  def keys = map.keySet().asScala

  def resetTime = resettime.get()

  def lastAccess = last.get()

  @throws(classOf[IOException])
  @throws(classOf[ClassNotFoundException])
  private def readObject(stream: ObjectInputStream) {
    stream.defaultReadObject()
    this.map = new ConcurrentSkipListMap()
    var len = stream.readInt()
    while (len > 0) {
      val ar = stream.readObject().asInstanceOf[Array[Long]]
      this.map.putIfAbsent(TimeKey(ar(0)), new AtomicLong(ar(1)))
      len = len -1
    }
  }

  @throws(classOf[IOException])
  private def writeObject(stream: ObjectOutputStream) {
    stream.defaultWriteObject()
    val array = map.entrySet().asScala
    stream.writeInt(array.size)
    for (e <- array) {
      val entry = Array(e.getKey.timestamp, e.getValue.get())
      stream.writeObject(entry)
    }
  }
}
