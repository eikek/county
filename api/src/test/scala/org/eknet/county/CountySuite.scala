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

  test ("resolve pools") {
    val county = County.create()
    val pool = new BasicCounterPool(Granularity.Millis)
    county.counterFactories = ("a.b.c" -> pool) :: county.counterFactories

    county("a.b.d").increment()
    county("a.b.d").increment()
    county("a.b.c").increment()
    county("a.b.c").increment()

    var pc = county("a.b.c").asInstanceOf[DefaultCounty.Tree].self.asInstanceOf[BasicCounter]
    pc.granularity should be (Granularity.Millis)

    pc = county("a.b.d").asInstanceOf[DefaultCounty.Tree].self.asInstanceOf[BasicCounter]
    pc.granularity should be (Granularity.Minute)
  }

  test ("multi county") {
    val county = County.create()
    county("a.b.c.1").increment()
    county("a.b.c.2").increment()
    county("a.b.d.1").increment()
    county("a.b.e.1").increment()

    county("a.*.c").totalCount should be (2)
    county("a.b.*.1").totalCount should be (3)
  }

  test ("filter keys") {

    val county = County.create()
    county("ips.80-100-100-1").increment()
    county("ips.81-100-100-2").increment()
    county("ips.81-100-100-3").increment()
    county("ips.83-100-100-4").increment()
    county("ips.83-10-100-5").increment()
    county("ips.83-10-100-6").increment()

    val locationMap = Map("80" -> "Germany", "81" -> "Italy", "83" -> "USA")
    val locationFun = (s: String) => locationMap.get(s.take(2)).getOrElse("unknown")
    val ipcounter = county("ips").filterKey(locationFun)
    ipcounter("Germany").totalCount should be (1)
    ipcounter("Italy").totalCount should be (2)
    ipcounter("USA").totalCount should be (3)

    ipcounter.children.toList.sorted should be (List("Germany", "Italy", "USA"))
  }

  test ("transform keys") {
    val county = County.create()

    val locationMap = Map("80" -> "Germany", "81" -> "Italy", "83" -> "USA")
    val locationFun = (s: String) => locationMap.get(s.take(2)).getOrElse("unknown")

    val locationCounter = county("bylocation").transformKey(locationFun)
    locationCounter("80-100-100-1").increment()
    locationCounter("80-100-100-1").increment()
    locationCounter("81-100-100-3").increment()
    locationCounter("81-100-100-4").increment()
    locationCounter("81-100-100-5").increment()
    locationCounter("83-10-100-6").increment()
    locationCounter("83-10-100-7").increment()
    locationCounter("83-10-100-8").increment()
    locationCounter("83-10-100-9").increment()

    county("bylocation").children.toList.sorted should be (List("Germany", "Italy", "USA"))
  }
}
