package org.eknet.county

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 24.03.13 18:10
 */
trait CompositeCounty extends County with ProxyCounter {

  def counties: Iterable[County]

  def self = new BasicCompositeCounter(counties)

  def apply(names: CounterKey*) = {
    val next = names.flatMap(n => counties.map(c => c(n)))
    val nextPath = if (names.size == 1) names.head else names.head / "**"
    if (next.size == 1) {
      next(0)
    } else {
      new BasicCompositeCounty(path / nextPath, next)
    }
  }

  def filterKey(fun: (String) => String) = null

  def transformKey(fun: (String) => String) = null

  def children = null
}

class BasicCompositeCounty(val path: CounterKey, val counties: Iterable[County]) extends CompositeCounty
