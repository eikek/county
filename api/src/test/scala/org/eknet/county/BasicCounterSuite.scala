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
import java.io._
import java.nio.file.Files

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 00:06
 */
class BasicCounterSuite extends AbstractCounterSuite {


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

  def createCounter(gran: Granularity) = new BasicCounter(gran)
}
