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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 28.03.13 11:47
 */
class CounterKeySuite extends FunSuite with ShouldMatchers {

  test ("simple paths") {
    val k = CounterKey("a.b.c")
    k.path should have size (3)
    k.asString should be ("a.b.c")
    k.path.map(_.size) should be (List(1,1,1))
  }

  test ("path with multiple selection") {
    var k = CounterKey("a.b|c|d.x")
    k.path should have size (3)
    k.asString should be ("a.b|c|d.x") //order does not matter, must be sorted
    k.path.map(_.size) should be (List(1,3,1))

    k = CounterKey("a.1|b|b")
    k.path should have size (2)
    k.asString should be ("a.1|b")
    k.path.map(_.size) should be (List(1, 2))
  }

  test ("creation") {
    CounterKey.create("hello") should be (CounterKey("hello"))
    CounterKey.create(Seq("hello", "world")) should be (CounterKey("hello|world"))
    CounterKey("") should be (CounterKey.empty)
  }
}
