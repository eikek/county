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

import com.xeiam.xchart.{BitmapEncoder, SwingWrapper, Chart}
import java.nio.file.Path

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 20:21
 */
class RichChart(val xchart: Chart) {

  def display() {
    new SwingWrapper(xchart).displayChart()
  }

  def savePng(file: Path) {
    BitmapEncoder.savePNG(xchart, file.toString)
  }
}

object RichChart {

  implicit def apply(xc: Chart) = new RichChart(xc)
  implicit def unapply(rc: RichChart) = rc.xchart

}
