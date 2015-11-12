package com.timcharper.cassandra_talks_scala

import akka.actor.Stash
import akka.stream.actor.ActorPublisher
import com.datastax.driver.core.{ResultSet, Row}
import scala.concurrent.Future

/**
  This is a Akka Stream producer, which sends async Cassandra results
  as soon as they are ready, allowing us to stream results from
  Cassandra using reactive streams and not blocking.
  */
private [this] class ResultSetProducer(runQuery: () => Future[ResultSet]) extends ActorPublisher[Row] with Stash {
  import akka.stream.actor.ActorPublisherMessage._
  import akka.pattern.pipe

  import context.dispatcher

  override def preStart(): Unit = {
    runQuery() pipeTo self
  }
  def receive = {
    case rs: ResultSet =>
      context.become(connected(rs))
      unstashAll()
    case _ =>
      stash()
  }

  def connected(resultSet: ResultSet): Receive = {
    case Request(_) =>
      while (totalDemand > 0 && resultSet.getAvailableWithoutFetching() > 0)
        onNext(resultSet.one())

      if (resultSet.isExhausted())
        context.stop(self)

      if (totalDemand > 0)
        resultSet.
          fetchMoreResults().
          toScalaFuture.
          onComplete { _ => self ! Request(totalDemand) }

    case Cancel =>
      context.stop(self)
  }
}
