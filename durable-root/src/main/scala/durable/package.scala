import scala.util.*

import upickle.default.*

import sporks.*
import sporks.given
import sporks.jvm.*

/** Durable and fault tolerant computation library for Scala 3 with workflows
  * and futures.
  *
  * @example
  *   {{{
  * import sporks.*
  * import sporks.given
  * import sporks.jvm.*
  * import durable.*
  * import durable.given
  *
  * val workflow = DWorkflow.apply(1) { Spork.apply {
  *   DFuture.apply { Spork.apply {
  *     ctx.log("Hello, ")
  *   }}.onComplete { Spork.apply { _ =>
  *     ctx.log("World!")
  *   }}
  * }}
  *
  * val service = new DExecutionServices.synchronous()
  * Observability.watch(service)
  * service.execute(workflow)
  * service.shutDown()
  *   }}}
  */
package object durable {
  // format: off

  private[durable] val DURABLE = """
       __                 __    __   
  ____/ /_  ___________ _/ /_  / /__ 
 / __  / / / / ___/ __ `/ __ \/ / _ \
/ /_/ / /_/ / /  / /_/ / /_/ / /  __/
\__,_/\__,_/_/   \__,_/_.___/_/\___/ 
""".stripPrefix("\n")

  def ctx(using DExecutionContext): DExecutionContext = summon[DExecutionContext]

  private[durable] type PS[T] = PackedSpork[T]
  private[durable] type PRW[T] = PackedSpork[ReadWriter[T]]
  private[durable] type RW[T] = ReadWriter[T]
  private[durable] type DEX = DExecutionContext

  private[durable] given RW[DPromise[?]] = macroRW
  private[durable] given RW[DFuture[?]]  = macroRW
  
  private[durable] object FUTURE_RW extends SporkObject[RW[DFuture[?]]](macroRW)
  given future_rw: PRW[DFuture[?]]      = FUTURE_RW.pack().asInstanceOf
  given future_rw_t[T]: PRW[DFuture[T]] = FUTURE_RW.pack().asInstanceOf

  private[durable] object PROMISE_RW extends SporkObject[RW[DPromise[?]]](macroRW)
  given promise_rw: PRW[DPromise[?]]      = PROMISE_RW.pack().asInstanceOf
  given promise_rw_t[T]: PRW[DPromise[T]] = PROMISE_RW.pack().asInstanceOf

  private case class StackTraceElementRepr(
    declaringClass: String,
    methodName: String,
    fileName: String,
    lineNumber: Int
  ) derives ReadWriter

  private object StackTraceElementRepr:
    def toJava(x: StackTraceElementRepr): java.lang.StackTraceElement = new java.lang.StackTraceElement(x.declaringClass, x.methodName, x.fileName, x.lineNumber)
    def fromJava(x: java.lang.StackTraceElement): StackTraceElementRepr = StackTraceElementRepr(x.getClassName(), x.getMethodName(), x.getFileName(), x.getLineNumber())

  private case class ThrowableRepr(
    `type`: String,
    message: String,
    stack: Array[StackTraceElementRepr]
  ) derives ReadWriter

  private object ThrowableRepr:
    def toJava(x: ThrowableRepr): scala.Throwable = {
      val clazz = Class.forName(x.`type`)
      val constructor = clazz.getConstructor(classOf[String])
      val instance = constructor.newInstance(x.message).asInstanceOf[java.lang.Throwable]
      instance.setStackTrace(x.stack.map(StackTraceElementRepr.toJava))
      instance
    }

    def fromJava(x: java.lang.Throwable): ThrowableRepr = ThrowableRepr(x.getClass.getName(), x.getMessage(), x.getStackTrace().map(StackTraceElementRepr.fromJava))

  private given [T: RW]: RW[Try[T]] =     macroRW
  private given [T: RW]: RW[Failure[T]] = macroRW
  private given [T: RW]: RW[Success[T]] = macroRW
  private given [T: RW]: RW[Throwable] =  readwriter[String].bimap[Throwable](
    t => write(ThrowableRepr.fromJava(t)),
    str => ThrowableRepr.toJava(read[ThrowableRepr](str))
  )

  // Needed for bootstrapping the `unpackWithCtx` combinator
  private object PACKED_TUPLE_RW extends SporkObject[RW[Tuple2[PS[?], PS[?]]]](macroRW)
  private given packed_tuple_rw[T, U]: PRW[Tuple2[PS[T], PS[U]]] = PACKED_TUPLE_RW.pack().asInstanceOf

  extension [T, U](spork: PS[T ?=> U]) {
    private inline def unpackWithCtx(packed: PS[T]): PS[U] =
      Spork.applyWithEnv(spork, packed) { (spork, packed) =>
        spork.build().apply(using packed.build())
      }
  }

  given try_rw[T](using t_rw: PRW[T]): PRW[Try[T]] =
    Spork.apply { (t: RW[T]) ?=> summon[RW[Try[T]]] }.unpackWithCtx(t_rw)

  private given [T: RW, U: RW]: RW[Tuple2[T, U]] = macroRW
  given packed_tuple2_rw[T, U](using t: PRW[T], u: PRW[U]): PRW[Tuple2[T, U]] =
    Spork.apply { (t: RW[T]) ?=> (u: RW[U]) ?=> summon[RW[Tuple2[T, U]]] }.unpackWithCtx(t).unpackWithCtx(u)

  private given [T: RW, U: RW, V: RW]: RW[Tuple3[T, U, V]] = macroRW
  given packed_tuple3_rw[T, U, V](using t: PRW[T], u: PRW[U], v: PRW[V]): PRW[Tuple3[T, U, V]] =
    Spork.apply { (t: RW[T]) ?=> (u: RW[U]) ?=> (v: RW[V]) ?=> summon[RW[Tuple3[T, U, V]]] }.unpackWithCtx(t).unpackWithCtx(u).unpackWithCtx(v)

  private given [T: RW, U: RW, V: RW, W: RW]: RW[Tuple4[T, U, V, W]] = macroRW
  given packed_tuple4_rw[T, U, V, W](using t: PRW[T], u: PRW[U], v: PRW[V], w: PRW[W]): PRW[Tuple4[T, U, V, W]] =
    Spork.apply { (t: RW[T]) ?=> (u: RW[U]) ?=> (v: RW[V]) ?=> (w: RW[W]) ?=> summon[RW[Tuple4[T, U, V, W]]] }.unpackWithCtx(t).unpackWithCtx(u).unpackWithCtx(v).unpackWithCtx(w)

  private given [T: RW, U: RW, V: RW, W: RW, X: RW]: RW[Tuple5[T, U, V, W, X]] = macroRW
  given packed_tuple5_rw[T, U, V, W, X](using t: PRW[T], u: PRW[U], v: PRW[V], w: PRW[W], x: PRW[X]): PRW[Tuple5[T, U, V, W, X]] =
    Spork.apply { (t: RW[T]) ?=> (u: RW[U]) ?=> (v: RW[V]) ?=> (w: RW[W]) ?=> (x: RW[X]) ?=> summon[RW[Tuple5[T, U, V, W, X]]] }.unpackWithCtx(t).unpackWithCtx(u).unpackWithCtx(v).unpackWithCtx(w).unpackWithCtx(x)

  private given [T: RW, U: RW, V: RW, W: RW, X: RW, Y: RW]: RW[Tuple6[T, U, V, W, X, Y]] = macroRW
  given packed_tuple6_rw[T, U, V, W, X, Y](using t: PRW[T], u: PRW[U], v: PRW[V], w: PRW[W], x: PRW[X], y: PRW[Y]): PRW[Tuple6[T, U, V, W, X, Y]] =
    Spork.apply { (t: RW[T]) ?=> (u: RW[U]) ?=> (v: RW[V]) ?=> (w: RW[W]) ?=> (x: RW[X]) ?=> (y: RW[Y]) ?=> summon[RW[Tuple6[T, U, V, W, X, Y]]] }.unpackWithCtx(t).unpackWithCtx(u).unpackWithCtx(v).unpackWithCtx(w).unpackWithCtx(x).unpackWithCtx(y)

  private given [T: RW, U: RW, V: RW, W: RW, X: RW, Y: RW, Z: RW]: RW[Tuple7[T, U, V, W, X, Y, Z]] = macroRW
  given packed_tuple7_rw[T, U, V, W, X, Y, Z](using t: PRW[T], u: PRW[U], v: PRW[V], w: PRW[W], x: PRW[X], y: PRW[Y], z: PRW[Z]): PRW[Tuple7[T, U, V, W, X, Y, Z]] =
    Spork.apply { (t: RW[T]) ?=> (u: RW[U]) ?=> (v: RW[V]) ?=> (w: RW[W]) ?=> (x: RW[X]) ?=> (y: RW[Y]) ?=> (z: RW[Z]) ?=> summon[RW[Tuple7[T, U, V, W, X, Y, Z]]] }.unpackWithCtx(t).unpackWithCtx(u).unpackWithCtx(v).unpackWithCtx(w).unpackWithCtx(x).unpackWithCtx(y).unpackWithCtx(z)
}
