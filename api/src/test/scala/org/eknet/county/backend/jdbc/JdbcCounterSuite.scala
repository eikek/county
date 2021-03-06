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

package org.eknet.county.backend.jdbc

import org.eknet.county.{Granularity, AbstractCounterSuite}
import java.util.UUID
import java.nio.file.Path
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfter}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.03.13 21:31
 */
class JdbcCounterSuite extends AbstractCounterSuite with DerbyFixture with BeforeAndAfterAll {

  private val dbname: Path = getUniqueDatabasename
  private val ds = createDataSource(dbname)


  override def afterAll() {
    removeDir(dbname)
  }

  def createCounter(gran: Granularity) = {
    val pool = new JdbcCounterPool(gran, ds)
    pool.getOrCreate(UUID.randomUUID().toString)
  }

}
