package org.eknet.county

import util.parsing.combinator.RegexParsers
import java.util.regex.Pattern

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 22.03.13 13:16
 * 
 */
final case class CounterKey(path: List[List[String]]) {

  lazy val empty = path.isEmpty
  lazy val size = path.size
  lazy val asString = path.map(seg => seg.mkString("|")).mkString(".")
  lazy val head = CounterKey(List(path.head))
  lazy val tail = CounterKey(path.tail)

  lazy val lastSegment = path.last
  lazy val headSegment = path.head

  def / (seg: CounterKey) = CounterKey(path ::: seg.path)

  def hasWildcard = path.flatMap(s => s).find(s => s.contains("*") || s.contains("?")).isDefined

  override def toString = asString
}

object CounterKey {

  val empty = CounterKey(List())

  val defaultSegmentDelimiter = '.'
  val defaultNameSeparator = '|'

  implicit def apply(path: String): CounterKey = parse(path, defaultSegmentDelimiter, defaultNameSeparator)

  def parse(path: String, delimiter: Char, separator: Char): CounterKey = {
    val parser = new KeyParser(delimiter, separator)
    CounterKey(parser.readPath(path))
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
