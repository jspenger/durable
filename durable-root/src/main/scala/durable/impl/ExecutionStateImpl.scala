package durable.impl

import java.io.*

import upickle.default.*

import durable.*

class MapDExecutionStateImpl() extends ExecutionState:

  private val state = State.empty

  override def freshUID(): UID =
    this.state.dUID += 1
    this.state.dUID

  override def insertDBlock(block: DBlock): Unit =
    this.state.dBlock.insert(block.uid, block)

  override def insertDPromiseData(pd: DPromiseData): Unit =
    this.state.dPromiseData.insert(pd.uid, pd)

  override def softDeleteDBlock(key: UID): Unit =
    this.state.dBlock.softDelete(key)

  override def softDeleteDPromiseData(key: UID): Unit =
    this.state.dPromiseData.softDelete(key)

  override def getDPromiseData(key: UID): Option[DPromiseData] =
    this.state.dPromiseData.get(key)

  override def getDBlock(key: UID): Option[DBlock] =
    this.state.dBlock.get(key)

  override def getDependencies(deps: Iterable[DPromise[?]]): Iterable[Option[DPromiseData]] =
    deps.map { dep =>
      this.state.dPromiseData.get(dep.uid)
    }

  override def getReadyDBlocks(): Iterable[(DBlock, Iterable[DPromiseData])] =
    state.dBlock.rows.values.flatMap { block =>
      if !block.retryPolicy.canRetry then None
      else
        val deps = getDependencies(block.dependencies)
        if !deps.forall(_.flatMap(_.result).isDefined) then None
        else Some((block, deps.map(_.get)))
    }

  override def checkpoint(fname: String): Boolean =
    val file = File(fname)
    if file.exists() then false
    else
      val out = FileOutputStream(file)
      val data =
        write(this.state)
          .getBytes()
      out.write(data)
      out.close()
      true

  override def restoreCheckpoint(fname: String): Boolean =
    val file = File(fname)
    if !file.exists() then false
    else
      val in = FileInputStream(file)
      val data = in.readAllBytes()
      val restored = read[State](data)
      this.state.replaceWith(restored)
      in.close()
      true

  override def deleteCheckpoint(fname: String): Boolean =
    val file = File(fname)
    if !file.exists() then false
    else
      file.delete()
      true

  /** Check if the `obj`ect / Spork contains a reference to a DPromise or
    * DFuture with the given `uid`.
    */
  private def containsRef(obj: Any, uid: UID): Boolean =
    import sporks.SporkExtractor.*

    obj match
      // format: off
      case DPromise(uid) if uid == uid => true
      case DFuture(uid)  if uid == uid => true
      case h *: t =>
        containsRef(h, uid) || containsRef(t, uid)
      case h :: t =>
        containsRef(h, uid) || containsRef(t, uid)
      case x @ Packed0() =>
        containsRef(x.unwrap(), uid)
      case Packed1(packed, env) =>
        containsRef(env.unwrap(), uid) || containsRef(packed, uid)
      case Packed2(packed, env) =>
        containsRef(env.unwrap(), uid) || containsRef(packed, uid)
      case _ => false // unknown
      // format: on

  /** Check if the `block` depends on the `promise`.
    *
    * A block depends on a promise if:
    *   - The promise is in the block's dependencies
    *   - The promise is the block's promise
    *   - The promise is serialized in the block's spork
    */
  private def dependsOn(block: DBlock, promise: DPromiseData): Boolean =
    block.dependencies.exists(_.uid == promise.uid)
      || block.promise.uid == promise.uid
      || containsRef(block.spork, promise.uid)

  /** Garbage collect all redundant DBlocks and DPromises.
    *
    * There are no redundant DBlocks. They are deleted after successful
    * execution.
    *
    * A DPromise is redundant if there is no block that depends on it (see
    * [[dependsOn]] above).
    */
  override def garbageCollect(): Unit =
    this.state.dPromiseData.rows.foreach { case (uid, promise) =>
      if !this.state.dBlock.rows.values.exists { block =>
          this.dependsOn(block, promise)
        }
      then this.state.dPromiseData.softDelete(uid)
    }

  override def status: String =
    val nRows = 10
    val maxWidth = 160

    TablePrinter.pprint(
      "DBlock",
      List("uid", "spork", "dependencies", "promise", "retryPolicy"),
      this.state.dBlock,
      nRows,
      maxWidth,
    )
      ++ "\n"
      ++ "\n"
      ++ TablePrinter.pprint(
        "DPromiseData",
        List("uid", "result", "tpe"),
        this.state.dPromiseData,
        nRows,
        maxWidth,
      )
      ++ "\n"
      ++ "\n"
      ++ TablePrinter.pprint(
        "DLogMsg",
        List("uid", "timestamp", "msg"),
        this.state.dLog,
        nRows,
        maxWidth,
      )
      ++ "\n"

  override def insertDLogMsg(logMsg: DLogMsg): Unit =
    this.state.dLog.insert(logMsg.uid, logMsg)
