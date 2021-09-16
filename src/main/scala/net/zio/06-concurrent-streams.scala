/**
 * The full power of ZIO Streams is best observed in its rich support for
 * concurrent operations. Through a small number of operators, you can
 * construct highly concurrent streams with minimal latency and high
 * parallelism.
 */
package advancedzio.concurrentstreams

import zio._

import zio.stream._

import zio.test._
import zio.test.TestAspect._
import zio.test.environment._

object ConcurrencyOps extends DefaultRunnableSpec {
  def spec =
    suite("ConcurrentOps") {

      /**
       * EXERCISE
       *
       * Insert a `.timeout(10.millis)` at the appropriate place to timeout the
       * infinite stream.
       */
      test("timeout") {
        val stream = ZStream(1).forever

        Live.live(for {
          size <- stream.runHead
        } yield assertTrue(size.isEmpty))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Find the right place to complete the promise that will interrupt the
         * provided infinite stream.
         */
        test("interruptWhen") {
          def makeStream(ref: Ref[Int]) = ZStream(1).tap(i => ref.update(_ + i)).forever

          Live.live(for {
            ref     <- Ref.make(0)
            done    <- Promise.make[Nothing, Unit]
            promise <- Promise.make[Nothing, Unit]
            _       <- makeStream(ref).interruptWhen(promise).ensuring(done.succeed(())).runDrain.forkDaemon
            _       <- (ref.get <* ZIO.yieldNow).repeatUntil(_ > 0)
            result  <- done.await.disconnect.timeout(1.second)
          } yield assertTrue(result.isDefined))
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Using `.merge`, perform a concurrent merge of two streams.
         */
        test("merge") {
          val stream1 = ZStream("1").forever
          val stream2 = ZStream("2").forever

          for {
            values <- stream1.take(10).runCollect
          } yield assertTrue(values.contains("1") && values.contains("2"))
        } @@ flaky @@ ignore +
        /**
         * EXERCISE
         *
         * Use `broadcast` to send one stream to 10 consumers, each of which is
         * created with the provided `consumer` function.
         */
        test("broadcast") {
          val stream = ZStream(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)

          def consumer(ref: Ref[Int], stream: ZStream[Any, Nothing, Int]) =
            stream.foreach(i => ref.update(_ + i))

          for {
            ref <- Ref.make(0)
            v   <- ref.get
          } yield assertTrue(v == 100)
        } @@ ignore
    }
}
