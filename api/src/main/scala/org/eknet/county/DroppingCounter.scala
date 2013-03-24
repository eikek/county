package org.eknet.county

/**
 * A counter that disregards every access until a specified `interval` has been
 * elapsed.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 19:47
 */
class DroppingCounter(val self: Counter, interval: Long) extends ProxyCounter with CounterBase {

  override def add(when: TimeKey, value: Long) {
    val now = when.timestamp
    if (now - lastAccess > interval) {
      super.add(when, value)
    } else {
      super.add(when, 0L)
    }
  }

}
