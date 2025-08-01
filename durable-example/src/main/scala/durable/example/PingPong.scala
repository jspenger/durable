package durable.example

import spores.default.*
import spores.default.given
import spores.jvm.*

import durable.*
import durable.given

object PingPong:
  inline def sleep = Thread.sleep(1)

  def ping(n: Int)(using DExecutionContext): DFuture[Unit] =
    // format: off
    DFuture.apply { Spore.applyWithEnv(n) {
      case n if n > 0 =>
        sleep
        ctx.log("ping " + n)
        pong(n - 1)
      case _ =>
        ctx.log("finished")
    }}
    // format: on

  def pong(n: Int)(using DExecutionContext): DFuture[Unit] =
    // format: off
    DFuture.apply { Spore.applyWithEnv(n) {
      case n if n > 0 =>
        sleep
        ctx.log("pong " + n)
        ping(n - 1)
      case _ =>
        ctx.log("finished")
    }}
    // format: on

  def workflow(n: Int) =
    // format: off
    DWorkflow { Spore.applyWithEnv(n) { n =>
      ctx.log("start for n: " + n)
      ping(n)
    }}
    // format: on

  def main(args: Array[String]): Unit =
    val n = 16
    val name = "ping-pong" + scala.util.Random().nextInt(1000)
    val service = DExecutionServices.synchronous(name)
    // Observability.watch(service)
    service.execute(workflow(n))
    service.shutDown()
