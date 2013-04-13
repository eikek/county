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

package org.eknet.county.tree

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.eknet.county.CounterKey

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.04.13 13:24
 */
class TreeSuite extends FunSuite with ShouldMatchers {

  test ("resolve nodes") {
    val root = Tree.create[Int]()
    val x = root.select("a.b|d.c")
    x should have size (2)
    root.select("a") should have size (1)
    root.select("a.b") should have size (1)
    x.foreach(_.data = Some(3))
    root.select("a.b.c") should have size(1)
    root.select("a.b.c")(0).data should be (Some(3))
    root.select("a.b.c")(0).getPath should be (CounterKey("a.b.c"))
    root.select("a.d.c") should have size(1)
    root.select("a.d.c")(0).data should be (Some(3))

    root.select("a.*.c") should be (x)
    root.select("") should be (List(root))
  }

  test ("get parent path") {
    val root = Tree.create[Int]()
    root.parent should be (None)
    root.getPath should be (CounterKey.empty)

    val n1 = root.select("a.b.c")
    n1 should have size (1)
    n1(0).getPath should be (CounterKey("a.b.c"))
    n1(0).parent.get.getPath should be (CounterKey("a.b"))
  }
}
