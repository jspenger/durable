package durable

import upickle.default.*

import sporks.*
import sporks.given

/** Internal API. A durable block to be executed by the [[DExecutionService]].
  *
  * A block's Spork has one of three shapes.
  *   - 0: `DEX ?=> R`, when `dependencies` = `List.empty`.
  *   - 1: `DEX ?=> Try[T] => R`, when `dependencies` = `List(dep)` and the type
  *     of `dep` is `T`.
  *   - N: `DEX ?=> Try[T <: Tuple] => R`, when `dependencies` = `List(dep1,
  *     dep2, ...)` and the type of `Tuple(dep1, dep2, ...)` is T.
  *
  * All shapes produce a value of type `R`, which is the result of the block. In
  * case the execution was successful (no exceptions were thrown), the result is
  * wrapped in a `Success` and written to the `promise` of type `DPromise[R]`.
  *
  * @param uid
  *   unique identifier of the block
  * @param spork
  *   packed function to be executed
  * @param dependencies
  *   list of promises that this block depends on
  * @param promise
  *   promise produced by the block
  * @param retryPolicy
  *   used retry policy in execution
  */
private[durable] case class DBlock(
    uid: UID,
    spork: Spork[?],
    dependencies: List[DPromise[?]],
    promise: DPromise[?],
    retryPolicy: DRetryPolicy,
) derives ReadWriter
end DBlock
