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

package org.eknet.countyj;

import org.eknet.county.Granularity$;
import scala.collection.JavaConversions$;

/**
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 19:28
 */
public enum Granularity {

  millis,
  second,
  minute,
  hour,
  day,
  month,
  year;

  public org.eknet.county.Granularity asScala() {
    for (org.eknet.county.Granularity g : JavaConversions$.MODULE$.asJavaCollection(Granularity$.MODULE$.values())) {
      if (g.name().equals(this.name())) {
        return g;
      }
    }
    throw new Error("unreachable code");
  }
}
