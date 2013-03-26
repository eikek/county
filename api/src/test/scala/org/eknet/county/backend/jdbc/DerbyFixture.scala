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

import javax.sql.DataSource
import org.apache.derby.jdbc.EmbeddedDataSource
import org.eknet.county.FileUtils
import java.nio.file.{Path, Files}
import java.util.UUID

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 26.03.13 19:58
 */
trait DerbyFixture extends FileUtils {

  def getUniqueDatabasename = {
    val tempdir = Files.createTempDirectory("testderby")
    Files.delete(tempdir)
    tempdir.resolveSibling(UUID.randomUUID().toString)
  }

  def createDataSource(name: Path) = {
    val ds = new EmbeddedDataSource
    ds.setDatabaseName(name.toString)
    ds.setCreateDatabase("create")
    ds
  }

  def withDataSource[A](name: Path)(body: DataSource => A): A = {
    val ds = createDataSource(name)
    body(ds)
  }

}
