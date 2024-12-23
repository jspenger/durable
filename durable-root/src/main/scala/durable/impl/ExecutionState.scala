package durable.impl

import durable.*

trait ExecutionState:
  def freshUID(): UID

  def insertDBlock(block: DBlock): Unit

  def insertDPromiseData(pd: DPromiseData): Unit

  def insertDLogMsg(logMsg: DLogMsg): Unit

  def softDeleteDBlock(key: UID): Unit

  def softDeleteDPromiseData(key: UID): Unit

  def getDPromiseData(key: UID): Option[DPromiseData]

  def getDBlock(key: UID): Option[DBlock]

  def getDependencies(deps: Iterable[DPromise[?]]): Iterable[Option[DPromiseData]]

  def getReadyDBlocks(): Iterable[(DBlock, Iterable[DPromiseData])]

  /** Checkpoint the execution state to `fname`. Returns false if file already
    * exists. Does not overwrite existing files.
    */
  def checkpoint(fname: String): Boolean

  /** Restore the execution state from a checkpoint `fname`. Returns false if
    * fname does not exist.
    */
  def restoreCheckpoint(fname: String): Boolean

  /** Delete checkpoint `fname`. Returns false if file does not exist. */
  def deleteCheckpoint(fname: String): Boolean

  def garbageCollect(): Unit

  def status: String
end ExecutionState
