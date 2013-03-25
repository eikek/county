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

package org.eknet.county.face.xchart

import com.xeiam.xchart.{ChartBuilder, SwingWrapper, QuickChart}
import com.xeiam.xchart.StyleManager.{ChartTheme, LegendPosition, ChartType}
import org.eknet.county._
import java.util.{Calendar, TimeZone, GregorianCalendar}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 19:45
 */
object ChartPlay extends App {

  val pageCounters = List(
    "app.firefox.hello-world",
    "app.chrome.hello-world",
    "app.firefox.installation",
    "app.chrome.installation",
    "app.firefox.howto-cook",
    "app.chrome.howto-cook"
  )

  def createRandomPageVisits(county: County) {
    val allVisits = (math.random * 1000).toInt
    val time = TimeKey.now
    for (i <- 0 to allVisits) {
      val page = (math.random * 10).toInt % pageCounters.size
      val count = (math.random * 10).toLong
      val when = time + (i * 24 * 60 * 60 * 1000)
      county(pageCounters(page)).add(when, count)
    }
  }


  val county = County.create()
  county.counterFactories = List("**" -> new BasicCounterPool(Granularity.Millis))
  //  val now = TimeKey.now
  //  county("app.firefox.hello-world").add(now, 5)
  //  county("app.firefox.installation").add(now + 200, 3)
  //  county("app.firefox.howto-cook").add(now + 400, 12)
  //  county("app.firefox.hello-world").add(now+ 600, 1)
  //
  //  county("app.chrome.hello-world").add(now + 400, 8)
  //  county("app.chrome.howto-cook").add(now + 890, 3)
  //  county("app.chrome.installation").add(now + 390, 3)
  createRandomPageVisits(county)

  import CountyChart._


  val byweek: TimeKey => TimeKey = t => {
    val c = new GregorianCalendar()
    c.setTimeZone(TimeZone.getTimeZone("UTC"))
    c.setTimeInMillis(t.timestamp)
    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    TimeKey(c.getTimeInMillis)
  }

  val chartopts = ChartOptions(width = 500, range = Some(TimeKey.now.byMonth.interval), compact = false)
  county("app.firefox", "app.chrome").createChart(chartopts).display()


  county("app.*.hello-world").createCounterChart(
    chartopts.copy(resolution = Some(byweek),
      seriesName = path => path.tail.tail.headSegment,
      compact = true,
      range = Some(TimeKey.now.byMonth.interval))).display()

}
