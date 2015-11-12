package com.timcharper.cassandra_talks_scala

import scala.concurrent.ExecutionContext

/**
  This code wraps the Java Cassandra driver to make it as Scala
  friendly as possible:

  - Unify custom Future types to ScalaFutures
  - Helper methods to bind and exec a param, returning either a Scala
    Future or a Akka Stream Source.
  */
private case class ExecutionContextExecutor(executionContext: ExecutionContext) extends java.util.concurrent.Executor {
  def execute(command: Runnable): Unit = { executionContext.execute(command) }
}
