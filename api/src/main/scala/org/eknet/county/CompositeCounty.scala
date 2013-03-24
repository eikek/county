package org.eknet.county

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 18:10
 */
trait CompositeCounty extends County with ProxyCounter {

  def counties: Iterable[County]

  def self = new BasicCompositeCounter(counties)

  def apply(name: CounterKey) = {
    val next = counties.map(c => c(name))
    new BasicCompositeCounty(path / name, next)
  }

  def filterKey(fun: (String) => String) = null

  def transformKey(fun: (String) => String) = null

  def children = null
}

class BasicCompositeCounty(val path: CounterKey, val counties: Iterable[County]) extends CompositeCounty
