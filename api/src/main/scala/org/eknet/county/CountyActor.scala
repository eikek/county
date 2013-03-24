package org.eknet.county

import actors.DaemonActor
import org.eknet.county.CountyActor.{CountRequest, Stop}

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 19:24
 */
class CountyActor(county: County) extends DaemonActor {

  def act() {
    loop {
      react {
        case Stop => exit()
        case CountRequest(path, when, value) => {
          county(path).add(TimeKey(when), value)
        }
      }
    }
  }

}

object CountyActor {

  case class CountRequest(path: CounterKey, when: Long, value: Long)
  case object Stop
}
