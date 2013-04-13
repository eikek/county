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

package org.eknet.county.util

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock.{WriteLock, ReadLock}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.04.13 13:39
 */
object locks {

  def wrapLock[A](lock: Lock)(body: => A): A = {
    lock.lock()
    try {
      body
    } finally {
      lock.unlock()
    }
  }

  def lockRead[A](body: => A)(implicit lock: ReadLock): A = wrapLock(lock)(body)

  def lockWrite[A](body: => A)(implicit lock: WriteLock): A = wrapLock(lock)(body)

  def upgradeLock[A](body: => A)(implicit rl: ReadLock, wl: WriteLock): A = {
    rl.unlock()
    val result = wrapLock(wl)(body)
    rl.lock()
    result
  }
}
