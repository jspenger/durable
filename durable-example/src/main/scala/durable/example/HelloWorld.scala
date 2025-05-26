package durable.example

import sporks.*
import sporks.given
import sporks.jvm.*

import durable.*
import durable.given

// format: off
object HelloWorld {

  def main(args: Array[String]): Unit = {

    val workflow = DWorkflow.apply { Spork.apply {
      DFuture.apply { Spork.apply {
        ctx.log("Hello, ")
      }}.onComplete { Spork.apply { _ =>
        ctx.log("World!")
      }}
    }}

    val dService = DExecutionServices.synchronous("hello-world")
    // Observability.watch(dService)
    dService.execute(workflow)
  }
}
