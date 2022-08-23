/**
 * In ZIO Streams, Sinks are consumers of values. Not only do they consume
 * values, but they are free to produce remainders ("leftovers"), as well as
 * terminate with values of a given type (or error).
 *
 * Sinks are the duals of streams. While streams emit values, sinks absorb
 * them. To create a complete pipeline, a stream must be connected to a
 * sink, because values need a place to go. All the run methods of Stream
 * are actually implemented by connecting streams to sinks and running the
 * complete pipeline.
 */
package advancedzio.sinks

import zio._
import zio.stream._

import zio.test._
import zio.test.TestAspect._

object Constructors extends ZIOSpecDefault {
  def spec =
    suite("Constructors") {

      /**
       * EXERCISE
       *
       * Replace the call to `.runSum` by a call to `run` using `ZSink.count`.
       */
      test("count") {
        val stream = ZStream(1, 2, 3, 4)

        for {
          size <- stream.runSum
        } yield assertTrue(size.toInt == 4)
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Replace the call to `.runCollect` by a call to `run` using
         * `ZSink.take(2)`.
         */
        test("take") {
          val stream = ZStream(1, 2, 3, 4)

          for {
            two <- stream.runCollect
          } yield assertTrue(two == Chunk(1, 2))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Replace the call to `.runDrain` by a call to `run` using
         * `ZSink.foreach`, specifying a callback that increments the
         * ref by each value consumed by the sink.
         */
        test("foreach") {
          val stream = ZStream(1, 2, 3, 4)

          for {
            ref <- Ref.make(0)
            _   <- stream.runDrain
            v   <- ref.get
          } yield assertTrue(v == 10)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Replace the call to `.runSum` by a call to `run` using a
         * sink constructed with `ZSink.foldLeft` that multiplies all
         * consumed values together (producing their product).
         */
        test("foldLeft") {
          val stream = ZStream(1, 2, 3, 4)

          for {
            value <- stream.runSum
          } yield assertTrue(value == 24)
        } @@ ignore
    }
}

object Operators extends ZIOSpecDefault {
  def spec =
    suite("Operators") {

      /**
       * EXERCISE
       *
       * Use the `.map` method of the provided `sink` to map the collected
       * values into the length of the collected values. Then replace the
       * call to `.runCount` with a call to `.run(sink)`.
       */
      test("map") {
        val sink = ZSink.collectAll[Int].map(_.length)

        val stream = ZStream(1, 2, 3, 4)

        for {
          value <- stream.runCount
        } yield assertTrue(value == 4)
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `zipPar` to parallel zip `ZSink.count` and `ZSink.sum` together,
         * to produce a tuple of their outputs.
         */
        test("zipPar") {
          def zippedSink: ZSink[Any, Nothing, Int, Nothing, (Long, Int)] =
            ???

          val stream = ZStream(1, 2, 3, 4)

          for {
            value <- stream.run(zippedSink)
          } yield assertTrue(value == (4L, 10))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `zip` to zip `ZSink.take(3)` and `ZSink.collectAll[Int]` together,
         * to produce a tuple of their outputs.
         */
        test("zip") {
          def zippedSink: ZSink[Any, Nothing, Int, Int, (Chunk[Int], Chunk[Int])] =
            ???

          val stream = ZStream(1, 2, 3, 4)

          for {
            value <- stream.run(zippedSink)
          } yield assertTrue(value == (Chunk(1, 2, 3), Chunk(4)))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `ZSink.take` and `ZSink.collectAll` in the provided `for`
         * comprehension to make the test pass.
         */
        test("flatMap") {
          val sink =
            for {
              two       <- ZSink.collectAll[Int]
              three     <- ZSink.collectAll[Int]
              remainder <- ZSink.collectAll[Int]
            } yield (two, three, remainder)

          val stream = ZStream(1, 2, 3, 4, 5, 6)

          for {
            chunks <- stream.run(sink)
          } yield assertTrue(chunks == (Chunk(1, 2), Chunk(3, 4, 5), Chunk(6)))
        } @@ ignore
    }
}

/**
 * GRADUATION
 *
 * To graduate from this section, you will implement a sink that writes results
 * to a persistet queue.
 */
object Graduation extends ZIOSpecDefault {
  trait PersistentQueue[-A] {
    def append(a: A): Task[Unit]

    def shutdown: Task[Unit]
  }

  trait PersistentQueueFactory {
    def create[A](topic: String): Task[PersistentQueue[A]]
  }

  def persistentQueue[A](topic: String): ZSink[PersistentQueueFactory, Throwable, A, Nothing, Unit] =
    ???

  def spec =
    suite("Graduation") {
      test("persistentQueue") {
        assertTrue(false)
      } @@ ignore
    }
}
