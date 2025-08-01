package durable

import scala.util.*

import upickle.default.*

import spores.default.*

/** Service for executing durable workflows. */
trait DExecutionService:

  def execute(dWorkflow: DWorkflow): Unit

  def shutDown(): Unit

  def isTerminated: Boolean

  def status: String

  private[durable] def freshUID(): UID

  private[durable] def timestamp(): Long

  private[durable] def submitBlock0[R](spore: Spore[DEX ?=> R])(using Spore[ReadWriter[Try[R]]]): DFuture[R]

  private[durable] def submitBlock1[T, R](spore: Spore[DEX ?=> Try[T] => R], dep: DPromise[T])(using Spore[ReadWriter[Try[R]]]): DFuture[R]

  private[durable] def submitBlockN[T <: Tuple, R](spore: Spore[DEX ?=> Try[T] => R], deps: List[DPromise[?]])(using Spore[ReadWriter[Try[R]]]): DFuture[R]

  private[durable] def freshPromise[T]()(using rw: Spore[ReadWriter[Try[T]]]): DPromise[T]

  private[durable] def promiseTryComplete[T](uid: UID, data: Try[T]): Boolean

  private[durable] def futureValue[T](uid: UID): Option[Try[T]]

  private[durable] def logMsg(msg: DLogMsg): Unit
end DExecutionService
