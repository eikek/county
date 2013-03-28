# County -- library for counting things

County is a library written in Scala that aims to provide flexible and easy to use ways
to count things. Counters are often used in all sorts of applications, ususally to create
a simple kind of statistics, like the number of visits to a web page, number of failed or
successful logins etc. It can quickly grow to manage counts by username or ip address
maybe to find ips that initiate brute force attacks.

County provides an API to easily count all kind of things and create some simple statistics
from it. It provides persistent counters as well as in-memory ones and lets you combine
those.


## Building block -- the *Counter*

The simple entity, everything else is build on top, is the `Counter`. A `Counter` is very
simple, it only allows to add numeric values. A counter additionally maps the timestamp
of modification to the new value. So it really is a map of timestamps to numeric values.
Since it is usually not sensible to have a counter per millisecond, a `Granulariy` can
be specified to normalize the timestamp. It may, for example, zero-out the millisecond, 
second and minute parts of a timestamp. This can be used to control the size of the 
counter map.

The counter itself does not define _what_ it counts, it just manages some numbers. The
counter lets you read its total count as well as counts in a time range. For example, you
might want to know the total number of visits of a web page and the number of visits on a
certain day. To answer this, the counter must be configured with a minimum `Granularity`,
of course.

Next, there is is a registry for counters, the `CounterPool`. This registry finally maps a
`String` to a counter, which names the event to be counted.

So the main component is a set of named counters. You can retrieve or create counters by
name and use them.


## Structuring

Counters can be organized in a tree using the `County` object. The `County` object creates
counters by handing in a path of names. For example:

    county("a.b.1")
    county("a")("b")("1")
    val b = county("a.b"); b("1")

creates a linked list with nodes `a`, `b` and `1` and returns a counter (all lines are
equivalent). The statement `county("a")` returns a new `County` of name `a` and adds it
to the child list of the root node. The next time `county("a")` is called, it returns the
previously created element. A shortcut is to specify the whole path as a string, where
segments are separated by dots `.`.  Modifying the counter

    county("a.b.1").increment()

first selects the node `1` and calls `increment()` on it. If this is the last node, it is
now initialized with a new counter and `increment()` is called on this. More counters 
can be added the same way:

    county("a.b.2").increment()
    county("a.b.3").increment()
    county("a.c.1").increment()
    ...

Now there is a simple tree

       a
       |`.
       b  c
     .´|`. `.
    1  2  3  1

where the leaf nodes have real counters attached. Inner nodes are composite counters that delegate to
its children. That means an inner node's value is the sum of its children. For the example above

    county("a.b").totalCount

would return `3`. Modifying an inner node results in modifying all its children recursively. Thus the
counters of all leafs reachable from the inner node are modified. Once a leaf node has been initialized
with a real counter, it stays a leaf node. It is not possible to add another node to it:

    county("a.b.1")             // creates three linked nodes 'a'->'b'->'1'. The last node is uninitialized
    county("a.b.1").increment() // initializes node '1' and increments its new counter
    county("a.b.1.2")           // ERROR cannot create a child of a leaf node

but the following is ok, since the last node has not been initialized:

    county("a.b.1")
    county("a.b.1.2")

In general, `county(path)` retrieves a node from the tree, creating it if it does not exist yet.

The `County` object allows to retrieve a list of children that names the next nodes in the tree. For
leaf nodes, the list is empty.

### Counter Sets

Specifying more than one path is possible and yields in a composite counter, that will operate on the
given set of nodes. The same can be achieved by using path patterns with wildcards `*` or `?`.
For example:

    county("a.b.1", "a.c.1").increment()
    county("a.b|c.1").increment()

would increment the counters `a.b.1` and `a.c.1`. You can specify a list of children to select by
separating the names by '|'. All nodes that don't exist, are created.

Additionally, you can use wildcards to select from _existing child nodes_. Use `*` to match any character
multiple times or `?` to match exactly one character.

    county("a.*.1").increment()

If the path pattern does not match any node, an empty counter is returned that cannot be modified.

### Filtering Keys

It is possible to create a counter that applies a given function to its children:

    val even = county("a.b").filterKeys(s => (s.toInt % 2).toString)
    even("0").increment()

The function is applied to the children of node `b`, which are numbers `1`, `2` and `3`. The function
get the remainder of the division by 2 of the argument. This means, that the counter created by `even("0")`
is collecting all children with even numbers and `even("1")` would collect the other.

Note that the function is applied to existing child nodes and creates a view of them. The line `even("3")`
would collect nothing and calling `increment()` on it has no effect.

### Transforming Keys

It's possible to provide a function that is applied when adding new childs. In contrast to `filterKey` this
is applied to the path argument before the child node is created.

    val c = county("a.b").transformKey(key => key +"s")
    c("word").increment()
    c("letter").increment()

The function adds an extra 's' character to its argument, thus `c("word")` is equivalent to `county("a.b.words")`. 
Its not just a view of existing children, like `filterKey`.

## Controlling Counter Implementations

A `CounterPool` is a generic interface for managing counters by names. The `BasicCounterPool` manages a 
in-memory map and there are also jdbc and [blueprints](http://blueprints.tinkerpop.com/) backends to store
counters in a database.

Those pools can be registered with `County` objects using a glob-style path:

    val county = County.create()
    val mypool = new BlueprintsPool(graph)
    county.counterFactories = ("a.b.**" -> mypool) :: county.counterFactories

This creates a new `CounterPool` and adds it to the head of a list of path -> pool mappings. The list is
iterated in order and if the glob matches the current node's path, the corresponding pool is used to
create the counter. By default the list contains a fallback mapping `"**" -> BasicCounterPool`. With the
example above, `county("a.b.c.d").increment()` yields in creating a counter using the `BlueprintsPool` while
`coutny("a.d.x").increment()` would create a counter using the fallback `BasicCounterPool`.

The path pattern can contain `**` to match everything, including the boundary character `.`, `*` to match
any number of characters but the boundary character and `?` to match any single character.

## Other Examples

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


## Plotting the data

The module "county-xchart" uses the [xchart](http://xeiam.com/xchart.jsp) library to plot
counter data.

When creating a chart for a `Counter`:

* x-axis: the timestamp formatted as date
* y-axis: the `countIn(key.interval)` value
* `range` option: to include only those keys within the range (inclusive)
* `resolution` option: consolidate the keys by mapping timestamps to more
   coarse timestamps. For example: map counters of every minute to counters
   of a day or week
* `compact` option: a `CompositeCounter` is treated like a single counter
  if `compact == true`. If set to `false`, then every internal counter is
  added to the same chart.
* 0 values are removed from the chart (means no measure point)

When creating a chart for a `County`:

* x-axis: the names of the child nodes
* y-axis: either the total count or the value of `countIn()`
  of the child counter
* `range` option: used as argument for `countIn()` when providing values for
  the y-axis. If not set, `totalCount` is used
* `resolution` option: not used here
* `compact` option: a `CompositeCounty` is treated like a single county, if
  `compact == true`. If set to `false` every internal `County` is added to the graph
* If the `County` has no children, it is treated as a single `Counter`

The options allow to provide a function to further customize the chart. This
function is applied as the last step, after the data has been added. Use it
to customize the style of the chart.

Import the `CountyChart._` or just the `CounterChart.apply` function. This function
is annotated with `implicit` and wraps your `County` into a `CountyChart` which makes
two methods available: `createChart` and `createCounterChart`.

![example chart](https://github.com/eikek/county/raw/master/screen1.png)


## Java Api

There is a thin Java wrapper to make using county from Java less painful. It just wraps some odd
calls to scala objects. When using county from java, you must add the scala library to the
dependencies.

The Java Api is located in the packate `org.eknet.countyj`. A new `County` object can be created
using

    JDefaultCounty county = Counties.create();

Nodes are selected using `county.apply()`, a better shortcuts is `county.get(path)`:

    county.get("app.logins").get(username).totalCount

