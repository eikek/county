package org.eknet.county.blueprints

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.eknet.county.Granularity

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 09:41
 * 
 */
class BlueprintsPoolSuite extends FunSuite with ShouldMatchers {

  test ("simple crud tests") {
    GraphUtil.withGraph { g =>
      val pool = new BlueprintsPool(g, Granularity.Second)
      pool.find("not-there") should be (None)

      val c1 = pool.getOrCreate("hello-counter")
      c1.increment()
      c1.totalCount should be (1)

      val c12 = pool.getOrCreate("hello-counter")
      c12.totalCount should be (1)

      val c13 = pool.find("hello-counter")
      c13 should not be (None)

      c13.get.totalCount should be (1)

      pool.remove("sdfsdf") should be (None)
      val rc = pool.remove("hello-counter")
      rc should not be (None)
      pool.find("hello-counter") should be (None)
    }
  }
}
