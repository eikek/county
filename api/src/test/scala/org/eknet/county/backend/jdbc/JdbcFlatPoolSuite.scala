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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.apache.derby.jdbc.EmbeddedDataSource
import java.nio.file.{FileVisitResult, Path, SimpleFileVisitor, Files}
import javax.sql.DataSource
import org.eknet.county.Granularity
import java.nio.file.attribute.BasicFileAttributes
import java.io.IOException

/**
 *
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.03.13 19:38
 * 
 */
class JdbcFlatPoolSuite extends FunSuite with ShouldMatchers {

  def removeDir(path: Path) {
    if (Files.exists(path)) {
      Files.walkFileTree(path, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException) = {
          if (exc == null) {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          } else {
            throw exc
          }
        }
      })
    }
  }
  def withDataSource[A](body: DataSource => A): A = {
    val tempdir = Files.createTempDirectory("testderby")
    Files.delete(tempdir)
    val dbname = tempdir.resolveSibling("testdb-derby")
    removeDir(dbname)

    val ds = new EmbeddedDataSource
    ds.setDatabaseName(dbname.toString)
    ds.setCreateDatabase("create")

    body(ds)

  }

  test ("crud tests") {
    withDataSource { ds =>
      val pool = new JdbcFlatCounterPool(Granularity.Second, ds)

      pool.find("not-there") should be (None)

      val c1 = pool.getOrCreate("hello-counter")
      c1.increment()
      c1.totalCount should be (1)

      val c12 = pool.getOrCreate("hello-counter")
      c12.totalCount should be (1)

      val c13 = pool.find("hello-counter")
      c13 should not be (None)

      c13.get.totalCount should be (1)

      pool.remove("sdfsdf") should be (false)
      val rc = pool.remove("hello-counter")
      rc should be (true)
      pool.find("hello-counter") should be (None)
    }
  }
}
