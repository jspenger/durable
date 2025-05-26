package durable

import scala.util.*

import upickle.default.*

import sporks.*
import sporks.given
import sporks.jvm.*

import durable.*
import durable.given

/** Durable representation of a result which may or may not be ready to read.
  *
  * @param uid
  *   The unique identifier of the `DFuture`.
  * @tparam T
  *   The type of the result that the `DFuture` will hold.
  */
case class DFuture[T] private[DFuture] (
    uid: UID
) derives ReadWriter:

  def value(using ctx: DEX): Option[Try[T]] =
    ctx.service.futureValue(uid)

  def onComplete(spork: PS[DEX ?=> Try[T] => Unit])(using PRW[Try[Unit]])(using DEX): Unit =
    ctx.service.submitBlock1(
      spork,
      DPromise.applyFromUID(this.uid),
    )

  def transform[U](spork: PS[DEX ?=> Try[T] => Try[U]])(using PRW[Try[U]])(using DEX): DFuture[U] =
    ctx.service.submitBlock1(
      Spork.applyWithEnv[PS[DEX ?=> Try[T] => Try[U]], DEX ?=> Try[T] => U](spork) { spork => result =>
        spork.unwrap().apply(result).get
      },
      DPromise.applyFromUID(this.uid),
    )

  def transformWith[U](spork: PS[DEX ?=> Try[T] => DFuture[U]])(using prw1: PRW[Try[DFuture[U]]], prw2: PRW[Try[U]])(using DEX): DFuture[U] =
    ctx.service
      .submitBlock1(
        spork,
        DPromise.applyFromUID(this.uid),
      )
      .flatten()

  def flatten[S]()(using ev: T <:< DFuture[S])(using PRW[Try[S]])(using DEX): DFuture[S] =
    val dProm = DPromise[S]()
    this.onComplete {
      Spork.applyWithEnv(dProm) { dProm => result =>
        result match
          case Failure(e) => dProm.tryComplete(Failure(e))
          case Success(t) =>
            t.asInstanceOf[DFuture[S]].onComplete {
              Spork.applyWithEnv(dProm) { dProm => result =>
                dProm.tryComplete(result)
              }
            }
      }
    }
    dProm.future

  def map[U](spork: PS[DEX ?=> T => U])(using PRW[Try[U]])(using DEX): DFuture[U] =
    this.transform {
      Spork.applyWithEnv(spork) { spork => result =>
        result match
          case fail @ Failure(_) => fail.asInstanceOf[Failure[U]]
          case Success(value) => Success(spork.unwrap().apply(value))
      }
    }

  def flatMap[U](spork: PS[DEX ?=> T => DFuture[U]])(using prw: PRW[Try[U]])(using DEX): DFuture[U] =
    this.transformWith {
      Spork
        .apply[PRW[Try[U]] ?=> PS[DEX ?=> T => DFuture[U]] => DEX ?=> Try[T] => DFuture[U]] { spork => result =>
          result match
            case fail @ Failure(_) => DPromise.fromTry[U](fail.asInstanceOf[Failure[U]]).future
            case Success(value) => spork.unwrap().apply(value)
        }
        .withCtx(prw)
        .withEnv(spork)
    }

  def zip[U](other: DFuture[U])(using rw1: Spork[ReadWriter[T]], rw2: Spork[ReadWriter[Try[(T, U)]]])(using DExecutionContext): DFuture[(T, U)] =
    this.sequence(Tuple(other))

  def sequence[U <: Tuple](others: Tuple.Map[U, DFuture])(using prw: PRW[Try[T *: U]])(using DExecutionContext): DFuture[T *: U] =
    val otherDeps = others.toList.asInstanceOf[List[DFuture[_]]].map { dFut => DPromise.applyFromUID(dFut.uid) }
    ctx.service.submitBlockN(
      Spork.apply[DEX ?=> Try[T *: U] => T *: U] { result =>
        result match
          case Failure(e) => throw e
          case Success(value) => value
      },
      DPromise.applyFromUID[T](this.uid) :: otherDeps
    )

end DFuture

object DFuture:
  /** Creates a new durable future from a spork.
    *
    * @param spork
    *   The executed function to produce the result of the `DFuture`.
    * @tparam T
    *   The type of the result that the `DFuture` will hold.
    */
  def apply[T](spork: Spork[DExecutionContext ?=> T])(using DExecutionContext)(using Spork[ReadWriter[Try[T]]]): DFuture[T] =
    ctx.service.submitBlock0(spork)

  /** Internal API. Create a DFuture from a UID. */
  private[durable] def applyFromUID[T](uid: UID): DFuture[T] =
    DFuture(uid)

end DFuture
