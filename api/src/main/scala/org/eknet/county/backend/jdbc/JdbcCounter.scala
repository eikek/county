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
import javax.sql.DataSource
import collection.mutable.ListBuffer

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.03.13 18:27
 */
class JdbcCounter(val granularity: Granularity, val metadataTable: String, val counterdataTable: String, val id: String, dataSource: DataSource) extends CounterBase {

  var totalCountSql = "SELECT SUM(COUNTERVALUE) FROM %s WHERE FKCOUNTER = ?".format(counterdataTable)
  var countInSql = "SELECT SUM(COUNTERVALUE) FROM %s WHERE FKCOUNTER = ? AND TIMEKEY >= ? AND TIMEKEY <= ?".format(counterdataTable)
  var resetSql = "DELETE FROM %s WHERE FKCOUNTER = ?".format(counterdataTable)

  var selectCountSql = "SELECT TIMEKEY, COUNTERVALUE FROM %s WHERE FKCOUNTER = ? AND TIMEKEY = ?".format(counterdataTable)
  var insertCountSql = "INSERT INTO %s (FKCOUNTER, TIMEKEY, COUNTERVALUE) VALUES (?, ?, ?)".format(counterdataTable)
  var updateCountSql = "UPDATE %s SET COUNTERVALUE = COUNTERVALUE + ? WHERE FKCOUNTER = ? AND TIMEKEY = ?".format(counterdataTable)

  var selectKeysSql = "SELECT TIMEKEY FROM %s WHERE FKCOUNTER = ? ORDER BY TIMEKEY ASC".format(counterdataTable)

  var setResetTimeSql = "UPDATE %s SET RESETTIME = ? WHERE COUNTERID = ?".format(metadataTable)
  var setLastAccessTimeSql = "UPDATE %s SET LASTUPDATE = ? WHERE COUNTERID = ?".format(metadataTable)
  var selectResetTimeSql = "SELECT RESETTIME FROM %s WHERE COUNTERID = ?".format(metadataTable)
  var selectLastAccessTimeSql = "SELECT LASTUPDATE FROM %s WHERE COUNTERID = ?".format(metadataTable)

  implicit private val ds = dataSource

  def add(when: TimeKey, value: Long) {
    if (value != 0) {
      val key = granularity.keyFor(when.timestamp).timestamp
      synchronized {

        withTx { conn =>
          val select = conn.prepareStatement(selectCountSql)
          select.setString(1, id)
          select.setLong(2, key)
          val rs = select.executeQuery()
          if (rs.next()) {
            val update = conn.prepareStatement(updateCountSql)
            update.setLong(1, value)
            update.setString(2, id)
            update.setLong(3, key)
            update.executeUpdate()
          } else {
            val insert = conn.prepareStatement(insertCountSql)
            insert.setString(1, id)
            insert.setLong(2, key)
            insert.setLong(3, value)
            insert.executeUpdate()
          }
          val update = conn.prepareStatement(setLastAccessTimeSql)
          update.setLong(1, System.currentTimeMillis())
          update.setString(2, id)
          update.executeUpdate()
        }
      }
    }
  }

  def totalCount = withTx { conn =>
    val select = conn.prepareStatement(totalCountSql)
    select.setString(1, id)
    val rs = select.executeQuery()
    if (rs.next()) rs.getLong(1) else 0L
  }

  def countIn(range: (TimeKey, TimeKey)) = withTx { conn =>
    val select = conn.prepareStatement(countInSql)
    select.setString(1, id)
    select.setLong(2, range._1.timestamp)
    select.setLong(3, range._2.timestamp)
    val rs = select.executeQuery()
    if (rs.next()) rs.getLong(1) else 0L
  }

  def reset() {
    withTx { conn =>
      val delete = conn.prepareStatement(resetSql)
      delete.setString(1, id)
      delete.executeUpdate()
      val update = conn.prepareStatement(setResetTimeSql)
      update.setLong(1, System.currentTimeMillis())
      update.setString(2, id)
      update.executeUpdate()
    }
  }

  def resetTime = withTx { conn =>
    val select = conn.prepareStatement(selectResetTimeSql)
    select.setString(1, id)
    val rs = select.executeQuery()
    if (rs.next()) rs.getLong(1) else 0L
  }

  def lastAccess = withTx { conn =>
    val select = conn.prepareStatement(selectLastAccessTimeSql)
    select.setString(1, id)
    val rs = select.executeQuery()
    if (rs.next()) rs.getLong(1) else 0L
  }

  def keys = withTx { conn =>
    val select = conn.prepareStatement(selectKeysSql)
    select.setString(1, id)
    val buffer = ListBuffer[TimeKey]()
    val rs = select.executeQuery()
    while (rs.next()) {
      buffer += TimeKey(rs.getLong(1))
    }
    buffer.toList
  }
}
