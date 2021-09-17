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
         * Use the `ZStream.fromZIO` constructor to convert the provided effect
         * into a singleton stream.
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

object RunningStreams extends DefaultRunnableSpec {
  def spec =
    suite("RunningStreams") {

      /**
       * EXERCISE
       *
       * Use `.runHead` to pull out the head element of an infinite stream.
       */
      test("runHead") {
        val stream = ZStream("All work and no play makes Jack a dull boy").forever

        for {
          headOption <- ZIO(Option.empty[String])
        } yield assertTrue(headOption == Some("All work and no play makes Jack a dull boy"))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `.runDrain` to run a stream by draining all of its elements and
         * throwing them away (change the `.runHead`).
         */
        test("runDrain") {
          val stream = ZStream.fromIterable(0 to 1000)

          for {
            drained   <- Ref.make(false)
            _         <- (stream ++ ZStream.fromZIO(drained.set(true)).drain).runHead
            isDrained <- drained.get
          } yield assertTrue(isDrained)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `.runCount` to drain the stream and count how many things were
         * emitted by the stream (change the `.runHead`).
         */
        test("runCount") {
          val stream = ZStream.fromIterable(0 to 100)

          for {
            count <- stream.runHead.some
          } yield assertTrue(count == 101)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `.run` with the provided "sink" to count the number of things
         * that were emitted by the stream (change the `.runHead`).
         */
        test("run") {
          val stream = ZStream.fromIterable(0 to 100)
          val sink   = ZSink.count

          for {
            count <- stream.runHead.some
          } yield assertTrue(count == 101)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `.fold` to fold over the stream, summing all the elements.
         */
        test("fold") {
          val stream = ZStream.fromIterable(0 to 100)

          for {
            sum <- stream.runHead.some
          } yield assertTrue(sum == 5050)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `.foldZIO` to fold over a stream of questions, asking responses,
         * and aggregating them into a map.
         */
        test("foldZIO") {
          val expected =
            Map("What is your name?" -> "Sherlock Holmes", "What is your age?" -> "42")

          val questions = ZStream.fromIterable(expected.keys)

          for {
            _ <- TestConsole.feedLines(expected.values.toVector: _*)
            map <- questions.fold(Map.empty[String, String]) {
                    case (map, question) =>
                      val answer = question

                      map + (question -> answer)
                  }
          } yield assertTrue(map == expected)
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
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `mapAccum` to keep track of word counts, emitting pairs of
         * words and their current running counts. Hint: Use a `Map[String, Int]`
         * as the state type for your `mapAccum`.
         */
        test("mapAccum") {
          val stream = ZStream("blue", "red", "blue", "red")

          def aggregate(stream: Stream[Nothing, String]): Stream[Nothing, (String, Int)] =
            ???

          for {
            tuple <- aggregate(stream).runLast.some
          } yield assertTrue(tuple == ("red", 2))
        }
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

object TemporalStreams extends DefaultRunnableSpec {
  def spec =
    suite("TemporalStreams") {

      /**
       * EXERCISE
       *
       * Use `ZStream.fromSchedule` to convert a schedule to a stream.
       */
      test("fromSchedule") {
        val schedule = Schedule.recurs(100)

        for {
          values <- ZStream().runCollect
        } yield assertTrue(values.length == 100)
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Use `ZStream.repeatZIOWithSchedule` to repeat the provided effect
         * according to the provided schedule.
         */
        test("repeatZIOWithSchedule") {
          val effect   = Console.printLine("All work and no play makes Jack a dull boy")
          val schedule = Schedule.recurs(100)

          val s = ZStream()

          for {
            _     <- ZStream().runDrain
            lines <- TestConsole.output
          } yield assertTrue(lines.length == 101)
        } @@ ignore
    }
}

object ChunkedStreams extends DefaultRunnableSpec {
  def spec =
    suite("ChunkedStreams") {

      /**
       * EXERCISE
       *
       * Use `.foreachChunk` in order to iterate through all the chunks which
       * are backing the ZStream.
       */
      test("foreachChunk") {
        val stream = ZStream.fromIterable(1 to 100)

        for {
          chunkCount <- Ref.make(0)
          chunks     <- stream.foreach(_ => chunkCount.update(_ + 1))
          v          <- chunkCount.get
        } yield assertTrue(v == 1)
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Map over the chunks backing the provided stream with `mapChunks`,
         * reversing each of them.
         */
        test("mapChunks") {
          val stream = ZStream.fromChunks(Chunk(1), Chunk(2), Chunk(3, 4), Chunk(5, 6, 7, 8, 9))

          for {
            values <- stream.runCollect
          } yield assertTrue(values == Chunk(1, 2, 4, 3, 9, 8, 7, 6, 5))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Provide a correct implementation of `chunked` that exposes the chunks
         * underlying a stream.
         */
        test("chunked") {
          val stream = ZStream.fromChunks(Chunk(1), Chunk(2), Chunk(3, 4), Chunk(5, 6, 7, 8, 9))

          def chunked[R, E, A](stream: ZStream[R, E, A]): ZStream[R, E, Chunk[A]] =
            stream.map(c => Chunk(c))

          for {
            chunks <- chunked(stream).runCollect
          } yield assertTrue(chunks.length == 4)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Implement a correct version of `unchunked` that hides the chunks
         * into the stream.
         */
        test("unchunked") {
          val stream = ZStream(Chunk(1), Chunk(2), Chunk(3, 4), Chunk(5, 6, 7, 8, 9))

          def unchunked[R, E, A](stream: ZStream[R, E, Chunk[A]]): ZStream[R, E, A] =
            stream.map(_.head)

          for {
            values1 <- unchunked(stream).runCollect
            values2 <- stream.flattenChunks.runCollect
          } yield assertTrue(values1 == values2)
        } @@ ignore
    }
}

/**
 * GRADUATION
 *
 * To graduate from this section, you will implement a command-line application
 * that uses ZIO Streams to perform "word counting" on a provided file.
 */
object Graduation extends zio.App {
  def wordCount(file: String): IO[IOException, Map[String, Int]] = ???

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    args match {
      case Nil => Console.printLine("You need to specify a file to word count").!.as(ExitCode.failure)
      case file :: _ =>
        (for {
          map <- wordCount(file)
          _   <- Console.printLine(map.mkString("", "\n", ""))
        } yield ()).exitCode
    }
}
