package durable

import upickle.default.*

sealed trait DRetryPolicy derives ReadWriter:
  def canRetry: Boolean
  def fail: DRetryPolicy

object DRetryPolicy:
  def default = Repeat(-1)

case class Repeat(maxRetries: Int, retry: Int = 0) extends DRetryPolicy derives ReadWriter:
  override def canRetry: Boolean = retry < maxRetries || maxRetries < 0
  override def fail: DRetryPolicy = Repeat(maxRetries, retry + 1)
