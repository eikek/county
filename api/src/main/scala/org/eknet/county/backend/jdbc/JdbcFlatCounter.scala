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
import reflect.BeanProperty

/**
 * This counter stores all its data in one table. The table has two columns:
 *
 * * timestamp key
 * * counter value
 *
 * The last access and reset time is saved using special timestamp keys `Long.MinValue`
 * and `Long.MinValue + 1`.
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 19:24
 * 
 */
class JdbcFlatCounter(val granularity: Granularity, table: String, dataSource: DataSource) extends CounterBase {

  implicit private val ds = dataSource

  @BeanProperty var ddl = SchemaDDL(
    Seq("""
          |CREATE TABLE %s (
          |  TIMEKEY NUMERIC(20,0) NOT NULL PRIMARY KEY,
          |  COUNTERVALUE NUMERIC(20,0) NOT NULL
          |)
        """.stripMargin
    )
  )

  @BeanProperty var specialTimeKeyLastAccess = Long.MinValue
  @BeanProperty var specialTimeKeyResetTime = Long.MinValue +1

  @BeanProperty
  var totalCountSQL = "SELECT SUM(COUNTERVALUE) FROM %s WHERE TIMEKEY > ?".format(table)

  @BeanProperty
  var countInSQL = "SELECT SUM(COUNTERVALUE) FROM %s WHERE TIMEKEY >= ? AND TIMEKEY <= ?".format(table)

  @BeanProperty
  var keysSQL = "SELECT TIMEKEY FROM %s WHERE TIMEKEY > ? ORDER BY TIMEKEY ASC".format(table)

  @BeanProperty
  var selectEntrySQL = "SELECT TIMEKEY, COUNTERVALUE FROM %s WHERE TIMEKEY = ?".format(table)

  @BeanProperty
  var resetSQL = "DELETE FROM %s WHERE TIMEKEY > ?".format(table)

  @BeanProperty
  var updateEntrySQL = "UPDATE %s SET COUNTERVALUE = COUNTERVALUE + ? WHERE TIMEKEY = ?".format(table)

  @BeanProperty
  var insertEntrySQL = "INSERT INTO %s (TIMEKEY, COUNTERVALUE) VALUES (?, ?)".format(table)

  @BeanProperty
  var setEntrySQL = "UPDATE %s SET COUNTERVALUE = ? WHERE TIMEKEY = ?".format(table)

  createSchema()

  private def boundaryKey = math.max(specialTimeKeyLastAccess, specialTimeKeyResetTime)

  private def createSchema() {
    if (!ddl.schemaExists(table)) {
      ddl.copy(createTable = ddl.createTable.map(_.format(table))).createSchema()
      withTx { conn =>
        val insert = conn.prepareStatement(insertEntrySQL)
        insert.setLong(1, specialTimeKeyLastAccess)
        insert.setLong(2, 0L)
        insert.executeUpdate()
        insert.setLong(1, specialTimeKeyResetTime)
        insert.setLong(2, System.currentTimeMillis())
        insert.executeUpdate()
      }
    }
  }

  def add(when: TimeKey, value: Long) {
    if (value != 0) {
      val timekey = granularity.keyFor(when.timestamp).timestamp
      withTx { conn =>
        val query = conn.prepareStatement(selectEntrySQL)
        query.setLong(1, timekey)
        val rs = query.executeQuery()
        if (rs.next()) {
          val update = conn.prepareStatement(updateEntrySQL)
          update.setLong(1, value)
          update.setLong(2, timekey)
          update.executeUpdate()
        } else {
          val insert = conn.prepareStatement(insertEntrySQL)
          insert.setLong(1, timekey)
          insert.setLong(2, value)
          insert.executeUpdate()
        }
        val update = conn.prepareStatement(setEntrySQL)
        update.setLong(1, System.currentTimeMillis())
        update.setLong(2, specialTimeKeyLastAccess)
        update.executeUpdate()
      }
    }
  }

  def totalCount = {
    withConnection { conn =>
      val select = conn.prepareStatement(totalCountSQL)
      select.setLong(1, boundaryKey)
      val rs = select.executeQuery()
      if (rs.next()) rs.getLong(1) else 0L
    }
  }

  def countIn(range: (TimeKey, TimeKey)) = {
    withConnection { conn =>
      val select = conn.prepareStatement(countInSQL)
      select.setLong(1, range._1.timestamp)
      select.setLong(2, range._2.timestamp)
      val rs = select.executeQuery()
      if (rs.next()) rs.getLong(1) else 0L
    }
  }

  def reset() {
    withTx { conn =>
      val delete = conn.prepareStatement(resetSQL)
      delete.setLong(1, boundaryKey)
      delete.executeUpdate()
      val update = conn.prepareStatement(setEntrySQL)
      update.setLong(1, System.currentTimeMillis())
      update.setLong(2, specialTimeKeyResetTime)
      update.executeUpdate()
    }
  }

  def resetTime = withTx { conn =>
    val select = conn.prepareStatement(selectEntrySQL)
    select.setLong(1, specialTimeKeyResetTime)
    val rs = select.executeQuery()
    if (rs.next()) {
      rs.getLong(2)
    } else {
      0L
    }
  }

  def lastAccess = withTx { conn =>
    val select = conn.prepareStatement(selectEntrySQL)
    select.setLong(1, specialTimeKeyLastAccess)
    val rs = select.executeQuery()
    if (rs.next()) {
      rs.getLong(2)
    } else {
      0L
    }
  }

  def keys = {
    withConnection { conn =>
      val select = conn.prepareStatement(keysSQL)
      select.setLong(1, boundaryKey)
      val rs = select.executeQuery()
      val buffer = ListBuffer[TimeKey]()
      while (rs.next()) {
        buffer += TimeKey(rs.getLong(1))
      }
      buffer.toList
    }
  }
}
