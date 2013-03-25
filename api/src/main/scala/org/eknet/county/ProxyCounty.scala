package org.eknet.county

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 12:28
 */
trait ProxyCounty extends County with ProxyCounter {

  def self: County

  def path = self.path
  def apply(name: CounterKey*) = self.apply(name: _*)
  def filterKey(fun: (String) => String) = self.filterKey(fun)
  def transformKey(fun: (String) => String) = self.transformKey(fun)
  def children = self.children
}
