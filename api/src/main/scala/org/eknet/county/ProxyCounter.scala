package org.eknet.county

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 14:24
 */
trait ProxyCounter extends Counter with Proxy {
  def self: Counter

  def add(when: TimeKey, value: Long) { self.add(when, value) }
  def increment() { self.increment() }
  def decrement() { self.decrement() }
  def add(value: Long) { self.add(value) }
  def totalCount = self.totalCount
  def countAt(key: TimeKey) = self.countAt(key)
  def reset() { self.reset() }
  def resetTime = self.resetTime
  def lastAccess = self.lastAccess
  def keys = self.keys

}
