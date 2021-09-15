/**
 * STREAMS
 *
 * ZIO Streams is an optional module of ZIO that provides support for async,
 * concurrent, high-performance streams that are built upon and tightly
 * integrated with ZIO, including embracing typed errors and environment.
 *
 * Although FS2 and other streaming libraries can be used with ZIO, ZIO
 * Streams offers a compelling package in a small surface area and is
 * considered extremely vital to the future of the ZIO ecosystem.
 */
package advancedzio.streams

import zio._
import zio.stream._

import zio.test._
import zio.test.TestAspect._
import zio.test.environment._

import java.io.IOException
import java.nio.file.Path
import java.nio.file.FileSystems

/**
 * ZIO Streams can be constructed from a huge number of other data types,
 * including iterables, ZIO effects, queues, hubs, input streams, and
 * much, much more.
 *
 * In this section, you'll explore a few of the common constructors.
 */
object SimpleConstructors extends DefaultRunnableSpec {
  def spec =
    suite("SimpleConstructors") {

      /**
       * EXERCISE
       *
       * Use the `ZStream.apply` constructor to make a stream from literal
       * integer values to make the unit test succeed.
       */
      test("apply") {
        for {
          ref    <- Ref.make(0)
          stream = ZStream[Int]()
          _      <- stream.foreach(value => ref.update(_ + value))
          v      <- ref.get
        } yield assertTrue(v == 15)
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Use the `ZStream.fromIterable` constructor to make a stream from the
         * `iterable` value to make the unit test succeed.
         */
        test("fromIterable") {
          val iterable: Iterable[Int] = List(1, 2, 3, 4, 5)

          for {
            ref    <- Ref.make(0)
            stream = ZStream[Int]()
            _      <- stream.foreach(value => ref.update(_ + value))
            v      <- ref.get
          } yield assertTrue(v == 15)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use the `ZStream.fromQueue` constructor to make a stream from the
         * `queue` to make the unit test succeed.
         */
        test("fromQueue") {
          for {
            ref    <- Ref.make(0)
            queue  <- Queue.bounded[Int](100)
            _      <- (ZIO.foreach(0 to 100)(queue.offer(_)) *> queue.size.repeatUntil(_ == 0) *> queue.shutdown).forkDaemon
            stream = ZStream[Int]()
            _      <- stream.foreach(value => ref.update(_ + value))
            v      <- ref.get
          } yield assertTrue(v == 5050)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use the
         */
        test("fromZIO") {
          val effect = Console.readLine

          for {
            ref    <- Ref.make("")
            _      <- TestConsole.feedLines("a", "b", "c")
            stream = ZStream[String]()
            _      <- stream.foreach(value => ref.update(_ + value))
            v      <- ref.get
          } yield assertTrue(v == "a")
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use the `ZStream.fromFile` method to read the "build.sbt" file.
         * Ignore the machinery that has NOT been introduced yet, such as
         * transduce.
         */
        test("fromFile") {
          lazy val path   = FileSystems.getDefault().getPath("build.sbt")
          lazy val decode = ZTransducer.utf8Decode >>> ZTransducer.splitLines

          for {
            _     <- ZStream[Byte]().transduce(decode).foreach(Console.printLine(_))
            lines <- TestConsole.output
          } yield assertTrue(lines.exists(_.contains("zio-streams")))
        } @@ ignore
    }
}

object SimpleOperators extends DefaultRunnableSpec {
  def spec =
    suite("SimpleOperators") {

      /**
       * EXERCISE
       *
       * Use `runCollect` on the provided stream to extract out all the values of the stream
       * and collect them into a `Chunk`.
       */
      test("runCollect") {
        val stream = ZStream(1, 2, 3, 4, 5)

        for {
          values <- (??? : UIO[Chunk[Int]])
        } yield assertTrue(values == Chunk(1, 2, 3, 4, 5))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `take(2)` at the right place to take the first two elements
         * of the stream.
         */
        test("take") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values == Chunk(1, 2))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `takeWhile(_ < 3)` at the right place to take the first two
         * elements of the stream.
         */
        test("takeWhile") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values == Chunk(1, 2))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `drop(2)` at the right place to drop the first two elements
         * of the stream.
         */
        test("drop") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values == Chunk(3, 4))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `dropWhile(_ < 3)` at the right place to drop the first
         * two elements of the stream.
         */
        test("dropWhile") {
          for {
            values <- ZStream(1, 2, 3, 4).runCollect
          } yield assertTrue(values == Chunk(3, 4))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `map(_ * 2)` at the right place.
         */
        test("map") {
          for {
            values <- ZStream(1, 2, 3).runCollect
          } yield assertTrue(values == Chunk(2, 4, 6))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `filter(_ % 2 == 0)` at the right place to filter out
         * all the odd numbers.
         */
        test("filter") {
          for {
            values <- ZStream(1, 2, 3, 4, 5, 6).runCollect
          } yield assertTrue(values == Chunk(2, 4, 6))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `.forever` call at the right place.
         */
        test("forever") {
          for {
            values <- ZStream(1).take(5).runCollect
          } yield assertTrue(values == Chunk(1, 1, 1, 1, 1))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `++` to concatenate the two streams together.
         */
        test("++") {
          val stream1 = Stream(1, 2, 3)
          val stream2 = Stream(4, 5, 6)

          for {
            values <- (stream1).runCollect
          } yield assertTrue(values == Chunk(1, 2, 3, 4, 5, 6))
        } @@ ignore
    }
}

object AdvancedConstructors extends DefaultRunnableSpec {
  def spec =
    suite("AdvancedConstructors") {

      /**
       * EXERCISE
       *
       * Use the ZStream.unfold constructor that can be used for statefully
       * "unfolding" a finite or infinite stream from an initial value.
       */
      test("unfold") {
        val fibs: ZStream[Any, Nothing, Int] = ZStream()

        for {
          values <- fibs.take(5).runCollect
        } yield assertTrue(values == Chunk(0, 1, 1, 2, 3))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `ZStream.repeatZIO` to construct a stream whose elements are
         * constructed by repeatedly executing the `Console.readLine` effect.
         */
        test("repeatZIO") {
          val stream = ZStream[String]()

          for {
            _      <- TestConsole.feedLines("Hello", "World")
            values <- stream.take(2).runCollect
          } yield assertTrue(values == Chunk("Hello", "World"))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `ZStream.repeatZIOOption` to repeat `Console.readLine` until
         * the line "John" is read from the console.
         */
        test("repeatZIOOption") {
          val readUntilJohn =
            for {
              line <- Console.readLine.mapError(Some(_))
              _    <- ZIO.fail(None).when(line == "John")
            } yield line

          val stream = ZStream[String]()

          for {
            _      <- TestConsole.feedLines("Sherlock", "Holmes", "John", "Watson")
            values <- stream.runCollect
          } yield assertTrue(values == Chunk("Sherlock", "Holmes"))
        } @@ ignore
    }
}

object AdvancedOperators extends DefaultRunnableSpec {
  def spec =
    suite("AdvancedOperators") {

      /**
       * EXERCISE
       *
       * Using `flatMap`, turn the provided stream into one where every element
       * is replicated 3 times.
       */
      test("flatMap") {
        val stream = Stream(1, 2, 3)

        for {
          values <- stream.runCollect
        } yield assertTrue(values == Chunk(1, 1, 1, 2, 2, 2, 3, 3, 3))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Insert a `.mapZIO` to print out each question using Console.printLine
         * and ask for a response using Console.readLine.
         */
        test("mapZIO") {
          val questions =
            ZStream("What is your name?", "What is your age?")

          for {
            _      <- TestConsole.feedLines("Sherlock Holmes", "42")
            values <- questions.runCollect
            lines  <- TestConsole.output
          } yield
            assertTrue(values == Chunk("Sherlock Holmes", "42")) &&
              assertTrue(lines == Vector("What is your name?\n", "What is your age?\n"))
        } @@ ignore
    }
}

object BasicError extends DefaultRunnableSpec {
  def spec =
    suite("BasicError") {

      /**
       * EXERCISE
       *
       * Use `ZStream.fail` to construct a stream that fails with the string
       * "Uh oh!".
       */
      test("fail") {
        for {
          value <- (ZStream(): Stream[String, Int]).runCollect.either
        } yield assertTrue(value == Left("Uh oh!"))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `.catchAll` to catch the error and turn it into a singleton stream.
         */
        test("catchAll") {
          for {
            value <- (ZStream.fail("Uh oh!"): Stream[String, String]).runCollect
          } yield assertTrue(value == Chunk("Uh oh!"))
        }
    }
}

object RunningStreams extends DefaultRunnableSpec {
  def spec =
    suite("RunningStreams")()
}

object TemporalStreams extends DefaultRunnableSpec {
  def spec =
    suite("TemporalStreams")()
}

object ChunkedStreams extends DefaultRunnableSpec {
  def spec =
    suite("ChunkedStreams")()
}
