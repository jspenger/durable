package durable

import upickle.default.*

import sporks.*
import sporks.given

private[durable] case class DWorkflow(
    init: PackedSpork[DExecutionContext ?=> Unit],
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
    * val workflow = DWorkflow.apply { SporkBuilder.apply {
    *   DFuture.apply { SporkBuilder.apply {
    *     ctx.log("Hello, ")
    *   }}.onComplete { SporkBuilder.apply { _ =>
    *     ctx.log("World!")
    *   }}
    * }}
    *   }}}
    */
  def apply(spork: PackedSpork[DExecutionContext ?=> Unit]): DWorkflow =
    DWorkflow.apply(
      spork,
      DRetryPolicy.default,
    )
