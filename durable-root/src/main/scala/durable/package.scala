import scala.util.*

import upickle.default.*

import spores.*
import spores.given
import spores.jvm.*

/** Durable and fault tolerant computation library for Scala 3 with workflows
  * and futures.
  *
  * @example
  *   {{{
  * import spores.*
  * import spores.given
  * import spores.jvm.*
  * import durable.*
  * import durable.given
  *
  * val workflow = DWorkflow.apply(1) { Spore.apply {
  *   DFuture.apply { Spore.apply {
  *     ctx.log("Hello, ")
  *   }}.onComplete { Spore.apply { _ =>
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

  private[durable] type PS[T] = Spore[T]
  private[durable] type PRW[T] = Spore[ReadWriter[T]]
  private[durable] type RW[T] = ReadWriter[T]
  private[durable] type DEX = DExecutionContext

  private[durable] given RW[DPromise[?]] = macroRW
  private[durable] given RW[DFuture[?]]  = macroRW
  
  private[durable] object FUTURE_RW extends SporeBuilder[RW[DFuture[?]]](macroRW)
  given future_rw: PRW[DFuture[?]]      = FUTURE_RW.build().asInstanceOf
  given future_rw_t[T]: PRW[DFuture[T]] = FUTURE_RW.build().asInstanceOf

  private[durable] object PROMISE_RW extends SporeBuilder[RW[DPromise[?]]](macroRW)
  given promise_rw: PRW[DPromise[?]]      = PROMISE_RW.build().asInstanceOf
  given promise_rw_t[T]: PRW[DPromise[T]] = PROMISE_RW.build().asInstanceOf

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

  given try_rw[T](using t_rw: PRW[T]): PRW[Try[T]] = Spore.apply { (t: RW[T]) ?=> summon[RW[Try[T]]]}.withCtx2(t_rw)

}
