/**
 * In ZIO Stream, transducers transform 1 or more values of one type to 1
 * or more values of another type. They are typically used for encoding,
 * decoding, compression, decompression, rechunking, encryption,
 * decryption, and other similar element transformations.
 */
package advancedzio.transducers

import zio._
import zio.stream._

import zio.test._
import zio.test.TestAspect._
import zio.test.environment._
import java.nio.charset.StandardCharsets

object Introduction extends DefaultRunnableSpec {
  def spec =
    suite("Introduction") {

      /**
       * EXERCISE
       *
       * Use `>>>` to append a transducer to a stream, transforming the
       * elements of the stream by the transducer.
       */
      test(">>>") {
        val stream = ZStream.range(1, 10)
        val trans  = ZTransducer.collectAllN[Int](2)

        for {
          chunks <- stream.runCollect
        } yield assertTrue(chunks.length == 5)
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Using `.transduce`, transform the elements of this stream by
         * the provided transducer.
         */
        test("transduce") {
          val stream = ZStream.range(1, 100)
          val trans  = ZTransducer.collectAllN[Int](10)

          for {
            values <- stream.runCollect
          } yield assertTrue(values.length == 10)
        } @@ ignore
    }
}

object Constructors extends DefaultRunnableSpec {
  def spec =
    suite("Constructors") {

      /**
       * EXERCISE
       *
       * Using `ZTransducer.splitLines` transform the stream elements so
       * they are split on newlines.
       */
      test("splitLines") {
        val stream = ZStream("a\nb\nc\n", "d\ne")

        for {
          values <- (stream).runCollect
        } yield assertTrue(values == Chunk("a", "b", "c", "d", "e"))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Using `ZTransducer.splitOn(",")`, transform the stream elements so
         * they are split on newlines.
         */
        test("splitOn") {
          val stream = ZStream("name,age,add", "ress,dob,gender")

          for {
            values <- (stream).runCollect
          } yield assertTrue(values == Chunk("name", "age", "address", "dob", "gender"))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Using `ZTransducer.utf8Decode`, transform the stream elements so
         * they are UTF8 decoded.
         */
        test("utf8Decode") {
          val bytes1 = "Hello".getBytes(StandardCharsets.UTF_8)
          val bytes2 = "World!".getBytes(StandardCharsets.UTF_8)

          val stream = ZStream.fromChunks(Chunk.fromArray(bytes1), Chunk.fromArray(bytes2))

          def decodedStream: ZStream[Any, Nothing, String] =
            stream >>> ???

          for {
            values <- decodedStream.runCollect
          } yield assertTrue(values == Chunk("Hello", "World!"))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Using `ZTransducer.fromFunction`, create a transducer that converts
         * strings to ints.
         */
        test("fromFunction") {
          def parseInt: ZTransducer[Any, Nothing, String, Int] =
            ???

          val stream = ZStream("1", "2", "3")

          for {
            values <- (stream >>> parseInt).runCollect
          } yield assertTrue(values == Chunk(1, 2, 3))
        } @@ ignore
    }
}

object Operators extends DefaultRunnableSpec {
  def spec =
    suite("Operators") {

      /**
       * EXERCISE
       *
       * Use `>>>` to compose `utf8Encode` and `utf8Decode`, and verify
       * this composition doesn't change the string stream.
       */
      test(">>>") {
        import ZTransducer.utf8Decode

        def utf8Encode: ZTransducer[Any, Nothing, String, Byte] =
          ZTransducer
            .fromFunction[String, Chunk[Byte]] { string =>
              Chunk.fromArray(string.getBytes(StandardCharsets.UTF_8))
            }
            .mapChunks(_.flatten)

        def composed = utf8Encode >>> utf8Decode

        val chunk = Chunk("All", "Work", "And", "No", "Play", "Makes", "Jack", "A", "Dull", "Boy")

        val stream = ZStream.fromChunk(chunk)

        for {
          values <- (stream >>> composed).runCollect
        } yield assertTrue(values == chunk)
      } @@ ignore
    }
}

/**
 * GRADUATION
 *
 * To graduate from this section, you will implement a transducer that rechunks
 * a stream.
 */
object Graduation extends DefaultRunnableSpec {
  def rechunkWith[A](f: (Chunk[A], Chunk[A]) => (Chunk[A], Chunk[A])): ZTransducer[Any, Nothing, A, A] =
    ???

  def rechunk[A](n: Int): ZTransducer[Any, Nothing, A, A] =
    rechunkWith {
      case (leftover, next) =>
        (leftover ++ next).splitAt(n + 1)
    }

  def spec =
    suite("Graduation") {
      test("rechunking") {
        val stream = ZStream.fromChunks(Chunk(1), Chunk(2, 3, 4), Chunk(5), Chunk(6, 7, 8))

        for {
          values <- (stream >>> rechunk[Int](2)).mapChunks(c => Chunk(c)).runCollect
        } yield assertTrue(values == Chunk(Chunk(1, 2), Chunk(3, 4), Chunk(5, 6)))
      } @@ ignore
    }
}
