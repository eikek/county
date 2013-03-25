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

import org.testng.Assert;
import org.testng.annotations.*;

import org.eknet.county.BasicCounterPool;
import org.eknet.county.CounterPool;

/**
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 19:21
 */
public class JCounterTest {

  @Test
  public void testJavaCounterApi() throws Exception {
    JDefaultCounty county = Counties.create();
    CounterPool pool = new BasicCounterPool(Granularity.millis.asScala());
    county.addFirst("a.b.c", pool);

    county.get("a.b.c").increment();
    county.get("a.b.c").increment();
    county.get("a.b.d").increment();
    county.get("a.b.e").increment();

    Assert.assertEquals(county.get("a.b").totalCount(), 4);
    Assert.assertEquals(county.get("a.*.c").totalCount(), 2);

  }
}
