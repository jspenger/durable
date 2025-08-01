package durable.example

import spores.default.*
import spores.default.given
import spores.jvm.*

import durable.*
import durable.given

object Fibonacci {
  // Option 1:
  // format: off
  def fib(n: Int)(using DExecutionContext): DFuture[Int] = n match
    case 0 => DFuture { Spore.apply { 0 } }
    case 1 => DFuture { Spore.apply { 1 } }
    case _ =>
      fib(n - 1).flatMap { Spore.applyWithEnv(n) { n => n1 =>
        fib(n - 2).map { Spore.applyWithEnv(n1) {  n1 => n2 =>
          n1 + n2
        }}
      }}
  // format: on

  // // Option 2:
  // // format: off
  // def fib(n: Int)(using DExecutionContext): DFuture[Int] = n match
  //   case 0 => DFuture { Spore.apply { 0 } }
  //   case 1 => DFuture { Spore.apply { 1 } }
  //   case _ =>
  //     val f1 = fib(n - 1)
  //     val f2 = fib(n - 2)
  //     f1.zip(f2).map { Spore.apply { (n1, n2) =>
  //       n1 + n2
  //     }}
  // format: on

  def main(args: Array[String]): Unit = {
    // format: off
    val n = 10
    val workflow = DWorkflow { Spore.applyWithEnv(n) { n =>
      fib(n).onComplete { Spore.applyWithEnv(n) { n => result =>
        ctx.log("Completed result of fib(" + n + "): " + result)
      }}
    }}
    // format: on

    val name = "fibonacci" + scala.util.Random().nextInt(1000)
    val service = DExecutionServices.synchronous(name)
    // Observability.watch(service)
    service.execute(workflow)
    service.shutDown()
  }
}
