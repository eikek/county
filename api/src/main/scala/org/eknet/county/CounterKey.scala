package org.eknet.county

import util.parsing.combinator.RegexParsers
import java.util.regex.Pattern

/**
 *
 * @author <a href="mailto:eike.kettner@gmail.com">Eike Kettner</a>
 * @since 22.03.13 13:16
 * 
 */
final case class CounterKey(path: List[String]) {

  lazy val empty = path.isEmpty
  lazy val size = path.size
  lazy val asString = path.mkString(".")
  lazy val head = CounterKey(path.head)
  lazy val tail = CounterKey(path.tail)

  lazy val lastSegment = path.last
  lazy val headSegment = path.head

  def / (seg: CounterKey) = CounterKey(path ::: seg.path)

  def hasWildcard = path.find(s => s.contains("*") || s.contains("?")).isDefined
}

object CounterKey {

  implicit def apply(path: String): CounterKey = parse(path, '.')

  def parse(path: String, delimiter: Char): CounterKey = {
    val parser = new KeyParser(delimiter)
    CounterKey(parser.readPath(path))
  }

  private class KeyParser(del: Char) extends RegexParsers {
    private val segment = ("[^"+ Pattern.quote(del.toString) +"]+").r

    private[this] def path = segment ~ rep(del ~ segment) ^^ {
      case seg ~ next => seg :: next.map(_._2)
    }

    def readPath(p: String) = parseAll(path, p) match {
      case Success(result, _) => result
      case failure : NoSuccess => scala.sys.error(failure.msg)
    }
  }
}
