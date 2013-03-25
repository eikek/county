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
import org.eknet.county.County;
import org.eknet.county.TimeKey;
import scala.Function1;
import scala.Tuple2;
import scala.collection.Iterable;
import scala.collection.JavaConversions$;
import scala.collection.Seq;

/**
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 25.03.13 18:53
 */
public class JCounty implements County {

  private final County self;

  private JCounty(County self) {
    this.self = self;
  }

  public static JCounty from(County c) {
    if (c instanceof JCounty) {
      return (JCounty) c;
    } else {
      return new JCounty(c);
    }
  }

  @Override
  public void add(TimeKey when, long value) {
    self.add(when, value);
  }

  @Override
  public void increment() {
    self.increment();
  }

  @Override
  public void decrement() {
    self.decrement();
  }

  @Override
  public void add(long value) {
    self.add(value);
  }

  @Override
  public long totalCount() {
    return self.totalCount();
  }

  @Override
  public long countIn(Tuple2<TimeKey, TimeKey> range) {
    return self.countIn(range);
  }

  public long countIn(TimeKey start, TimeKey end) {
    return countIn(new Tuple2<TimeKey, TimeKey>(start, end));
  }

  @Override
  public void reset() {
    self.reset();
  }

  @Override
  public long resetTime() {
    return self.resetTime();
  }

  @Override
  public long lastAccess() {
    return self.lastAccess();
  }

  @Override
  public scala.collection.Iterable<TimeKey> keys() {
    return self.keys();
  }

  public java.lang.Iterable<TimeKey> keysIterable() {
    return JavaConversions$.MODULE$.asJavaIterable(self.keys());
  }

  @Override
  public CounterKey path() {
    return self.path();
  }

  @Override
  public JCounty apply(Seq<CounterKey> name) {
    return new JCounty(self.apply(name));
  }

  public JCounty get(String... path) {
    return Counties.next(this, path);
  }

  public JCounty get(CounterKey... paths) {
    return Counties.next(this, paths);
  }

  @Override
  public JCounty filterKey(Function1<String, String> fun) {
    return new JCounty(self.filterKey(fun));
  }

  @Override
  public JCounty transformKey(Function1<String, String> fun) {
    return new JCounty(self.transformKey(fun));
  }

  @Override
  public Iterable<String> children() {
    return self.children();
  }
}
