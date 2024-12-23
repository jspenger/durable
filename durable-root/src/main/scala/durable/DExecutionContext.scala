package durable

/** Execution context for side-effecting operations. */
class DExecutionContext(
    private[durable] val service: DExecutionService
):
  def log(msg: String): Unit =
    service.logMsg(
      DLogMsg(
        service.freshUID(),
        service.timestamp(),
        msg
      )
    )
end DExecutionContext
