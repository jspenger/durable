package durable

import upickle.default.*

import sporks.*
import sporks.given

private[durable] case class DWorkflow(
    init: Spork[DExecutionContext ?=> Unit],
    retryPolicy: DRetryPolicy,
) derives ReadWriter

object DWorkflow:
  /** Create a new `DWorkflow` to be executed by a [[DExecutionService]].
    *
    * @param packed
    *   The packed function / Spork to be executed.
    * @return
    *   A new `DWorkflow`.
    *
    * @example
    *   {{{
    * val workflow = DWorkflow.apply { Spork.apply {
    *   DFuture.apply { Spork.apply {
    *     ctx.log("Hello, ")
    *   }}.onComplete { Spork.apply { _ =>
    *     ctx.log("World!")
    *   }}
    * }}
    *   }}}
    */
  def apply(spork: Spork[DExecutionContext ?=> Unit]): DWorkflow =
    DWorkflow.apply(
      spork,
      DRetryPolicy.default,
    )
