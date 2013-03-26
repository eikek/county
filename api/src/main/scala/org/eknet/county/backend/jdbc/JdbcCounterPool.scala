package org.eknet.county.backend.jdbc

import org.eknet.county.{Granularity, CounterPool}
import javax.sql.DataSource
import java.util.concurrent.atomic.AtomicBoolean
import org.eknet.county.backend.Digest
import reflect.BeanProperty

/**
 * Stores counters using a simple relational schema of two connected tables. The first
 * stores counter meta data, the second all key-value pairs.
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 26.03.13 14:56
 * 
 */
class JdbcCounterPool(val granularity: Granularity, dataSource: DataSource) extends CounterPool {

  implicit private val ds = dataSource
  private val metadataTableName = "COUNTY_COUNTER_META"
  private val counterDataTableName = "COUNTY_COUNTER_DATA"

  @BeanProperty var ddl = SchemaDDL(
    Seq(
      """
        |CREATE TABLE %s (
        |  COUNTERID CHAR(32) NOT NULL PRIMARY KEY,
        |  RESETTIME NUMERIC(20,0),
        |  LASTUPDATE NUMERIC(20,0) NOT NULL
        |)
      """.stripMargin.format(metadataTableName),
      """
        |CREATE TABLE %s (
        |  FKCOUNTER CHAR(32) NOT NULL,
        |  TIMEKEY NUMERIC(20,0) NOT NULL,
        |  COUNTERVALUE NUMERIC(20, 0) NOT NULL,
        |  PRIMARY KEY (fkcounter, timekey),
        |  FOREIGN KEY (fkcounter) REFERENCES %s (counterId) ON DELETE CASCADE
        |)
      """.stripMargin.format(counterDataTableName, metadataTableName)
    )
  )

  @BeanProperty var insertCounterSQL = "INSERT INTO %s (COUNTERID, LASTUPDATE, RESETTIME) VALUES (?, ?, ?)".format(metadataTableName)
  @BeanProperty var findCounterSQL = "SELECT COUNTERID FROM %s WHERE COUNTERID = ?".format(metadataTableName)
  @BeanProperty var removeCounterSQL = "DELETE FROM %s WHERE COUNTERID = ?".format(metadataTableName)

  private val schemaCreated = new AtomicBoolean(false)

  protected def createSchema() {
    if (schemaCreated.compareAndSet(false, true)) {
      if (!ddl.schemaExists(metadataTableName)) {
        ddl.createSchema()

      }
    }
  }
  def getOrCreate(name: String) = find(name) getOrElse (createCounter(name))

  def find(name: String) = {
    createSchema()
    val counterId = createCountId(name)
    withConnection { conn =>
      val select = conn.prepareStatement(findCounterSQL)
      select.setString(1, counterId)
      val rs = select.executeQuery()
      if (rs.next()) {
        Some(newCounter(counterId))
      } else {
        None
      }
    }
  }

  def remove(name: String) = {
    createSchema()
    val id = createCountId(name)
    withTx { conn =>
      val delete = conn.prepareStatement(removeCounterSQL)
      delete.setString(1, id)
      val r = delete.executeUpdate()
      r == 1
    }
  }

  protected def createCounter(name: String) = {
    val counterId = createCountId(name)
    withTx { conn =>
      val insert = conn.prepareStatement(insertCounterSQL)
      insert.setString(1, counterId)
      insert.setLong(2, 0L)
      insert.setLong(3, System.currentTimeMillis())
      insert.executeUpdate()
    }
    newCounter(counterId)
  }

  private def newCounter(id: String) = new JdbcCounter(granularity, metadataTableName, counterDataTableName, id, ds)

  protected def createCountId(name: String) = Digest.digest(name)
}
