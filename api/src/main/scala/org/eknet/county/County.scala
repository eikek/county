package org.eknet.county

import collection.mutable.ListBuffer
import annotation.tailrec

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 23.03.13 18:07
 */
trait County extends Counter {

  /**
   * The path to this county.
   *
   * @return
   */
  def path: CounterKey

  /**
   * Selects a county by applying the given name(s) to this
   * county's path.
   *
   * @param names
   * @return
   */
  def apply(names: CounterKey*): County

  /**
   * Removes nodes selected by the given keys.
   *
   * @param names
   */
  def remove(names: CounterKey*)

  /**
   * Returns a new county that will map the children of this
   * county using the given function. For example, there is
   * the following counter structure:
   * {{{
   *   ip.81-100-100-1  -> 21
   *   ip.84-121-32-3   -> 2
   *   ip.121-42-1-42   -> 14
   * }}}
   *
   * And a function that maps an IP address to its location:
   * {{{
   *   val location: String=>String = (ip: String) => ...
   * }}}
   *
   * Then you can create a new county object that returns the
   * count of each *location*
   * {{{
   *   val c = county("ip").filterKey(location)
   *   c("Germany").totalCount
   * }}}
   *
   * If the function maps to no children, an empty county
   * is returned that cannot be modified (and returns 0).
   *
   * @param fun
   * @return
   */
  def filterKey(fun: String => String): County

  /**
   * Creates a new county by applying the given function to the
   * argument before searching for children.
   *
   * This transforms each input in `apply` according to the given
   * function regarding the next child nodes.
   *
   * For example, this could be used to do some normalizing:
   * {{{
   *   val ips = county("ips").transformKey(s => s.toLowercase)
   *   ips("LOGIN_OK").increment() // is now ips("login_ok").increment()
   * }}}
   *
   * @param fun
   * @return
   */
  def transformKey(fun: String => String): County

  /**
   * Returns the list of child counties of this.
   *
   * @return
   */
  def children: Iterable[String]
}

object County {

  def create() = new DefaultCounty

  def newEmptyCounty(key: CounterKey*) = {
    if (key.size == 1) {
      new EmptyCounty(key(0))
    } else {
      new BasicCompositeCounty(key.map(k => new EmptyCounty(k)))
    }
  }

  class EmptyCounty(val path: CounterKey) extends ProxyCounter with County {
    val self = new BasicCompositeCounter(List())
    def apply(names: CounterKey*) = {
      new BasicCompositeCounty(names.map(n => new EmptyCounty(n)))
    }
    def remove(names: CounterKey*) {}
    def filterKey(fun: (String) => String) = this
    def transformKey(fun: (String) => String) = this
    def children = List()
  }

}