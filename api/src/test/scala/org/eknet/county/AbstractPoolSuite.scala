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

import backend.jdbc.JdbcFlatCounterPool
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{FunSuite, fixture}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.03.13 19:59
 */
abstract class AbstractPoolSuite extends FunSuite with ShouldMatchers {

  def createPool(): CounterPool

  def performSimpleTests(pool: CounterPool) {
    pool.find("not-there") should be (None)

    val c1 = pool.getOrCreate("hello-counter")
    c1.increment()
    c1.totalCount should be (1)

    val c12 = pool.getOrCreate("hello-counter")
    c12.totalCount should be (1)

    val c13 = pool.find("hello-counter")
    c13 should not be (None)

    c13.get.totalCount should be (1)

    pool.remove("sdfsdf") should be (false)
    val rc = pool.remove("hello-counter")
    rc should be (true)
    pool.find("hello-counter") should be (None)
  }

  test ("counter pool crud tests") {
    val pool = createPool()
    performSimpleTests(pool)
  }
}
