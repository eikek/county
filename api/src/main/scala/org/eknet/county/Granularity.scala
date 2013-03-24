package org.eknet.county

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 12:30
 */
sealed trait Granularity extends Serializable {

  def name: String

  def keyFor(millis: Long): TimeKey

}

object Granularity {

  val values = List(Millis, Second, Minute, Hour, Day, Month, Year)

  @SerialVersionUID(20130322L)
  object Millis extends Granularity {
    val name = "millis"
    def keyFor(millis: Long) = TimeKey(millis)
  }

  @SerialVersionUID(20130322L)
  object Second extends Granularity {
    val name = "second"
    def keyFor(millis: Long) = TimeKey(millis).bySeconds
  }

  @SerialVersionUID(20130322L)
  object Minute extends Granularity {
    val name = "minute"
    def keyFor(millis: Long) = TimeKey(millis).byMinutes
  }

  @SerialVersionUID(20130322L)
  object Hour extends Granularity {
    val name = "hour"
    def keyFor(millis: Long) = TimeKey(millis).byHour
  }

  @SerialVersionUID(20130322L)
  object Day extends Granularity {
    val name = "day"
    def keyFor(millis: Long) = TimeKey(millis).byDay
  }

  @SerialVersionUID(20130322L)
  object Month extends Granularity {
    val name = "month"
    def keyFor(millis: Long) = TimeKey(millis).byMonth
  }

  @SerialVersionUID(20130322L)
  object Year extends Granularity {
    val name = "year"
    def keyFor(millis: Long) = TimeKey(millis).byYear
  }
}
