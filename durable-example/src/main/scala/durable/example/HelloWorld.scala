package durable.example

import spores.default.*
import spores.default.given

import durable.*
import durable.given

// format: off
object HelloWorld {

  def main(args: Array[String]): Unit = {

    val workflow = DWorkflow.apply { Spore.apply0 {
      DFuture.apply { Spore.apply0 {
        ctx.log("Hello, ")
      }}.onComplete { Spore.apply0 { _ =>
        ctx.log("World!")
      }}
    }}

    val dService = DExecutionServices.synchronous("hello-world")
    // Observability.watch(dService)
    dService.execute(workflow)
  }
}
