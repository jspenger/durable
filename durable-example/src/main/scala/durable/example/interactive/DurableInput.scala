package durable.example.interactive

import spores.default.*
import spores.default.given

import durable.*
import durable.given

object DurableInput:
  def inpt: Int =
    val input = scala.io.StdIn.readLine("Enter a number: ")
    try input.toInt
    catch
      case e: NumberFormatException =>
        println("Invalid input. Please enter a number.")
        inpt

  def step(str: String)(using DExecutionContext): DFuture[Unit] =
    // format: off
    DFuture.apply { Spore.applyWithEnv(str) { str =>
      val n = inpt
      val newStr = n.toString() + " :: " + str
      ctx.log("You entered: " + n)
      ctx.log("You've entered so far: " + newStr)
      step(newStr)
    }}
    // format: on

  def main(args: Array[String]): Unit =
    // format: off
    val workflow = DWorkflow { Spore.apply0 {
      step("nil")
    }}
    // format: on
    val service = DExecutionServices.synchronous("durable-input")
    service.execute(workflow)
    service.shutDown()
