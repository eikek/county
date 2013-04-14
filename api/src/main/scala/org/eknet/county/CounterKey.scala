package org.eknet.county

import scala.util.parsing.combinator.RegexParsers
import java.util.regex.Pattern
import scala.annotation.tailrec

/**
 * The representation of a county "path" like
 * {{{
 *   webapp.logins.byname.john|mary.sucess
 * }}}
 * The path is a list of segments that itself can be
 * specified a list. The example above would translate
 * into this:
 * {{{
 *   List( List("webapp"),List("logins"),List("byname"),List("john","mary"),List("success") )
 * }}}
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 22.03.13 13:16
 * 
 */
final case class CounterKey(path: List[List[String]]) {

  lazy val empty = path.isEmpty
  lazy val size = path.size
  lazy val asString = path.map(seg => seg.mkString(CounterKey.defaultNameSeparator+"")).mkString(CounterKey.defaultSegmentDelimiter+"")
  lazy val head = CounterKey(List(path.head))
  lazy val tail = CounterKey(path.tail)

  lazy val lastSegment = path.last
  lazy val headSegment = path.head

  def / (seg: CounterKey) = CounterKey(path ::: seg.path)

  /**
   * Returns `true` is there is at least one segment that contains
   * a wildcard.
   */
  lazy val hasWildcard = path.flatMap(s => s).exists(CounterKey.containsWildcard)

  override def toString = asString
}

object CounterKey {

  val empty = CounterKey(List())

  val defaultSegmentDelimiter = '.'
  val defaultNameSeparator = '|'

  implicit def apply(path: String): CounterKey = parse(path, defaultSegmentDelimiter, defaultNameSeparator)

  def apply(path: String, delimiter: Char): CounterKey = parse(path, delimiter, defaultNameSeparator)

  def parse(path: String, delimiter: Char, separator: Char): CounterKey = {
    if (path.isEmpty) {
      CounterKey.empty
    } else {
      val parser = new KeyParser(delimiter, separator)
      CounterKey(parser.readPath(path))
    }
  }

  /**
   * Creates a key using the given list as one segment.
   *
   * The following two lines are equivalent:
   * {{{
   *   CounterKey.create(List("a", "b"))
   *   CounterKey("a|b")
   * }}}
   *
   * @param multiple
   * @return
   */
  def create(multiple: Iterable[String]) = CounterKey(List(multiple.toList.sorted))

  /**
   * Creates a key with the given single segment.
   * @param segment
   * @return
   */
  def create(segment: String) = CounterKey(List(List(segment)))

  def containsWildcard(str: String) = str.contains("*") || str.contains("?")

  /**
   * Creates one [[org.eknet.county.CounterKey]] for a given list of [[org.eknet.county.CounterKey]]s.
   *
   * The `result` should be intially `CounterKey.empty`, because segments are appended to it
   * recursively.
   *
   * The segments of the new `CounterKey` are the union of the segments of the given `CounterKeys`
   * at the same position. For example:
   *
   * {{{
   *   "a.b.c", "a.e.c" -> "a.b|e.c"
   *   "a.b.c", "a.e.d" -> "a.b|e.c|d"
   *   "a.b.c", "a.b" -> "a.b"
   * }}}
   *
   * If the list contains keys of different length, longer ones are discarded.
   *
   * @param keys
   * @param result
   * @return
   */
  @tailrec
  private[county] final def mergePaths(keys: List[CounterKey], result: CounterKey): CounterKey = {
    if (keys.exists(_.empty)) {
      result
    } else {
      val mergedHead = keys.flatMap(k => k.headSegment).distinct
      mergePaths(keys.map(_.tail), result / CounterKey(List(mergedHead)))
    }
  }

  private class KeyParser(segmentDelimiter: Char, segmentSeparator: Char) extends RegexParsers {

    private val delimiters = List(segmentDelimiter.toString, segmentSeparator.toString)

    private val segLiteral = delimiters.map(s => Pattern.quote(s)).mkString("[^", "", "]+").r

    private val segplus = segLiteral ~ rep(segmentSeparator ~ segLiteral) ^^ {
      case s ~ next => (s :: next.map(_._2)).distinct.sorted
    }

    private[this] def path = segplus ~ rep(segmentDelimiter ~ segplus) ^^ {
      case seg ~ next => seg :: next.map(_._2)
    }

    def readPath(p: String) = parseAll(path, p) match {
      case Success(result, _) => result
      case failure : NoSuccess => scala.sys.error(failure.msg)
    }
  }
}
