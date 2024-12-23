package durable

/** System failures that may occur when running a block. */
sealed trait DFailure extends Throwable

case object DTimeoutFailure extends DFailure
