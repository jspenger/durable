package durable.example

import sporks.*
import sporks.given
import sporks.jvm.*

import durable.*
import durable.given

// format: off
object HelloWorld {

  def main(args: Array[String]): Unit = {

    val workflow = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.apply {
        ctx.log("Hello, ")
      }}.onComplete { SporkBuilder.apply { _ =>
        ctx.log("World!")
      }}
    }}

    val dService = DExecutionServices.synchronous("hello-world")
    // Observability.watch(dService)
    dService.execute(workflow)
  }
}
