package org.eknet.county.tree

import org.eknet.county.CounterKey
import scala.collection.mutable.ListBuffer
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.eknet.county.util.Glob
import org.eknet.county

/**
 * @author Eike Kettner eike.kettner@gmail.com
 * @since 13.04.13 12:33
 */
abstract sealed class Tree[A](var parent: Option[Tree[A]], val name: String, var data: Option[A]) {
  import county.util.locks._

  def this(parent: Option[Tree[A]], name: String, data: A) = this(parent, name, Option(data))
  def this(parent: Option[Tree[A]], name: String) = this(parent, name, None)

  private val childLock = new ReentrantReadWriteLock()

  protected def addChild(name: String): Tree[A]
  protected def removeChild(node: Tree[A])
  protected def findChildren(pattern: String): List[Tree[A]]

  def getPath: CounterKey = {
    parent match {
      case Some(p) => p.getPath / name
      case None => name
    }
  }

  def getChildren = {
    implicit val l = childLock.readLock()
    lockRead { findChildren("*") }
  }

  def hasChildren = {
    !getChildren.isEmpty
  }

  def select(key: CounterKey): List[Tree[A]] = {
    if (key.empty) {
      List(this)
    }
    else {
      val children = key.headSegment.flatMap(resolveSegment)
      if (key.size == 1) {
        children
      } else {
        children.flatMap(_.select(key.tail))
      }
    }
  }

  def remove(key: CounterKey) {
    if (!key.empty) {
      if (key.size == 1) {
        key.headSegment.flatMap(findChildren).foreach(n => removeChild(n))
      } else {
        val children = key.headSegment.flatMap(resolveSegment)
        children.foreach(_.remove(key.tail))
      }
    }
  }

  private def resolveSegment(seg: String) = {
    implicit val rl = childLock.readLock()
    implicit val wl = childLock.writeLock()
    lockRead {
      findChildren(seg) match {
        case Nil => {
          if (CounterKey.containsWildcard(seg)) {
            Nil
          } else {
            upgradeLock {
              List(addChild(seg))
            }
          }
        }
        case list => list
      }
    }
  }
}

object Tree {

  def create[A](): Tree[A] = new RootNode[A](CounterKey.defaultSegmentDelimiter)
  def create[A](delimiter: Char): Tree[A] = new RootNode[A](delimiter)

}

class MutableNode[A](parent: Option[Tree[A]], name: String, childList: ListBuffer[Tree[A]], delimiter: Char, data: Option[A]) extends Tree[A](parent, name, data) {
  def this(parent: Option[Tree[A]], name: String, delimiter: Char, data: A) = this(parent, name, ListBuffer(), delimiter, Option(data))
  def this(parent: Option[Tree[A]], name: String, delimiter: Char) = this(parent, name, ListBuffer(), delimiter, None)

  def addChild(name: String): Tree[A] = {
    val n = new MutableNode[A](Some(this), name, delimiter)
    childList += n
    n
  }

  def removeChild(node: Tree[A]) {
    childList -= node
    node.parent = None
  }

  def findChildren(pattern: String) = childList.filter(n => Glob(pattern, delimiter).matches(n.name)).toList
}

class RootNode[A](childList: ListBuffer[Tree[A]], delimiter: Char, data: Option[A]) extends MutableNode[A](None, "_root_", childList, delimiter, data) {
  def this(delimiter: Char) = this(ListBuffer(), delimiter, None)

  override def getPath = CounterKey.empty
}

