package durable.example

import spores.*
import spores.given
import spores.jvm.*

import durable.*
import durable.given

// format: off
object Random {
  inline val SLEEP_TIME = 500

  inline def slowComputation: Int = { Thread.sleep(SLEEP_TIME); scala.util.Random.nextInt(100) }

  val workflow = DWorkflow.apply { Spore.apply {
      ctx.log("Starting workflow")

      val num1 = slowComputation

      val fut1 = DFuture.apply { Spore.apply {  
          slowComputation
      }}

      val fut2 = DFuture.apply { Spore.applyWithEnv(num1) { num1 =>
        num1 * slowComputation
      }}

      fut1.zip(fut2).map { Spore.apply { (f1, f2) =>
        ctx.log(s"f1: $f1")
        ctx.log(s"f2: $f2")
        ctx.log(s"Result: ${f1 * f2}")
      }}
    }}

  def main(args: Array[String]): Unit = {
    val name = "random" + scala.util.Random.nextInt(1000)
    val dService = DExecutionServices.synchronous(name)
    // Observability.watch(dService)
    dService.execute(workflow)
  }
}
