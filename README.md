# Description

Simple integration library which enriches a Cassandra Java Driver Session so you can easily return Scala Futures and Akka Streams.

# Install

    libraryDependencies += "com.timcharper" %% "cassandra-talks-scala" % "0.2"

# Depends on

```
libraryDependencies := Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.9",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M1"
)
```

# Using

cassandra-talks-scala enriches `com.datastax.driver.core.Session` to contain Scala / Akka compatible methods; instead of `executeAsync`, run `executeFuture` or `executeStream`.

    import com.timcharper.cassandra_talks_scala._
    // ...
      // Get a Future[ResultSet]
      session.executeFuture("SELECT * FROM my_table")

      // Get a Akka Stream Source of Row.
      session.executeStream("SELECT * FROM my_table")

The `executeStream` method presently lacks a mechanism to determine the paging options. You can customize the ResultSet on your own by using ResultSetSource directly:

    import com.timcharper.cassandra_talks_scala._

    ResultSetSource { () =>
      session.executeFuture(statement).flatMap { resultSet =>
        // set paging options here...
        resultSet
      }
    }

Streaming backpressure causes the driver to fetch pages from Cassandra more slowly.
