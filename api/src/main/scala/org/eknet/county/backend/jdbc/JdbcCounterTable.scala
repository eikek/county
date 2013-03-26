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

import org.eknet.county.{TimeKey, CounterBase, Granularity}
import java.util.concurrent.atomic.AtomicBoolean
import collection.mutable.ListBuffer
import javax.sql.DataSource

/**
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 19:24
 * 
 */
class JdbcCounterTable(val granularity: Granularity, table: String, dataSource: DataSource, ddl: SchemaDDL) extends CounterBase {

  implicit private val ds = dataSource

  private val schemaCreated = new AtomicBoolean(false)

  var totalCountSQL = "SELECT SUM(COUNTERVALUE) FROM %s"
  var countInSQL = "SELECT SUM(COUNTERVALUE) FROM %s WHERE TIMEKEY >= ? AND TIMEKEY <= ?"
  var resetSQL = "DELETE FROM %s"
  var keysSQL = "SELECT TIMEKEY FROM %s ORDER BY TIMEKEY ASC"
  var selectEntrySQL = "SELECT TIMEKEY, COUNTERVALUE FROM %s WHERE TIMEKEY = ?"
  var updateEntrySQL = "UPDATE %s SET COUNTERVALUE = COUNTERVALUE + ? WHERE TIMEKEY = ?"
  var insertEntrySQL = "INSERT INTO %s (TIMEKEY, COUNTERVALUE) VALUES (?, ?)"

  private def createSchema() {
    if (schemaCreated.compareAndSet(false, true)) {
      if (!ddl.schemaExists(table)) {
        ddl.createSchema(table)
      }
    }
  }

  def add(when: TimeKey, value: Long) {
    createSchema()
    val timekey = granularity.keyFor(when.timestamp).timestamp
    withTx { conn =>
      val query = conn.prepareStatement(selectEntrySQL.format(table))
      query.setLong(1, timekey)
      val rs = query.executeQuery()
      if (rs.next()) {
        val update = conn.prepareStatement(updateEntrySQL.format(table))
        update.setLong(1, value)
        update.setLong(2, timekey)
        update.executeUpdate()
      } else {
        val insert = conn.prepareStatement(insertEntrySQL.format(table))
        insert.setLong(1, timekey)
        insert.setLong(2, value)
        insert.executeUpdate()
      }
    }
  }

  def totalCount = {
    createSchema()
    withConnection { conn =>
      val rs = conn.createStatement().executeQuery(totalCountSQL.format(table))
      if (rs.next()) rs.getLong(1) else 0L
    }
  }

  def countIn(range: (TimeKey, TimeKey)) = {
    createSchema()
    val sql = countInSQL.format(table, range._1.timestamp, range._2.timestamp)
    withConnection { conn =>
      val rs = conn.createStatement().executeQuery(sql)
      if (rs.next()) rs.getLong(1) else 0L
    }
  }

  def reset() {
    createSchema()
    withTx { conn =>
      conn.createStatement().executeUpdate(resetSQL.format(table))
    }
  }

  def resetTime = 0L

  def lastAccess = 0L

  def keys = {
    createSchema()
    withConnection { conn =>
      val rs = conn.prepareStatement(keysSQL.format(table)).executeQuery()
      val buffer = ListBuffer[TimeKey]()
      while (rs.next()) {
        buffer += TimeKey(rs.getLong(1))
      }
      buffer.toList
    }
  }
}
