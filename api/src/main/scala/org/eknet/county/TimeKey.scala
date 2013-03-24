package org.eknet.county

import java.util.{GregorianCalendar, Locale, TimeZone, Calendar}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 22.03.13 19:55
 */
final case class TimeKey(year: Int, month: Option[Int], day: Option[Int], hour: Option[Int], minute: Option[Int], second: Option[Int], millis: Option[Int]) extends Ordered[TimeKey] {

  def bySeconds = copy(millis = None)
  def byMinutes = bySeconds.copy(second = None)
  def byHour = byMinutes.copy(minute = None)
  def byDay = byHour.copy(hour = None)
  def byMonth = byDay.copy(day = None)
  def byYear = byMonth.copy(month = None)

  lazy val interval = (TimeKey(timestamp), TimeKey(boundaryTimestamp))

  lazy val timestamp = {
    val cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT)
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month.getOrElse(1) -1)
    cal.set(Calendar.DAY_OF_MONTH, day.getOrElse(1))
    cal.set(Calendar.HOUR_OF_DAY, hour.getOrElse(0))
    cal.set(Calendar.MINUTE, minute.getOrElse(0))
    cal.set(Calendar.SECOND, second.getOrElse(0))
    cal.set(Calendar.MILLISECOND, millis.getOrElse(0))
    cal.getTimeInMillis
  }

  private lazy val boundaryTimestamp = {
    val cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT)
    cal.set(Calendar.YEAR, year)
    cal.set(Calendar.MONTH, month.getOrElse(12) -1)
    cal.set(Calendar.DAY_OF_MONTH, day.getOrElse(31))
    cal.set(Calendar.HOUR_OF_DAY, hour.getOrElse(23))
    cal.set(Calendar.MINUTE, minute.getOrElse(59))
    cal.set(Calendar.SECOND, second.getOrElse(59))
    cal.set(Calendar.MILLISECOND, millis.getOrElse(999))
    cal.getTimeInMillis
  }

  def includes(time: TimeKey): Boolean = {
    time >= interval._1 && time <= interval._2
  }

  def includes(time: Long): Boolean = includes(TimeKey(time))

  def + (other: TimeKey) = TimeKey(timestamp + other.timestamp)
  def + (millis: Long) = TimeKey(timestamp + millis)

  def compare(that: TimeKey) = this.timestamp.compare(that.timestamp)
}

object TimeKey {

  def now = TimeKey(System.currentTimeMillis())

  def apply(timestamp: Long): TimeKey = {
    val cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT)
    cal.setTimeInMillis(timestamp)
    cal.setTimeZone(TimeZone.getTimeZone("UTC"))
    val yr = cal.get(Calendar.YEAR)
    val mon = cal.get(Calendar.MONTH) +1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val min = cal.get(Calendar.MINUTE)
    val sec = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)
    TimeKey(yr, Some(mon), Some(day), Some(h), Some(min), Some(sec), Some(millis))
  }

}
