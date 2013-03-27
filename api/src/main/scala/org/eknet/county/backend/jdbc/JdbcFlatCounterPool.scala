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

import org.eknet.county.{CounterPool, Granularity}
import javax.sql.DataSource
import org.eknet.county
import county.backend.Digest
import reflect.BeanProperty

/**
 * This pool creates counters that are persisted in a database using JDBC. The pool
 * is called *flat*, because it creates one table per counter. This is in contrast
 * to a relational data model. If a counter is newly created, a table is created.
 * If a counter is remove, the corresponding table is dropped.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 19:20
 */
class JdbcFlatCounterPool(val granularity: Granularity, dataSource: DataSource) extends CounterPool {

  @BeanProperty var tableNamePrefix = "county_"
  @BeanProperty var ddl = SchemaDDL(
    Seq("""
      |CREATE TABLE %s (
      |  TIMEKEY NUMERIC(20,0) NOT NULL PRIMARY KEY,
      |  COUNTERVALUE NUMERIC(20,0) NOT NULL
      |)
    """.stripMargin)
  )

  implicit private val ds = dataSource

  def getOrCreate(name: String) = find(name) getOrElse createCounter(name)

  def find(name: String) = {
    val table = createTableName(name)
    if (ddl.schemaExists(table)) {
      Some(createCounter(name))
    } else {
      None
    }
  }

  def remove(name: String) = {
    val table = createTableName(name)
    if (ddl.schemaExists(table)) {
      ddl.dropTable(table)
      true
    } else {
      false
    }
  }

  protected def createCounter(name: String) = {
    val table = createTableName(name)
    val c = new JdbcFlatCounter(granularity, table, dataSource)
    c.ddl = this.ddl
    c
  }

  protected def createTableName(name: String) = tableNamePrefix + Digest.digest(name)
}
