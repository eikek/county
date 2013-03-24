package org.eknet.county

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 01:47
 */
class CountySuite extends FunSuite with ShouldMatchers {

  test ("playing") {

    val county = County.create()
    val pool = new BasicCounterPool(Granularity.Millis)
    county.counterFactories = ("a.b.c" -> pool) :: county.counterFactories

    county("a.b.c").increment()
    county("a.b.c").increment()
    county("a.b.d").increment()
    county("a.b.e").increment()
    county("a.b").totalCount should be (4)
    county("a.*.c").totalCount should  be (2)

    val c = county("a.b").filterKey(s => s+"1")
    c("d1").totalCount should be (1)
    toString
  }
}
