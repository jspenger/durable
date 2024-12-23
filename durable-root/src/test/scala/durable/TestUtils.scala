package durable

import scala.concurrent.*
import scala.util.*

object TestUtils:
  def name = System.currentTimeMillis().toString() + ":" + java.util.UUID.randomUUID().toString()

  class TestResult[T]:
    private var result = Promise.apply[T]

    def complete(value: T): Unit = result.success(value)

    def read: T = result.future.value match
      case Some(Success(value)) => value
      case _ => throw new Exception("No result available!")

    def clear(): Unit = result = Promise.apply[T]

    def run(workflow: DWorkflow): TestResult[T] =
      this.clear()
      val service = DExecutionServices.synchronous(name)
      Logger.setRootLevel(Logger.Level.ERROR)
      service.execute(workflow)
      service.shutDown()
      this
