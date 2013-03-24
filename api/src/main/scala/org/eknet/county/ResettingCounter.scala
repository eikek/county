package org.eknet.county

/**
 * A counter that starts over if the last time it was modified was longer ago
 * than the given `interval`.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 02:06
 */
class ResettingCounter(val self: Counter, interval: Long) extends ProxyCounter with CounterBase {

  private[this] def checkAndReset(when: TimeKey) {
    val now = when.timestamp
    if (now - lastAccess > interval) {
      reset()
    }
  }

  override def add(when: TimeKey, value: Long) {
    checkAndReset(when)
    self.add(when, value)
  }

}
