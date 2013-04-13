package org.eknet.county

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import scala.annotation.tailrec
import org.eknet.county.tree.TreeCounty

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
    county("a.b.c").totalCount should  be (2)
    county("a.b").totalCount should  be (2)
    county("a").totalCount should  be (2)
    county.totalCount should  be (2)
    county("a.b.d").increment()
    county("a.b.e").increment()
    county("a.b.c").path should be (CounterKey("a.b.c"))
    county("a.b").totalCount should be (4)
    county("a.*.c").totalCount should  be (2)

    val c = county("a.b").filterKey(s => s+"1")
    c("d1").totalCount should be (1)
  }

  test ("counting inner nodes") {
    val county = County.create()
    county("a.b.1|2|3.z").add(2)
    county.totalCount should be (6)
    county("a.b").totalCount should be (6)
    county("a.b.1").totalCount should be (2)
    county("a.b.2").totalCount should be (2)
    county("a.b.3").totalCount should be (2)

    county.remove("*")
    county("a.b.c").increment()
    county("a.b.d").increment()
    county("a.b.e").increment()
    county("a.b").totalCount should be (3)
    county("a").totalCount should be (3)
    county.totalCount should be (3)
  }

  test ("select nodes") {
    val c = County.create()
    c("a.b.c").increment()
    c("a.b").children should have size (1)
    c("a.b")("c").path should be (CounterKey("a.b.c"))

    c("a.x.c").increment()
    c("a.x.q").increment()
    c("a.y.c").increment()
    c("a.y.q").increment()
    val m = c("a.*.c")
    m.totalCount should be (3)
    m.asInstanceOf[CompositeCounty].counties should have size (3)

    val m2 = c("a.x|y.c")
    m2.totalCount should be (2)
    m2.asInstanceOf[CompositeCounty].counties should have size (2)

    val m3 = c("a.x|y")
    m3.asInstanceOf[CompositeCounty].counties should have size (2)
    m3.totalCount should be (4)

    val m4 = c("a")
    m4("x|y").asInstanceOf[CompositeCounty].counties should have size (2)
    m4("x|y").totalCount should be (4)

    c("o.p|q").increment()
    c("o").totalCount should be (2)
    c("o").children should have size (2)

    c("z.*.1").increment()
    c("z").totalCount should be (0)
    c("z").children should have size (0)
  }

  test ("resolve pools") {
    val county = County.create()
    val pool = new BasicCounterPool(Granularity.Millis)
    county.counterFactories = ("a.b.c" -> pool) :: county.counterFactories

    county("a.b.d").increment()
    county("a.b.d").increment()
    county("a.b.c").increment()
    county("a.b.c").increment()

    var pc = TreeCounty.findTree(county("a.b.c")).get.data.get.asInstanceOf[BasicCounter]
    pc.granularity should be (Granularity.Millis)

    pc = TreeCounty.findTree(county("a.b.d")).get.data.get.asInstanceOf[BasicCounter]
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

    val empty = county("x.*.a")
    empty.totalCount should be (0)
    empty.increment()
    empty.totalCount should be (0)

    val more = county("a.*.c", "a.b.d")
    more.totalCount should be (3)

    county("a.*.c").path should be (CounterKey("a.b.c"))
    county("a.b.*.1").path should be (CounterKey("a.b.c|d|e.1"))
    county("a.b.c.1", "a.b.c.2", "a.b.d.1").path should be (CounterKey("a.b.c|d.1|2"))
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
    val france = ipcounter("France")
    france.totalCount should be (0)
    france.path.toString should be ("ips.France")
    france.increment() //does nothing!
    france.totalCount should be (0)

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

  test ("create children of composite counters") {
    val county = County.create()

    val cc = county("a.b.c", "a.x.v")
    cc("help").increment()
    cc.totalCount should be (2)
    county("a.b.c.help").totalCount should be (1)
    county("a.b").totalCount should be (1)
  }

  test ("calculate next paths from multiple") {
    var multiple = List(CounterKey("a.c.1"), CounterKey("a.d.1"), CounterKey("a.e.3"))
    var next = CounterKey.mergePaths(multiple, CounterKey.empty)
    next.asString should be ("a.c|d|e.1|3")

    multiple = List(CounterKey("a.c.1"), CounterKey("a.d.1"), CounterKey("a.e.1"))
    next = CounterKey.mergePaths(multiple, CounterKey.empty)
    next.asString should be ("a.c|d|e.1")
  }

  test ("select multiple children") {

    val county = County.create()
    county("a.h|k|l").increment()
    county("a").children should have size (3)
    county("a.h").totalCount should be (1)
    county("a.k").totalCount should be (1)
    county("a.l").totalCount should be (1)
  }

  test ("remove nodes") {
    val c = County.create()

    c("a.b.c").increment()
    c("a.b.c").totalCount should be (1)

    val x = County.create()
    x("a.b.c|d").add(3)
    x("a.b.c").totalCount should be (3)
    x("a.b.d").totalCount should be (3)

    x.remove("*")
    x("a.b.c|d").increment()
    x("a.b.c").totalCount should be (1)
    x("a.b.d").totalCount should be (1)

    c.remove("*")
    c("a.b.c").increment()
    c("a.b.d").increment()
    c("a.b.e").increment()

    c("a.b").totalCount should be (3)
    c("a.b").remove("c|e")
    c("a.b").totalCount should be (1)
    c("a.b").children should have size (1)

    c.remove("*")
    c.totalCount should be (0)
    c.children should have size (0)

    c("a.b.c|d|e").increment()
    c("a.b").totalCount should be (3)
    c("a").remove("b.c|d")
    c("a").totalCount should be(1)
    c("a").children should have size(1)
    c("a.b").children should have size(1)
    c("a.b").children.toList(0) should be ("e")

    c.remove("*")
    c("a.b.c|d|e").add(2)
    val fc = c("a.b").filterKey(s => if (s=="c"||s=="d") "x" else s)
    fc("x").totalCount should be (4)
    fc.children should have size (2)
    fc.remove("x")
    fc.children should have size (1)
    c("a.b").children should have size (1)
    c("a.b").totalCount should be (2)

    c.remove("*")
    c("a.b.c|d|e|f").add(10)
    c("a.b").totalCount should be (40)
    val tc = c("a.b").transformKey(s => if (s == "1") "c" else s)
    tc.totalCount should be (40)
    tc.remove("1")
    tc.children should have size (3)
    tc.totalCount should be (30)
  }

  test ("path on county") {
    val county = County.create()
    county("a.*.c").path should be (CounterKey("a.*.c"))
    county("a.b.*").path should be (CounterKey("a.b.*"))
    county("a.b", "a.c").path should be (CounterKey("a.b|c"))
    county("a.b.c", "a.e.c").path should be (CounterKey("a.b|e.c"))
    county("a.b.c", "a.e.f").path should be (CounterKey("a.b|e.c|f"))

    county("x.h|l|k.z").increment()
    county("x.k|h.z").path should be (CounterKey("x.h|k.z"))
  }

  test ("county reset") {
    val county = County.create()
    county("x.y.h|l|k.z").increment()
    county("x.y.h|l|k.z").totalCount should be (3)
    county("x.y").totalCount should be (3)
    county("x.y").reset()
    county("x").totalCount should be (0)
    county("x.y.h|l|k.z").totalCount should be (0)
    county("x.y").increment()
    county("x.y").totalCount should be (3)
  }
}
