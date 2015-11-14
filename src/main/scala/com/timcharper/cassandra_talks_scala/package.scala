package com.timcharper

import akka.actor.{Props, Stash}
import akka.stream.scaladsl.Source
import com.datastax.driver.core.{PreparedStatement, RegularStatement, ResultSet, ResultSetFuture, Row, Session, Statement}
import com.google.common.util.concurrent.ListenableFuture
import scala.annotation.tailrec
import scala.concurrent.{CanAwait, Future}

private [timcharper] trait CassandraTalksScalaLowPriorityImplicits {
  /**
    Implicit class which adds the method `toScalaFuture` to a ListenableFuture[T].
    
    Lower priority; ResultSetFutureConversion takes precedence.
    */
  implicit class ListenableFutureConversion[T](future: ListenableFuture[T]) {
    def toScalaFuture: Future[T] = {
      new cassandra_talks_scala.ScalaListenableFuture[T, ListenableFuture[T]](future)
    }
  }
}

package object cassandra_talks_scala extends CassandraTalksScalaLowPriorityImplicits {

  import scala.concurrent.ExecutionContext


  // WARNING!!! Don't block inside of Runnable (Future) that uses this.
  private[cassandra_talks_scala] object SameThreadExecutionContext extends ExecutionContext {
    def execute(r: Runnable): Unit =
      r.run()
    override def reportFailure(t: Throwable): Unit =
      throw new IllegalStateException("problem in op_rabbit internal callback", t)
  }


  /**
    Implicit class which adds the method `toScalaFuture` to ResultSetFuture
    */
  implicit class ResultSetFutureConversion(future: ResultSetFuture) {
    def toScalaFuture: Future[ResultSet] = {
      new ScalaListenableFuture[ResultSet, ResultSetFuture](future, { _.getUninterruptibly })
    }
  }

  implicit class ScalaTalksSession(session: Session) {
      /**
     * Executes the provided query asynchronously.
     * <p>
     * This is a convenience method for {@code executeFuture(new SimpleStatement(query))}.
     *
     * @param query the CQL query to execute.
     * @return a future on the result of the query.
     */
    def executeFuture(query: String): Future[ResultSet] =
      session.executeAsync(query).toScalaFuture

    /**
     * Executes the provided query asynchronously using the provided values.
     *
     * This is a convenience method for {@code executeAsync(new SimpleStatement(query, values))}.
     *
     * @param query the CQL query to execute.
     * @param values values required for the execution of {@code query}. See
     * [[http://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/SimpleStatement.html#SimpleStatement-java.lang.String-java.lang.Object...- SimpleStatement#SimpleStatement(String, Object...) SimpleStatement]] for more detail.
     * @return a future on the result of the query.
     *
     * @throws [[http://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/exceptions/UnsupportedFeatureException.html UnsupportedFeatureException]] if version 1 of the protocol
     * is in use (i.e. if you've force version 1 through [[http://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/Cluster.Builder.html#withProtocolVersion-com.datastax.driver.core.ProtocolVersion- Cluster.Builder.withProtocolVersion]]
     * or you use Cassandra 1.2).
     */
    def executeFuture(query: String, values: Object*): Future[ResultSet] =
      session.executeAsync(query, values : _*).toScalaFuture

    /**
     * Executes the provided query asynchronously.
     *
     * This method does not block. It returns as soon as the query has been
     * passed to the underlying network stack. In particular, returning from
     * this method does not guarantee that the query is valid or has even been
     * submitted to a live node. Any exception pertaining to the failure of the
     * query will be thrown when accessing the Future[ [[http://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/ResultSet.html ResultSet]] ]
     * <p>
     * Note that for queries that doesn't return a result (INSERT, UPDATE and
     * DELETE), you will need to access the ResultSetFuture (that is call one of
     * its get method to make sure the query was successful.
     *
     * @param statement the CQL query to execute (that can be either any [[http://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/Statement.html Statement]].
     * @return a future on the result of the query.
     *
     * @throws [[http://docs.datastax.com/en/drivers/java/2.1/com/datastax/driver/core/exceptions/UnsupportedFeatureException.html UnsupportedFeatureException]] if the protocol version 1 is in use and
     * a feature not supported has been used. Features that are not supported by
     * the version protocol 1 include: BatchStatement, ResultSet paging and binary
     * values in RegularStatement.
     */
    def executeFuture(statement: Statement): Future[ResultSet] =
      session.executeAsync(statement).toScalaFuture

    def prepareFuture(statement: String): Future[PreparedStatement] =
      session.prepareAsync(statement).toScalaFuture

    def prepareFuture(statement: RegularStatement): Future[PreparedStatement] =
      session.prepareAsync(statement).toScalaFuture

    /**
      Like executeFuture, but returns a stream of Rows.
     
      Note - the query is re-executed each time the stream is run.
      */
    def executeStream(query: String): Source[Row, Unit] =
      ResultSetSource { () =>
        session.executeFuture(query)
      }

    /**
      Like executeFuture, but returns a stream of Rows.
     
      Note - the query is re-executed each time the stream is run.
      */
    def executeStream(query: String, args: Object*): Source[Row, Unit] =
      ResultSetSource { () =>
        executeFuture(query, args : _*)
      }

    /**
      Like executeFuture, but returns a stream of Rows.
     
      Note - the query is re-executed each time the stream is run.
      */
    def executeStream(statement: Statement): Source[Row, Unit] =
      ResultSetSource { () =>
        executeFuture(statement)
      }
  }
}
