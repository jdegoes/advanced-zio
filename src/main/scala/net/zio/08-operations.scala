/**
 * ZIO provides features specifically designed to improve your experience
 * deploying, scaling, monitoring, and troubleshooting ZIO applications.
 * These features include async stack traces, fiber dumps, logging hooks,
 * and integrated metrics and monitoring.
 *
 * In this section, you get to explore the operational side of ZIO.
 */
package advancedzio.ops

import zio._

import zio.test._
import zio.test.TestAspect._

object AsyncTraces extends ZIOSpecDefault {
  def spec =
    suite("AsyncTraces") {

      /**
       * EXERCISE
       *
       * Pull out the `traces` associated with the following sandboxed
       * failure, and verify there is at least one trace element.
       */
      test("traces") {
        def async =
          for {
            _ <- ZIO.sleep(1.millis)
            _ <- ZIO.fail("Uh oh!")
          } yield ()

        def traces(cause: Cause[String]): List[StackTrace] = ???

        Live.live(for {
          cause <- async.sandbox.flip
          ts    = traces(cause)
        } yield assertTrue(ts(0).stackTrace.length > 0))
      } @@ ignore
    }
}

object FiberDumps extends ZIOSpecDefault {
  def spec =
    suite("FiberDumps") {

      /**
       * EXERCISE
       *
       * Compute and print out all fiber dumps of the fibers running in this test.
       */
      test("dump") {
        val example =
          for {
            promise <- Promise.make[Nothing, Unit]
            blocked <- promise.await.forkDaemon
            child1  <- ZIO.foreach(1 to 100000)(_ => ZIO.unit).forkDaemon
          } yield ()

        for {
          supervisor <- Supervisor.track(false)
          _          <- example.supervised(supervisor)
          children   <- supervisor.value
          _          <- ZIO.foreach(children)(child => ZIO.unit)
        } yield assertTrue(children.length == 2)
      } @@ flaky
    }
}

object Logging extends ZIOSpecDefault {
  def spec =
    suite("Logging")()
}

object Metrics extends ZIOSpecDefault {
  def spec =
    suite("Metrics")()
}
