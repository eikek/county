package org.eknet

import java.util.concurrent.locks.ReentrantReadWriteLock.{WriteLock, ReadLock}
import java.util.concurrent.locks.Lock

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 21:26
 *
 */
package object county {

  private[county] def wrapLock[A](lock: Lock)(body: => A): A = {
    lock.lock()
    try {
      body
    } finally {
      lock.unlock()
    }
  }

  private[county] def lockRead[A](body: => A)(implicit lock: ReadLock): A = wrapLock(lock)(body)
  private[county] def lockWrite[A](body: => A)(implicit lock: WriteLock): A = wrapLock(lock)(body)

  private[county] def upgradeLock[A](body: => A)(implicit rl: ReadLock, wl: WriteLock): A = {
    rl.unlock()
    val result = wrapLock(wl)(body)
    rl.lock()
    result
  }

}
