package durable

import upickle.default.*

import sporks.*
import sporks.given

/** Internal API. String representation of a *type*. Also contains a PackedSpork
  * for the reading and writing of the represented type.
  *
  * @param manifest
  *   String representation of the type.
  * @param rw
  *   PackedSpork for reading and writing the type.
  */
private[durable] case class TypeInfo(
    manifest: String,
    rw: PackedSpork[ReadWriter[?]]
) derives ReadWriter
end TypeInfo

private[durable] object TypeInfo:
  /** Internal API. TypeInfo factory using Scala's Manifest. */
  def apply[T: scala.reflect.ClassTag](obj: T)(using PackedSpork[ReadWriter[T]]): TypeInfo =
    TypeInfo(
      scala.reflect.classTag[T].toString(),
      summon[PackedSpork[ReadWriter[T]]],
    )

  /** Internal API. TypeInfo factory using Scala's Manifest. */
  def apply[T: scala.reflect.ClassTag](using PackedSpork[ReadWriter[T]]): TypeInfo =
    TypeInfo(
      scala.reflect.classTag[T].toString(),
      summon[PackedSpork[ReadWriter[T]]],
    )
end TypeInfo
