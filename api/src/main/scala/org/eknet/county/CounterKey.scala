package org.eknet.county

import scala.util.parsing.combinator.RegexParsers
import java.util.regex.Pattern

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

  def parse(path: String, delimiter: Char, separator: Char): CounterKey = {
    if (path.isEmpty) {
      CounterKey.empty
    } else {
      val parser = new KeyParser(delimiter, separator)
      CounterKey(parser.readPath(path))
    }
  }

  def containsWildcard(str: String) = str.contains("*") || str.contains("?")

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
