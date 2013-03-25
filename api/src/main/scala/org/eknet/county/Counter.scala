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
 * @author Eike Kettner eike.kettner@gmail.com
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
   * Returns the sum of all values whose keys are in
   * the specified range.
   *
   * @param range
   * @return
   */
  def countIn(range: (TimeKey, TimeKey)): Long

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

  override def increment() {
    add(1L)
  }

  override def decrement() {
    add(-1L)
  }

  override def add(value: Long) {
    add(TimeKey.now, value)
  }
}