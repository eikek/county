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

import java.sql.SQLException
import javax.sql.DataSource

/**
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.03.13 18:39
 * 
 */
case class SchemaDDL(createTable: Seq[String], dropTableSQL: String, testSQL: String) {

  def schemaExists(table: String)(implicit ds: DataSource) = {
    withConnection { conn =>
      try {
        conn.createStatement().executeQuery(testSQL.format(table))
        true
      } catch {
        case e: SQLException => false
      }
    }
  }

  def createSchema(table: String)(implicit ds: DataSource) {
    withTx { conn =>
      createTable.map(_.format(table)) map { sql =>
        conn.createStatement().executeUpdate(sql)
      }
    }
  }

  def dropTable(table: String)(implicit ds: DataSource) {
    withTx { conn =>
      conn.prepareStatement(dropTableSQL.format(table)).executeUpdate()
    }
  }
}
