package com.timcharper.cassandra_talks_scala

import akka.actor.Props
import akka.actor.Stash
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import com.datastax.driver.core.{ResultSet, Row}
import scala.concurrent.Future

/**
  This is a Akka Stream producer, which sends async Cassandra results
  as soon as they are ready, allowing us to stream results from
  Cassandra using reactive streams and not block.
  
  See [[ResultSetProducer$apply]]
  */
private [this] class ResultSetSource(runQuery: Future[ResultSet]) extends ActorPublisher[Row] with Stash {
  import akka.stream.actor.ActorPublisherMessage._
  import akka.pattern.pipe

  import context.dispatcher

  override def preStart(): Unit = {
    runQuery pipeTo self
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
      else if (totalDemand > 0)
        resultSet.
          fetchMoreResults().
          toScalaFuture.
          onComplete { _ => self ! Request(totalDemand) }

    case Cancel =>
      context.stop(self)
  }
}

object ResultSetSource {
  /**
    Accepts a Function0 that returns a Future[ResultSet].

    Why? Because a stream Source can be run multiple times, and it
    would be disasterous if they shared a reference.
    */
  def apply(runQuery: () => Future[ResultSet]): Source[Row, Unit] =
    Source.actorPublisher[Row](
      Props {
        new ResultSetSource(runQuery())
      }).
      mapMaterializedValue(_ => ())

  /**
    Returns a Source that is only materializable once; future attempts
    at materialization fail the stream.
    
    Note, the ResultSet is a mutable object whose mutability will be
    owned by the Stream. You should leave it alone once you hand it
    off!
    */
  def apply(results: Future[ResultSet]): Source[Row, Unit] = {
    var materialized = false
    apply { () =>
      results.synchronized {
        if (materialized)
          throw new RuntimeException("Cowardly refusing to stream the same Cassandra ResultSet twice.")
        materialized = true
      }
      results
    }
  }
}
