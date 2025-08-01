package durable

import upickle.default.*

import spores.default.*
import spores.default.given

/** Internal API. String representation of a *type*. Also contains a Spore for
  * the reading and writing of the represented type.
  *
  * @param manifest
  *   String representation of the type.
  * @param rw
  *   Spore for reading and writing the type.
  */
private[durable] case class TypeInfo(
    manifest: String,
    rw: Spore[ReadWriter[?]]
) derives ReadWriter
end TypeInfo

private[durable] object TypeInfo:
  /** Internal API. TypeInfo factory using Scala's Manifest. */
  def apply[T: scala.reflect.ClassTag](obj: T)(using Spore[ReadWriter[T]]): TypeInfo =
    TypeInfo(
      scala.reflect.classTag[T].toString(),
      summon[Spore[ReadWriter[T]]],
    )

  /** Internal API. TypeInfo factory using Scala's Manifest. */
  def apply[T: scala.reflect.ClassTag](using Spore[ReadWriter[T]]): TypeInfo =
    TypeInfo(
      scala.reflect.classTag[T].toString(),
      summon[Spore[ReadWriter[T]]],
    )
end TypeInfo
