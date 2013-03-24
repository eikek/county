package org.eknet.county

import collection.mutable.ListBuffer
import annotation.tailrec

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 18:07
 */
trait County extends Counter {

  def path: CounterKey

  def apply(name: CounterKey): County

  def filterKey(fun: String => String): County

  def transformKey(fun: String => String): County

  def children: Iterable[String]
}

object County {

  def create() = new DefaultCounty

}