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


## Structuring, Transformations

- want to define transformation function for events. example: count ip addresses, but the
  transformation function will get the geolocation of that ip and count the location, not
  the ip.
- wnat those functions for counting and reading
- structure counters in a tree.


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