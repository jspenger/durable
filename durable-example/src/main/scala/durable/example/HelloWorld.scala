package durable.example

import spores.*
import spores.given
import spores.jvm.*

import durable.*
import durable.given

// format: off
object HelloWorld {

  def main(args: Array[String]): Unit = {

    val workflow = DWorkflow.apply { Spore.apply {
      DFuture.apply { Spore.apply {
        ctx.log("Hello, ")
      }}.onComplete { Spore.apply { _ =>
        ctx.log("World!")
      }}
    }}

    val dService = DExecutionServices.synchronous("hello-world")
    // Observability.watch(dService)
    dService.execute(workflow)
  }
}
