# County -- library for counting things

County is a library written in Scala that aims to provide flexible and easy to use ways
to count things. Counters are often used in all sorts of applications, ususally to create
a simple kind of statistics, like the number of visits to a web page, number of failed or
successful logins etc. From there it often grows to manage counts by username or ip address
maybe to find ips that initiate brute force attacks.

County provides an API to easily count all kind of things and create some simple statistics
from it. It provides persistent counters as well as in-memory ones and lets you combine
those.


## Building block -- the `Counter`

The simple entity, everything else is build on top, is the `Counter`. A `Counter` is very
simple, it only allows to add numeric values. The counter organizes them using a timestamp
that specifies the point in time the counter was modified. In other words, there exists
a numeric value for one timestamp. that is incremented (or decremented, resp) if the counter
is modified by multiple threads at the same time. You can define a `Granularity` that is
used to normalize the timestamp, such that all counts within one second, minute or month
are accumulated at one value.

The counter itself does not define _what_ it counts, it just manages some numbers. The
counter lets you read its total count as well as counts in a time range. For example, you
might want to know the total number of visits of a web page and the number of visits on a
certain day. To propery answer this, the counter must be configured with a minimum `Granularity`,
of course.

The other part is a registry for counters. This registry finally maps a name to a counter,
which names the event to be counted.

So the main component is a set of named counters. You can retrieve or create counters by
name and use them.


## Structuring

Counters can be organized in a tree using the `County` object. The `County` object creates
counters by handing in a path of names. For example:

    county("a.b.1")
    county("a")("b")("1")
    val b = county("a.b"); b("1")

creates a linked list with nodes `a`, `b` and `1` and returns a counter (all lines are
equivalent). Modifying the counter

    county("a.b.1").increment()

yields in modifying the counter that is attached to node `1`. More counters can be added the
same way:

    county("a.b.2").increment()
    county("a.b.3").increment()
    county("a.c.1").increment()
    ...

Now there is a simple tree

       a
       |`.
       b  c
     .´|`- `.
    1  2  3  1

where the leaf nodes have real counters attached. Inner nodes are counters that delegate to
its children. An inner node's value is the sum of its children. For the example above

    county("a.b").totalCount

would return `3` now.

The `County` object allows to retrieve a list of children that names the next nodes in the tree. For
leaf nodes, the list is empty.

## Counter Sets

Using a glob style path, a counter can be created that consists of other counters in the tree. For example:

    county("a.*.1").increment()

would increment the counters `a.b.1` and `a.c.1`. You can use `*` to match any character multiple times
or `?` to match exactly one character.

## Filtering Keys

It is possible to create a counter that applies a given function to its children:

    val even = county("a.b").filterKeys(s => (s.toInt % 2).toString)
    event("0").increment()

The function is applied to the children of node `b`, which are numbers `1`, `2` and `3`. The function
get the remainder of the division by 2 of the argument. This means, that the counter created by `even("0")`
is collecting all children with even numbers and `even("1")` would collect the other.

Note that the function is applied to existing child nodes and creates a view of them. The line `even("3")`
would collect nothing and calling `increment()` on it has no effect.

## Transforming Keys

It's possible to provide a function that is applied when adding new childs. In contrast to `filterKey` this
is applied to the path argument before the child node is created.

    val c = county("a.b").transformKey(key => key +"s")
    c("word").increment()
    c("letter").increment()

The function adds an extra 's' character to its argument, thus `c("word")` is creating a new child node
of name `words`. Its not just a view of existing children, like `filterKey`.

## Controlling Counter Implementations

A `CounterPool` is a generic interface for managing counters by names. The `BasicCounterPool` manages
a in-memory map and there is also a [blueprints](http://blueprints.tinkerpop.com/) backend that stores
the counter data in a graph database.

Those pools can be registered with `County` objects using a glob-style path:

    val county = County.create()
    val mypool = new BlueprintsPool(graph)
    county.counterFactories = ("a.b.**" -> mypool) :: county.counterFactories

This creates a new `CounterPool` and adds it to the head of a list of path -> pool mappings. The list is
iterated in order and if the glob matches the current node's path, the corresponding pool is used to
create the counter. By default the list contains a fallback mapping `"**" -> BasicCounterPool`. With the
example above, `county("a.b.c.d").increment()` yields in creating a counter using the `BlueprintsPool` while
`coutny("a.d.x").increment()` would create a counter using the fallback `BasicCounterPool`.


# Other Examples

    county("pages.visits.my-webpage-html").increment()

    // login count for user mary
    county("app.logins.mary").totalCount

    // same as above
    county("app")("logins")("mary").totalCount

    // successful login count for mary
    county("app.logins.mary.success").totalCount

    // total login count for all user
    county("app.logins").totalCount

    // total count of all successful logins
    county("app.logins.*.success").totalCount

    // how many visits from china. this is generated from the list of ips
    county("pages.visits.byip").filterKey(ip => geolocation(ip))("china").totalCount

    // transform keys: look up the country for an ip and uses the result as real key
    val ipcounter = county("pages.visits.bycountry").transformKey(ip => geolocation(ip))
    ipcounter("80-100-100-1").increment()