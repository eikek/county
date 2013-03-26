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

package org.eknet.county.backend

import java.sql.Connection
import javax.sql.DataSource

/**
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 25.03.13 18:25
 *
 */
package object jdbc {

  def withConnection[A](body: Connection => A)(implicit ds: DataSource): A = {
    val conn = ds.getConnection
    try {
      body(conn)
    } finally {
      conn.close()
    }
  }

  def withTx[A](body: Connection => A)(implicit ds: DataSource): A = {
    withConnection { conn =>
      val old = conn.getAutoCommit
      conn.setAutoCommit(false)
      try {
        val r = body(conn)
        conn.commit()
        r
      } finally {
        conn.setAutoCommit(old)
      }
    }
  }
}
