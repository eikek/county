package org.eknet.county

/**
  * @author Eike Kettner eike.kettner@gmail.com
  * @since 22.03.13 19:02
  */
trait CounterPool {

   def getOrCreate(name: String): Counter

   def find(name: String): Option[Counter]

   def remove(name: String): Option[Counter]
 }
