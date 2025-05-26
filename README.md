# Durable 🎄

<!-- [![Build Status](https://github.com/jspenger/durable/actions/workflows/build-test.yaml/badge.svg)](https://github.com/jspenger/durable/actions/workflows/build-test.yaml) -->
<!-- [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/jspenger/durable/blob/main/LICENSE) -->

Durable and fault tolerant computation library for Scala 3 with workflows and futures.

*Disclaimer: This is an early stage project. It is intended for experimentation and feedback on the API design, but has some known issues / limitations. It is an offspring of the [Portals project](https://github.com/portals-project).*

## Project Overview

Create and run durable and fault tolerant workflows with a simple API based on lambda functions / closures. Compose services with end-to-end exactly-once guarantees.

<details>
<summary>Fully executable example</summary>

```scala
import sporks.*
import sporks.given
import sporks.jvm.*

import durable.*
import durable.given

object Fibonacci {
  def fib(n: Int)(using DExecutionContext): DFuture[Int] = n match
    case 0 => DFuture { Spork.apply { 0 } }
    case 1 => DFuture { Spork.apply { 1 } }
    case _ =>
      fib(n - 1).flatMap { Spork.applyWithEnv(n) { n => n1 =>
        fib(n - 2).map { Spork.applyWithEnv(n1) { n1 => n2 =>
          n1 + n2
        }}
      }}

  def main(args: Array[String]): Unit = {
    val n = 10
    val workflow = DWorkflow { Spork.applyWithEnv(n) { n =>
      fib(n).onComplete { Spork.applyWithEnv(n) { n => result =>
        ctx.log("Completed result of fib(" + n + "): " + result)
      }}
    }}

    val service = DExecutionServices.synchronous("Fibonacci")
    // Observability.watch(service)
    service.execute(workflow)
    service.shutDown()
  }
}
```
</details>

A workflow can be created by using the `DWorkflow.apply` method, which takes a `Spork`, a serializable closure from the [Sporks3]((https://github.com/jspenger/sporks3)) library, as an argument for the initial block which starts the execution.

```scala
DWorkflow("my-workflow") { Spork.apply {
  DFuture.apply { Spork.apply {
    ctx.log("Hello, world!")
  }}
}}
```

The above program will run a workflow which consists of an initial block, the outer Spork.
The initial block will create a future, the inner Spork, which will be executed asynchronously and print "Hello, world!".

By using futures and promises, and common operations on them, it is possible to write complex workflows.
For example, this workflow computes the 10th Fibonacci number.
After successful execution, it prints "Completed result of fib(10): 55"

```scala
def fib(n: Int)(using DExecutionContext): DFuture[Int] = n match
  case 0 => DFuture { Spork.apply { 0 } }
  case 1 => DFuture { Spork.apply { 1 } }
  case _ =>
    fib(n - 1).flatMap { Spork.applyWithEnv(n) { n => n1 =>
      fib(n - 2).map { Spork.applyWithEnv(n1) { n1 => n2 =>
        n1 + n2
      }}
    }}

val n = 10
val workflow = DWorkflow { Spork.applyWithEnv(n) { n =>
  fib(n).onComplete { Spork.applyWithEnv(n) { n => result =>
    ctx.log("Completed result of fib(" + n + "): " + result)
  }}
}}
```

The example shows more interesting features of the durable library.
As mentioned, futures support common operations such as `map`, `flatMap`, and `onComplete`.
Additionally, the serializable closures can capture environment variables, which can be used to pass data between blocks.
Furthermore, note is that it is possible to write fully recursive workflows.
This is not typically possible in other durable workflow frameworks, as they are not based on closures.

If the execution is interrupted, for example, by failure or a system crash, it will resume from the last successfully executed block.
It is possible to periodically watch the execution state by using the `Observer.watch` method, which shows the ongoing state of the blocks and promises.

```scala
val service = DExecutionServices.synchronous("Fibonacci")
Observer.watch(service)
service.execute(workflow)
```

## Guarantees

The execution service guarantees to execute workflows such that it provides *exactly-once side effects*.

Workflows are composed of durable blocks, which are closures that are executed in an atomic and fault tolerant manner.
Each block is executed atomically: it may either *succeed* after one or many retries, or *fail* if the maximum number of retries is exceeded.
Note that a block cannot partially succeed or fail.
For example, a block that has only failed once, but not yet exceeded the maximum number of retries, *must* be retried until it either succeeds or exceeds the maximum number of retries.
This is also the case under system crashes or other failures.
A successful block execution will result in its side effects, such as all new blocks, new promises, and changes to existing promises, being produced exactly-once.
A failed execution, in contrast, will never produce any side effects.
In addition to this, each result of a block execution is written to a promise which contains the execution result either as the success value or the failure cause.

## Dependencies

This project depends on [Sporks3](https://github.com/jspenger/sporks3), which is not yet published to Maven Central.
It can be built and published locally by running the following commands:

```shell
git clone https://github.com/jspenger/sporks3.git
cd sporks3
sbt publishLocal
```

## Roadmap
- Add parallel execution runtime
- Add support for distributed futures / promises
