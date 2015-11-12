package com.timcharper.cassandra_talks_scala

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.{TimeUnit => JavaTimeUnit}
import scala.concurrent.{CanAwait, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Success, Try}

/**
  Wraps a ListenableFuture subtype.

  We receive a lambda for how to get the future result, because
  Cassandra ResultSetFuture has a special method to get the result
  with better exceptions.

  Shamelessly inspired by https://github.com/eigengo/activator-akka-cassandra/blob/master/src/main/scala/core/cassandra.scala
  */
private [timcharper] class ScalaListenableFuture[T, F <: ListenableFuture[T]](
  future: F,
  getResult: F => T = { f: F => f.get ()}) extends Future[T] {
  @throws(classOf[InterruptedException])
  @throws(classOf[scala.concurrent.TimeoutException])
  def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    future.get(atMost.toMillis, JavaTimeUnit.MILLISECONDS)
    this
  }

  @throws(classOf[Exception])
  def result(atMost: Duration)(implicit permit: CanAwait): T = {
    future.get(atMost.toMillis, JavaTimeUnit.MILLISECONDS)
  }

  def onComplete[U](func: (Try[T]) => U)(implicit executionContext: ExecutionContext): Unit = {
    if (future.isDone) {
      func(Success(getResult(future)))
    } else {
      future.addListener(
        new Runnable {
          def run(): Unit = {
            func(Try(future.get()))
          }
        },
        ExecutionContextExecutor(executionContext))
    }
  }

  def isCompleted: Boolean = future.isDone

  def value: Option[Try[T]] = if (future.isDone) Some(Try(future.get())) else None
}
