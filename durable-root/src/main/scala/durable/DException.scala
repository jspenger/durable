package durable

/** Exception thrown when block execution fails. */
sealed trait DException extends Exception

case class DMaxRetriesException(cause: DFailure) extends DException
