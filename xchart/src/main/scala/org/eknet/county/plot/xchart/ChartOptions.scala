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

package org.eknet.county.plot.xchart

import com.xeiam.xchart.StyleManager.ChartType
import com.xeiam.xchart.{Chart, ChartBuilder}
import org.eknet.county.{CounterKey, TimeKey}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 20:10
 */
final case class ChartOptions(width: Int = 600,
                              height: Int = 500,
                              chart: ChartType = ChartType.Bar,
                              title: String = "",
                              xaxis: String = "",
                              yaxis: String = "",
                              seriesName: CounterKey => String = k => k.lastSegment.head,
                              range: Option[(TimeKey, TimeKey)] = None,
                              resolution: Option[TimeKey => TimeKey] = None,
                              compact: Boolean = true,
                              customizer: Chart => Unit = c => ()) {

  def toBuilder = {
    new ChartBuilder().chartType(chart)
      .height(height)
      .width(width)
      .title(title)
      .xAxisTitle(xaxis)
      .yAxisTitle(yaxis)
  }

  def consolidate(key: TimeKey) = resolution match {
    case Some(res) => res(key)
    case _ => key
  }

  def inRange(key: TimeKey) = range match {
    case Some(r) => key >= r._1 && key <= r._2
    case _ => true
  }

  def transform(keys: Iterable[TimeKey]) = {
    val k0 = range map { r => keys.filter(k => k >= r._1 && k <= r._2) } getOrElse(keys)
    resolution map { r => k0.map(r).toList.distinct } getOrElse(keys)
  }
}
