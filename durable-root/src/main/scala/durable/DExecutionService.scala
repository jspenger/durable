package durable

import scala.util.*

import upickle.default.*

import sporks.*

/** Service for executing durable workflows. */
trait DExecutionService:

  def execute(dWorkflow: DWorkflow): Unit

  def shutDown(): Unit

  def isTerminated: Boolean

  def status: String

  private[durable] def freshUID(): UID

  private[durable] def timestamp(): Long

  private[durable] def submitBlock0[R](spork: PackedSpork[DEX ?=> R])(using PackedSpork[ReadWriter[Try[R]]]): DFuture[R]

  private[durable] def submitBlock1[T, R](spork: PackedSpork[DEX ?=> Try[T] => R], dep: DPromise[T])(using PackedSpork[ReadWriter[Try[R]]]): DFuture[R]

  private[durable] def submitBlockN[T <: Tuple, R](spork: PackedSpork[DEX ?=> Try[T] => R], deps: List[DPromise[?]])(using PackedSpork[ReadWriter[Try[R]]]): DFuture[R]

  private[durable] def freshPromise[T]()(using rw: PackedSpork[ReadWriter[Try[T]]]): DPromise[T]

  private[durable] def promiseTryComplete[T](uid: UID, data: Try[T]): Boolean

  private[durable] def futureValue[T](uid: UID): Option[Try[T]]

  private[durable] def logMsg(msg: DLogMsg): Unit
end DExecutionService
