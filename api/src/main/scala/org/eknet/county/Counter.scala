package org.eknet.county

/**
 * This is a counter for some property. The counter can be incremented
 * or an arbitary value can be added. The counter is initialized at `0`.
 *
 * A counter will memorize the timestamp of each count. Implementations
 * can choose the granularity. The granularity defines how dense counters
 * are maintained. For example, if the granularity is "minute", then
 * timestamps are normalized to minutes. Values added to the counter
 * within one minute are delegated to the same counter, because it is
 * mapped to the same timestamp (normalized to minutes). You could have
 * one counter per millisecond, or one counter per year.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 22.03.13 20:16
 * 
 */
trait Counter {

  /**
   * Adds the value to this counter using the `when` timestamp
   * as key.
   *
   * @param when
   * @param value
   */
  def add(when: TimeKey, value: Long)

  /**
   * Increments this counters value using the current timestamp
   * as key.
   */
  def increment()

  /**
   * Decrements this counters value using the current timestamp
   * as key.
   *
   */
  def decrement()

  /**
   * Adds the given value to this counter using the current timestamp
   * as key.
   *
   * @param value
   */
  def add(value: Long)

  /**
   * Returns the total value of this counter.
   * @return
   */
  def totalCount: Long

  /**
   * Returns the value corrresponding to all keys in
   * the range of the specified [[org.eknet.county.TimeKey]].
   *
   * @param key
   * @return
   */
  def countAt(key: TimeKey): Long

  /**
   * Resets this counter. All values are removed and the
   * total is set to `0`.
   */
  def reset()

  /**
   * Returns the timestamp of the last call to `reset()`.
   *@return
   */
  def resetTime: Long

  /**
   * Returns the timesampt of the last modification.
   * @return
   */
  def lastAccess: Long

  /**
   * Returns a list of all keys that have a corresponding value.
   *
   * @return
   */
  def keys: Iterable[TimeKey]
}

trait CounterBase extends Counter {

  def increment() {
    add(1L)
  }

  def decrement() {
    add(-1L)
  }

  def add(value: Long) {
    add(TimeKey(System.currentTimeMillis()), value)
  }
}