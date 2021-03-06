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

import org.eknet.county.CounterKey;
import org.eknet.county.CounterKey$;
import org.eknet.county.CounterPool;
import org.eknet.county.DefaultCounty;
import scala.Tuple2;

/**
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 19:12
 */
public class JDefaultCounty extends DefaultCounty {

  public JDefaultCounty(char segmentDelimiter) {
    super(segmentDelimiter);
  }

  public JDefaultCounty() {
    super(CounterKey$.MODULE$.defaultSegmentDelimiter());
  }

  public void addFirst(String pattern, CounterPool pool) {
    setCounterFactories(this.getCounterFactories().$colon$colon(new Tuple2<String, CounterPool>(pattern, pool)));
  }

  public JCounty get(String... path) {
    return Counties.next(this, path);
  }

  public JCounty get(CounterKey... paths) {
    return Counties.next(this, paths);
  }
}
