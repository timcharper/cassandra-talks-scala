package com.timcharper

import akka.actor.{Props, Stash}
import akka.stream.scaladsl.Source
import com.datastax.driver.core.{PreparedStatement, RegularStatement, ResultSet, ResultSetFuture, Row, Session, Statement}
import com.google.common.util.concurrent.ListenableFuture
import scala.annotation.tailrec
import scala.concurrent.{CanAwait, Future}

trait CassandraTalksScalaLowPriorityImplicits {
  implicit class ListenableFutureConversion[T](future: ListenableFuture[T]) {
    def toScalaFuture: Future[T] = {
      new cassandra_talks_scala.ScalaListenableFuture[T, ListenableFuture[T]](future)
    }
  }
}

package object cassandra_talks_scala extends CassandraTalksScalaLowPriorityImplicits {
  implicit class ResultSetFutureConversion(future: ResultSetFuture) {
    def toScalaFuture: Future[ResultSet] = {
      new ScalaListenableFuture[ResultSet, ResultSetFuture](future, { _.getUninterruptibly })
    }
  }

  implicit class ScalaTalksSession(session: Session) {
    def executeFuture(sql: String): Future[ResultSet] =
      session.executeAsync(sql).toScalaFuture

    def executeFuture(sql: String, args: Object*): Future[ResultSet] =
      session.executeAsync(sql, args : _*).toScalaFuture

    def executeFuture(statement: Statement): Future[ResultSet] =
      session.executeAsync(statement).toScalaFuture

    def prepareFuture(statement: String): Future[PreparedStatement] =
      session.prepareAsync(statement).toScalaFuture

    def prepareFuture(statement: RegularStatement): Future[PreparedStatement] =
      session.prepareAsync(statement).toScalaFuture

    def executeStream(sql: String): Source[Row, Unit] =
      Source.actorPublisher[Row](
        Props {
          new ResultSetProducer({ () =>
            session.executeAsync(sql).toScalaFuture
          })
        }).
        mapMaterializedValue(_ => ())

    def executeStream(sql: String, args: Object*): Source[Row, Unit] =
      Source.actorPublisher[Row](
        Props {
          new ResultSetProducer({ () =>
            session.executeAsync(sql, args : _*).toScalaFuture
          })
        }).
        mapMaterializedValue(_ => ())

    def executeStream(statement: Statement): Source[Row, Unit] =
      Source.actorPublisher[Row](
        Props {
          new ResultSetProducer({ () =>
            executeFuture(statement)
          })
        }).
        mapMaterializedValue(_ => ())
  }
}
