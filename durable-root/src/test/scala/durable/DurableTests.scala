package durable

import scala.util.*

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Assert.*
import org.junit.Test

import upickle.default.*

import sporks.*
import sporks.given
import sporks.jvm.*

import durable.*
import durable.given
import durable.TestUtils.*

object DurableTests:
  val test = TestResult.apply[Int]

@RunWith(classOf[JUnit4])
class DurableTests:
  import DurableTests.*

  @Test
  def testWorkflow(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      test.complete(1)
    }}
    // format: on
    test.run(wf)
    assertEquals(1, test.read)

  @Test
  def testFuture(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.apply {
        test.complete(1)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(1, test.read)

  @Test
  def testFutureOnComplete(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.apply {
        ()
      }}.onComplete { SporkBuilder.apply { _ =>
        test.complete(2)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(2, test.read)

  @Test
  def testFutureMap(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.apply {
        1
      }}.map { SporkBuilder.apply { i =>
        test.complete(i + 1)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(2, test.read)

  @Test
  def testFutureFlatMap(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.apply {
        1
      }}.flatMap { SporkBuilder.apply { i =>
        DFuture.apply { SporkBuilder.applyWithEnv(i) { i =>
          test.complete(i + 1)
        }}
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(2, test.read)

  @Test
  def testFutureZip(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val f1 = DFuture.apply { SporkBuilder.apply {
        1
      }}
      val f2 = DFuture.apply { SporkBuilder.apply {
        2
      }}
      f1.zip(f2).map { SporkBuilder.apply { (i1, i2) =>
        test.complete(i1 + i2)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(3, test.read)

  @Test
  def testPromiseTryComplete(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val promise = DPromise.apply[Int]()
      promise.future.map { SporkBuilder.apply { i =>
        test.complete(i + 1)
      }}
      promise.tryComplete(Success(1))
    }}
    // format: on
    test.run(wf)
    assertEquals(2, test.read)

  @Test
  def testPromiseIsCompleted(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val promise = DPromise.apply[Int]()
      promise.future.onComplete { SporkBuilder.applyWithEnv(promise) { promise => i =>
        if promise.isCompleted && i.isSuccess then
          test.complete(1)
      }}
      promise.tryComplete(Success(1))
    }}
    // format: on
    test.run(wf)
    assertEquals(1, test.read)

  @Test
  def testPromiseIsCompletedFailure(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val promise = DPromise.apply[Int]()
      promise.future.onComplete { SporkBuilder.applyWithEnv(promise) { promise => i =>
        if promise.isCompleted && i.isFailure then
          test.complete(1)
      }}
      promise.tryComplete(Failure(new Exception("test")))
    }}
    // format: on
    test.run(wf)
    assertEquals(1, test.read)

  @Test
  def testFutureWithPromiseEnv(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val promise = DPromise.apply[Int]()
      DFuture.apply { SporkBuilder.applyWithEnv(promise) { promise =>
        promise.tryComplete(Success(1))
      }}
      promise.future.map { SporkBuilder.apply { i =>
        test.complete(i + 1)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(2, test.read)

  @Test
  def testFutureWithEnv(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.applyWithEnv(1) { i =>
        test.complete(i + 1)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(2, test.read)

  @Test
  def testFutureWithTuple2Env(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.applyWithEnv((1, 2)) { (i1, i2) =>
        test.complete(i1 + i2)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(3, test.read)

  @Test
  def testFutureWithTuple3Env(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      DFuture.apply { SporkBuilder.applyWithEnv((1, 2, 3)) { (i1, i2, i3) =>
        test.complete(i1 + i2 + i3)
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(6, test.read)

  @Test
  def testFutureWithFutureEnv(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val f1 = DFuture.apply { SporkBuilder.apply {
        1
      }}
      DFuture.apply { SporkBuilder.applyWithEnv(f1) { f1 =>
        f1.map { SporkBuilder.apply { i =>
          test.complete(i + 1)
        }}
      }}
    }}
    // format: on
    test.run(wf)
    assertEquals(2, test.read)

  @Test
  def testPromiseNested(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val p = DPromise.apply[Int]()
      p.tryComplete(Success(5))
      val spork = SporkBuilder.applyWithEnv[((Int, Int, DPromise[Int])), DExecutionContext ?=> Unit](1, 2, p) { (_, _, p) =>
        p.future.onComplete { SporkBuilder.apply { x => test.complete(x.get) }}
      }
      DFuture.apply { spork }
    }}
    // format: on
    test.run(wf)
    assertEquals(5, test.read)

  @Test
  def testPromiseNestedNested(): Unit =
    // format: off
    val wf = DWorkflow.apply { SporkBuilder.apply {
      val p = DPromise.apply[Int]()
      p.tryComplete(Success(5))
      val spork = SporkBuilder.applyWithEnv[((Int, Int, DPromise[Int])), DExecutionContext ?=> Unit](1, 2, p) { (_, _, p) =>
        p.future.onComplete { SporkBuilder.apply { x => test.complete(x.get) }}
      }
      val spork2 = SporkBuilder.applyWithEnv(spork) { spork => (_: DExecutionContext) ?=>
        DFuture.apply { spork }
      }
      DFuture.apply { spork2 }
    }}
    // format: on
    test.run(wf)
    assertEquals(5, test.read)
