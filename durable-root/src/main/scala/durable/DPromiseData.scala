package durable

import upickle.default.*

import spores.*

/** Internal API. ReadWriter'able representation of a `Dpromise`'s data.
  *
  * @param uid
  *   The unique identifier of the `DPromise`.
  * @param result
  *   The result of the `DPromise`. If None, then the `DPromise is not yet
  *   resolved. If it is Some, then its result is resolved and final.
  * @param tpe
  *   The type information of the `DPromise`, contains a `Spore[ReadWriter]` for
  *   serializing and deserializing the result.
  */
private[durable] case class DPromiseData(
    uid: UID,
    result: Option[String], // Represents: Option[Try[T]]
    tpe: TypeInfo // Represents: Try[T]
) derives ReadWriter:
  def promise[T]: DPromise[T] = DPromise.applyFromUID(uid)
end DPromiseData

private[durable] object DPromiseData:
  /** Internal API. Create an empty `DPromiseData` for a given `uid`. */
  def empty[T: scala.reflect.ClassTag](uid: UID)(using Spore[ReadWriter[T]]): DPromiseData =
    DPromiseData(
      uid,
      None,
      TypeInfo.apply[T],
    )
end DPromiseData
