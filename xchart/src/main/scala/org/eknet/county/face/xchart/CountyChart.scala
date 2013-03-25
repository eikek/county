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

import org.eknet.county._
import com.xeiam.xchart.Chart
import com.xeiam.xchart.StyleManager.LegendPosition

/**
 * Creates a basic chart using either a [[org.eknet.county.County]] or
 * [[org.eknet.county.Counter]].
 *
 * When creating a chart for a [[org.eknet.county.Counter]]:
 *
 * * x-axis: the timestamp formatted as date
 * * y-axis: the `countIn(key.interval)` value
 * * `range` option: to include only those keys within the range (inclusive)
 * * `resolution` option: consolidate the keys by mapping timestamps to more
 *    coarse timestamps. For example: map counters of every minute to counters
 *    of a day or week
 * * `compact` option: if the counter is a [[org.eknet.county.CompositeCounter]]
 *   the counter is treated like a single counter if `compact == true`. If set to
 *   `false`, then every internal counter is added to the same chart.
 * * 0 values are removed from the chart (means no measure point)
 *
 * When creating a chart for a [[org.eknet.county.County]]:
 *
 * * x-axis: the names of the child nodes
 * * y-axis: either the total count or the value of `countIn()`
 *   of the child counter
 * * `range` option: used as argument for `countIn()` when providing values for
 *   the y-axis. If not set, `totalCount` is used
 * * `resolution` option: not used here
 * * `compact` option: if this is a [[org.eknet.county.CompositeCounty]], then
 *   it is treated like a single county, if `compact == true`. If set to `false`
 *   every internal [[org.eknet.county.County]] is added to the graph
 * * If the [[org.eknet.county.County]] has no children, it is treated as
 *   a single [[org.eknet.county.Counter]]
 *
 * The options allow to provide a function to further customize the chart. This
 * function is applied as the last step, after the data has been added. Use it
 * to customize the style of the chart. See [[http://xeiam.com/xchart_examplecode.jsp]]
 * for some examples.
 *
 * Usage: Import the `CountyChart._` or just the `CounterChart.apply` function. This
 * function is annotated with `implicit` and wraps your [[org.eknet.county.County]] into
 * a [[org.eknet.county.face.xchart.CountyChart]] which makes the two methods `createChart`
 * and `createCounterChart` available.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 19:42
 */
class CountyChart(county: County) {

  def createChart(options: ChartOptions = ChartOptions()): RichChart = {
    if (county.children.isEmpty) {
      createCounterChart(options)
    } else {
      val chart = options.toBuilder.build()
      county match {
        case comp: CompositeCounty if (!options.compact) => {
          comp.counties.foreach(c => addCountyData(c, chart, options))
        }
        case _ => addCountyData(county, chart, options)
      }
      chart.getStyleManager.setLegendPosition(LegendPosition.OutsideW)
      options.customizer(chart)
      chart
    }
  }

  def createCounterChart(options: ChartOptions = ChartOptions()): RichChart = {
    val chart = options.toBuilder.build()
    county match {
      case comp: CompositeCounty if (!options.compact) => {
        comp.counties.foreach(c => addCounterData(c, chart, options))
      }
      case _ => addCounterData(county, chart, options)
    }
    chart.getStyleManager.setLegendPosition(LegendPosition.OutsideW)
    options.customizer(chart)
    chart
  }

  private[this] def addCountyData(county: County, chart: Chart, options: ChartOptions) = {
    import collection.JavaConverters.asJavaCollectionConverter

    val xData = county.children.asJavaCollection
    val numbers = (options.range match {
      case Some(range) => county.children.map(county(_).countIn(range))
      case _ => county.children.map(county(_).totalCount)
    }).map(_.asInstanceOf[Number]).asJavaCollection

    chart.addCategorySeries(options.seriesName(county.path), xData, numbers)
  }

  private[this] def addCounterData(county: County, chart: Chart, options: ChartOptions) = {
    import collection.JavaConverters.asJavaCollectionConverter

    val keys = options.transform(county.keys)
      .filter(k => county.countIn(k.interval) > 0L)
      .toList.sorted
    val values = keys.map(k => county.countIn(k.interval))

    chart.addDateSeries(options.seriesName(county.path),
      keys.map(m => new java.util.Date(m.timestamp)).asJavaCollection,
      values.map(_.asInstanceOf[Number]).asJavaCollection)
  }
}

object CountyChart {

  implicit def apply(c: County) = new CountyChart(c)

}