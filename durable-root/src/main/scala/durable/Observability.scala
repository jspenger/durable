package durable

object Observability:
  private def clearTerminal() =
    print("\u001b[2J")
    print("\u001b[H")

  private def pprint(dService: DExecutionService): Unit =
    println:
      DURABLE
    println:
      dService.status

  /** Periodically display the execution status on the terminal. */
  def watch(dService: DExecutionService): Unit =
    val thread = new Thread:

      override def run(): Unit =
        while !dService.isTerminated do
          clearTerminal()
          pprint(dService)
          Thread.sleep(100)

        clearTerminal()
        pprint(dService)

    thread.start()
end Observability
