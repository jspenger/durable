package durable

import scala.util.*

import upickle.default.*

import sporks.*

/** A durable promise of a result with type `T`.
  *
  * @tparam uid
  *   The unique identifier of the `DPromise`.
  * @tparam T
  *   The result type that the `DPromise` holds.
  */
case class DPromise[T] private[DPromise] (
    uid: UID
) derives ReadWriter:

  def future: DFuture[T] =
    DFuture.applyFromUID(this.uid)

  def isCompleted(using ctx: DExecutionContext): Boolean =
    ctx.service.futureValue(uid).isDefined

  def tryComplete(result: Try[T])(using ctx: DExecutionContext): Boolean =
    ctx.service.promiseTryComplete(uid, result)
end DPromise

object DPromise:
  /** Creates a new durable promise.
    *
    * @tparam T
    *   The result type that the promise holds.
    * @return
    *   A new durable promise.
    */
  def apply[T]()(using DExecutionContext)(using Spork[ReadWriter[Try[T]]]): DPromise[T] =
    ctx.service.freshPromise[T]()

  def fromTry[T](result: Try[T])(using DExecutionContext)(using prwt: Spork[ReadWriter[Try[T]]]): DPromise[T] =
    val promise = DPromise.apply[T]()
    promise.tryComplete(result)
    promise

  /** Internal API. Create a durable promise from a UID. Note: This does not
    * create a new durable promise in the execution state.
    */
  private[durable] def applyFromUID[T](uid: UID): DPromise[T] =
    DPromise[T](uid)
end DPromise
