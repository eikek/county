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
import org.slf4j.LoggerFactory
import java.util.concurrent.{TimeUnit, Executors, CountDownLatch}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.03.13 19:55
 */
abstract class AbstractCounterSuite extends FunSuite with ShouldMatchers {

  def createCounter(gran: Granularity): Counter

  test ("simple counting") {
    val c = createCounter(Granularity.Second)
    c.increment()
    val key = TimeKey.now
    c.increment()
    c.add(4)
    c.increment()

    c.totalCount should be (7)
    c.countIn(key.interval) should be (0)
    c.countIn(key.byDay.interval) should be (7)
  }

  test ("more counters") {
    val c = createCounter(Granularity.Millis)
    val key = TimeKey(1364336410830L)
    c.add(key, 1)
    c.add(key + 100, 1)
    c.add(key + 200, 2)

    c.totalCount should be (4)
    c.countIn(key.byMinutes.interval) should be (4)
    c.countIn(key.interval) should be (1)
    c.countIn(key.copy(millis = key.millis.map(_ - 50)).interval) should be (0)
    c.countIn(key.copy(millis = key.millis.map(_ + 50)).interval) should be (0)
    c.countIn(key.copy(millis = key.millis.map(_ + 100)).interval) should be (1)
    c.countIn(key.copy(millis = key.millis.map(_ + 200)).interval) should be (2)
  }

  test ("dropping counter") {
    val counter = new DroppingCounter(createCounter(Granularity.Millis), 250L)
    counter.increment()
    counter.increment()
    counter.increment()

    counter.totalCount should be (1)
    Thread.sleep(251L)
    counter.increment()
    counter.increment()

    counter.totalCount should be (2)
  }

  test ("reset time and last access time") {
    val counter = createCounter(Granularity.Second)
    val created = TimeKey.now
    counter.resetTime should be (created.timestamp plusOrMinus 500L)
    counter.lastAccess should be (0L)
    Thread.sleep(100)
    counter.increment()
    val access = TimeKey.now
    counter.lastAccess should be (access.timestamp plusOrMinus 500L)
    counter.resetTime should be (created.timestamp plusOrMinus 500L)

    counter.reset()
    val reset = TimeKey.now
    counter.totalCount should be (0)
    counter.resetTime should be (reset.timestamp plusOrMinus 500L)
  }

  test ("mass update test") {
    createCounter(Granularity.Millis).reset()
    stresstest(Granularity.Millis)
    info("----------------------------------------------------------")
    createCounter(Granularity.Second).reset()
    stresstest(Granularity.Second)
    info("----------------------------------------------------------")
    createCounter(Granularity.Minute).reset()
    stresstest(Granularity.Minute)
    info("----------------------------------------------------------")
    createCounter(Granularity.Second).reset()
    stresstest(Granularity.Second)
    info("----------------------------------------------------------")
    createCounter(Granularity.Millis).reset()
    stresstest(Granularity.Millis)
  }

  private def stresstest(gran: Granularity, max: Int = 1000) {
    val start = System.currentTimeMillis()
    info("Starting %d updates for counter with granularity=%s".format(max, gran.name))
    val counter = createCounter(gran)
    info("Created counter in %d ms".format(System.currentTimeMillis() - start))
    var sum = 0L
    for (i <- 1 to max) {
      val starti = System.currentTimeMillis()
      counter.increment()
      if (i % 4 == 0) {
        counter.increment()
        counter.increment()
        counter.increment()
      }
      sum = sum + (System.currentTimeMillis() - starti)
    }
    counter.totalCount should be (max + ((max /4)*3))
    info("Incremented in %d ms. Average time per loop: %d ms".format( (System.currentTimeMillis() - start), (sum / max)))
    val totals = max + ((max / 4) * 3)
    counter.totalCount should be (totals)
    val executionTime = System.currentTimeMillis() - start
    info("Total time: %d ms".format(executionTime))
  }

  private def threadingTest(gran: Granularity) {
    val start = System.currentTimeMillis()
    val counter = createCounter(gran)
    val peng = new CountDownLatch(1)
    val pool = Executors.newFixedThreadPool(3)
    val max = 200
    val counting = new Runnable {
      def run() {
        peng.await(20, TimeUnit.SECONDS)
        for (i <- 1 to max) {
          counter.increment()
          if (i % 4 == 0) {
            counter.add(2)
          }
        }
      }
    }
    val nThreads = 3
    val futures = for (i <- 1 to nThreads) yield pool.submit(counting)
    peng.countDown()
    futures.foreach(_.get(20, TimeUnit.SECONDS))
    val expectedCount = nThreads * (max + (max/4*2))
    counter.totalCount should be (expectedCount)
    info("-- Threading ["+gran.name+"] took "+ (System.currentTimeMillis() - start)+ "ms")
  }

  test ("threading test") {
    //counters should be safe to use concurrently from multiple threads
    threadingTest(Granularity.Millis)
    threadingTest(Granularity.Second)
    threadingTest(Granularity.Minute)
  }
}
