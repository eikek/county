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
 * @since 23.03.13 14:24
 */
trait ProxyCounter extends Counter with Proxy {
  def self: Counter

  def add(when: TimeKey, value: Long) { self.add(when, value) }
  def increment() { self.increment() }
  def decrement() { self.decrement() }
  def add(value: Long) { self.add(value) }
  def totalCount = self.totalCount
  def countIn(range: (TimeKey, TimeKey)) = self.countIn(range)
  def reset() { self.reset() }
  def resetTime = self.resetTime
  def lastAccess = self.lastAccess
  def keys = self.keys

}
