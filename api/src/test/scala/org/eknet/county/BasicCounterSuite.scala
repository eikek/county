package org.eknet.county

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.io._
import java.nio.file.Files

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 00:06
 */
class BasicCounterSuite extends FunSuite with ShouldMatchers {

  test ("simple counting") {
    val c = new BasicCounter(Granularity.Second)
    c.increment()
    val key = TimeKey.now
    c.increment()
    c.add(4)
    c.increment()

    c.totalCount should be (7)
    c.countAt(key) should be (0)
    c.countAt(key.byDay) should be (7)
  }

  test ("more counters") {
    val c = new BasicCounter(Granularity.Millis)
    val key = TimeKey.now
    c.add(key, 1)
    c.add(key + 100, 1)
    c.add(key + 200, 2)

    c.totalCount should be (4)
    c.countAt(key.bySeconds) should be (4)
    c.countAt(key) should be (1)
    c.countAt(key.copy(millis = key.millis.map(_ - 50))) should be (0)
    c.countAt(key.copy(millis = key.millis.map(_ + 50))) should be (0)
    c.countAt(key.copy(millis = key.millis.map(_ + 100))) should be (1)
    c.countAt(key.copy(millis = key.millis.map(_ + 200))) should be (2)
  }

  test ("serialized / deserialize") {
    val c = new BasicCounter(Granularity.Millis)
    val key = TimeKey.now
    c.add(key, 1)
    c.add(key + 100, 1)
    c.add(key + 200, 2)

    val bout = new ByteArrayOutputStream()
    val oout = new ObjectOutputStream(bout)
    oout.writeObject(c)

    val oin = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray))
    val nc = oin.readObject().asInstanceOf[BasicCounter]
    nc.granularity should be (c.granularity)
    nc.totalCount should be (c.totalCount)
    nc.keys should be (c.keys)

    //write to file
    val fout = Files.newOutputStream(Files.createTempFile("test", ".obj"))
    val oout2 = new ObjectOutputStream(fout)
    oout2.writeObject(c)
    fout.close()
  }

  test ("read version 20130322") {
    val in = getClass.getResource("/basiccounter-20130322.obj")
    val oin = new ObjectInputStream(in.openStream())
    val r = oin.readObject().asInstanceOf[BasicCounter]

    r.granularity should be (Granularity.Millis)
    r.keys should have size (3)
    r.keys.toList(0).timestamp should be (1364044220575L)
    r.keys.toList(1).timestamp should be (1364044220675L)
    r.keys.toList(2).timestamp should be (1364044220775L)
    r.totalCount should be (4)
  }
}
