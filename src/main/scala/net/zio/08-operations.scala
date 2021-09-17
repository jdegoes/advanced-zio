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

object FiberDumps extends DefaultRunnableSpec {
  def spec =
    suite("FiberDumps") {
      test("dump") {
        val example =
          for {
            child1 <- ZIO.foreach(1 to 100000)(_ => ZIO.unit).forkDaemon
            child2 <- ZIO.foreach(1 to 100000)(_ => ZIO.unit).forkDaemon
          } yield ()

        for {
          supervisor <- Supervisor.track(false)
          _          <- example.supervised(supervisor)
          children   <- supervisor.value
          dumps      <- ZIO.foreach(children)(child => child.dump)
          _          <- ZIO.foreach(dumps)(dump => dump.prettyPrintM.flatMap(Console.printLine(_)))
        } yield assertTrue(children.length == 2)
      }
    }
}
