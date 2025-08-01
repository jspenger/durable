package durable.impl

import scala.util.*

import upickle.default.*

import spores.default.*
import spores.default.given

import durable.*
import durable.given

class DExecutionServiceImpl(fname: String) extends DExecutionService {
  private val ctx = new DExecutionContext(this)
  private val state = new MapDExecutionStateImpl()
  private var isShutdown = false

  Logger.setRootLevel(Logger.Level.DEBUG)
  private val logger = Logger("DExecutionServiceImpl")
  private val appLogger = Logger(fname)

  override def execute(dWorkflow: DWorkflow): Unit =
    this.restore() match
      case false =>
        this.submitBlock0[Unit](dWorkflow.init)
        this.checkpoint()
      case true => ()
    this.isShutdown = false
    this.run()

  override def shutDown(): Unit = this.isShutdown = true

  override def isTerminated: Boolean = this.isShutdown

  override def status: String = this.state.status

  private[durable] override def freshUID(): UID = this.state.freshUID()

  private[durable] override def timestamp(): Long = System.currentTimeMillis()

  private def submitBlock01N[R](
      spore: Spore[?],
      deps: List[DPromise[?]]
  )(using
      Spore[ReadWriter[Try[R]]]
  ): DFuture[R] =
    val dPromise = this.freshPromise[R]()
    val dBlock =
      DBlock(
        this.freshUID(),
        spore,
        deps,
        dPromise,
        Repeat(Int.MaxValue),
      )
    this.state.insertDBlock(dBlock)
    dPromise.future

  private[durable] override def submitBlock0[R](
      spore: Spore[DEX ?=> R]
  )(using
      Spore[ReadWriter[Try[R]]]
  ): DFuture[R] =
    this.submitBlock01N[R](spore, List.empty)

  private[durable] override def submitBlock1[T, R](
      spore: Spore[DEX ?=> Try[T] => R],
      dep: DPromise[T]
  )(using
      Spore[ReadWriter[Try[R]]]
  ): DFuture[R] =
    this.submitBlock01N[R](spore, List(dep))

  private[durable] override def submitBlockN[T <: Tuple, R](
      spore: Spore[DEX ?=> Try[T] => R],
      deps: List[DPromise[?]]
  )(using
      Spore[ReadWriter[Try[R]]]
  ): DFuture[R] =
    this.submitBlock01N[R](spore, deps)

  private[durable] override def freshPromise[T](
  )(using
      rw: Spore[ReadWriter[Try[T]]]
  ): DPromise[T] =
    val dPromise =
      DPromiseData.empty[Try[T]](
        this.freshUID()
      )
    this.state.insertDPromiseData(dPromise)
    dPromise.promise

  private[durable] override def promiseTryComplete[T](
      uid: UID,
      data: Try[T]
  ): Boolean =
    this.state.getDPromiseData(uid) match
      case Some(old @ DPromiseData(uid, None, tpe)) =>
        val result = write(data)(using tpe.rw.unwrap().asInstanceOf)
        val newProm = old.copy(result = Some(result))
        this.state.insertDPromiseData(newProm)
        true
      case Some(DPromiseData(_, Some(_), _)) => false
      case None => throw new Exception(s"Promise with UID $uid not found")

  private[durable] override def futureValue[T](
      uid: UID
  ): Option[Try[T]] =
    this.state.getDPromiseData(uid) match
      case Some(DPromiseData(_, Some(result), tpe)) =>
        Some(
          Try(
            read(result)(tpe.rw.unwrap())
          )
        ).asInstanceOf[Option[Try[T]]]
      case Some(DPromiseData(_, None, _)) => None
      case None => throw new Exception(s"Promise with UID $uid not found")

  private[durable] override def logMsg(dLogMsg: DLogMsg): Unit =
    appLogger.info(dLogMsg.message)
    this.state.insertDLogMsg(dLogMsg)

  private def runDBlock(
      dBlock: DBlock,
      deps: Iterable[DPromiseData]
  ): Try[Any] =
    val fun = dBlock.spore.unwrap()

    // format: off
    val res = Try { deps match
      // Block0
      case Nil =>
        fun.asInstanceOf[DExecutionContext ?=> Any]
          .apply(using this.ctx)
      
      // Block1
      case x :: Nil =>
        fun
          .asInstanceOf[DExecutionContext ?=> Any => Any]
          .apply(using this.ctx)
          .apply(read(x.result.get)(using x.tpe.rw.unwrap()))
      
      // BlockN
      case _ =>
        // Convert the list of DPromiseData into a Try[Tuple]
        val tupled = {
          val listOfRes = deps.toList.map{ dep => read(dep.result.get)(using dep.tpe.rw.unwrap()) }.asInstanceOf[List[Try[?]]]
          val flattened = Try(listOfRes.map(_.get))
          flattened match
            case Failure(exception) => Failure(exception)
            case Success(List(v1, v2)) => Success((v1, v2))
            case Success(List(v1, v2, v3)) => Success((v1, v2, v3))
            case Success(List(v1, v2, v3, v4)) => Success((v1, v2, v3, v4))
            case Success(List(v1, v2, v3, v4, v5)) => Success((v1, v2, v3, v4, v5))
            case Success(List(v1, v2, v3, v4, v5, v6)) => Success((v1, v2, v3, v4, v5, v6))
            case Success(List(v1, v2, v3, v4, v5, v6, v7)) => Success((v1, v2, v3, v4, v5, v6, v7))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8)) => Success((v1, v2, v3, v4, v5, v6, v7, v8))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21))
            case Success(List(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22)) => Success((v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20, v21, v22))
            case _ => throw new Exception("Maximum number of dependencies exceeded")
        }
        fun
          .asInstanceOf[DExecutionContext ?=> Try[Tuple] => Any]
          .apply(using this.ctx)
          .apply(tupled)
    }

    res
    // format: on

  private val suffix = ".durable.json"
  private val tmp = "_tmp"
  private def filetmpStr: String = fname + tmp + suffix
  private def fileStr: String = fname + suffix

  private def checkpoint(): Unit =
    this.state.deleteCheckpoint(filetmpStr)
    this.state.checkpoint(filetmpStr)
    this.state.deleteCheckpoint(fileStr)
    this.state.checkpoint(fileStr)
    this.state.deleteCheckpoint(filetmpStr)

  private def restore(): Boolean =
    this.state.restoreCheckpoint(fileStr) match
      case true =>
        this.logger.info("Execution restored from disk")
        true
      case false =>
        this.state.restoreCheckpoint(filetmpStr) match
          case true =>
            this.logger.info("Execution restored from disk")
            true
          case false =>
            this.logger.info("Execution started from scratch")
            this.state.deleteCheckpoint(fileStr)
            this.state.deleteCheckpoint(filetmpStr)
            this.state.checkpoint(fileStr)
            false

  private def run(): Unit =
    while !this.isShutdown do {
      this.state.getReadyDBlocks() match
        case Nil =>
          // No more ready blocks, we are done
          this.shutDown()

        case readyBlocks =>
          // Execute all ready blocks
          for (dBlock, deps) <- readyBlocks do
            this.runDBlock(dBlock, deps) match

              // Execution failed
              case Failure(exception) =>
                this.logger.error(
                  s"Execution failed for DBlock: $dBlock." + "\r\n"
                    + s"Exception: $exception" + "\r\n"
                    + s"${exception.getStackTrace().mkString("\r\n")}" + "\r\n"
                )
                val newDBlock = dBlock.copy(retryPolicy = dBlock.retryPolicy.fail)
                this.state.insertDBlock(newDBlock)
                if !newDBlock.retryPolicy.canRetry then
                  this.promiseTryComplete(
                    dBlock.promise.uid,
                    Failure(exception)
                  )

              // Execution succeeded
              case Success(value) =>
                this.promiseTryComplete(
                  dBlock.promise.uid,
                  Success(value)
                )
                this.state.softDeleteDBlock(dBlock.uid)
      this.state.garbageCollect()
      this.checkpoint()
    }
}
