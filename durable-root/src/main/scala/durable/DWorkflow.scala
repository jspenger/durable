package durable

import upickle.default.*

import spores.*
import spores.given

private[durable] case class DWorkflow(
    init: Spore[DExecutionContext ?=> Unit],
    retryPolicy: DRetryPolicy,
) derives ReadWriter

object DWorkflow:
  /** Create a new `DWorkflow` to be executed by a [[DExecutionService]].
    *
    * @param packed
    *   The packed function / Spore to be executed.
    * @return
    *   A new `DWorkflow`.
    *
    * @example
    *   {{{
    * val workflow = DWorkflow.apply { Spore.apply {
    *   DFuture.apply { Spore.apply {
    *     ctx.log("Hello, ")
    *   }}.onComplete { Spore.apply { _ =>
    *     ctx.log("World!")
    *   }}
    * }}
    *   }}}
    */
  def apply(spore: Spore[DExecutionContext ?=> Unit]): DWorkflow =
    DWorkflow.apply(
      spore,
      DRetryPolicy.default,
    )
