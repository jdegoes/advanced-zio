/**
 * INTERRUPTION
 *
 * ZIO is a functional framework for building highly-scalable, resilient,
 * cloud-native applications. The concurrency of ZIO is based on fibers,
 * which are freely and safely interruptible (unlike threads). The
 * interruptibility of ZIO fibers means that ZIO applications can be
 * globally efficient, performing no wasted computations in the presence
 * of errors, early termination, and timeouts.
 *
 * Yet, interruption can be one of the trickest concepts to grasp in ZIO,
 * because in other programming models, developers don't have to worry
 * about pre-emptive cancellation of their own logic.
 *
 * In this section, you will explore the intricacies of interruption and
 * learn how to master interruption, even when writing tricky code that
 * needs to modify the default interruption behavior.
 */
package advancedzio.interruption

import zio._
import zio.test._
import zio.test.TestAspect._
import zio.test.environment.Live
import scala.annotation.tailrec

object InterruptGuarantees extends DefaultRunnableSpec {
  def spec = suite("InterruptGuarantees") {
    test("ensuring") {

      /**
       * EXERCISE
       *
       * Learn about the guarantees of `ensuring` by making this test pass.
       */
      for {
        ref     <- Ref.make(0)
        latch   <- Promise.make[Nothing, Unit]
        promise <- Promise.make[Nothing, Unit]
        fiber   <- (latch.succeed(()) *> promise.await).ensuring(ref.update(_ + 1)).forkDaemon
        _       <- latch.await // await until fiber starts before interrupting
        _       <- fiber.interrupt
        v       <- ref.get
      } yield assertTrue(v == 0)
    } +
      test("onExit") {

        /**
         * EXERCISE
         *
         * Learn about the guarantees of `onExit` by verifying the `Exit` value is
         * interrupted.
         */
        for {
          latch   <- Promise.make[Nothing, Unit]
          promise <- Promise.make[Nothing, Exit[Nothing, Nothing]]
          fiber   <- (latch.succeed(()) *> ZIO.never).onExit(promise.succeed(_)).forkDaemon
          _       <- latch.await // await until fiber starts before interrupting
          _       <- fiber.interrupt
          exit    <- promise.await
        } yield assertTrue(false)
      } +
      test("acquireRelease") {

        /**
         * EXERCISE
         *
         * Learn about the guarantees of `acquireRelease` by making this test pass.
         *
         */
        for {
          latch <- Promise.make[Nothing, Unit]
          fiber <- (latch.succeed(()) *> ZIO.never).acquireRelease(ZIO.unit)(ZIO.succeed(42)).forkDaemon
          value <- latch.await *> Live.live(fiber.join.disconnect.timeout(1.second))
        } yield assertTrue(value == Some(42))
      }
  }
}

object InterruptibilityRegions extends DefaultRunnableSpec {
  def spec = suite("InterruptibilityRegions") {
    test("uninterruptible") {

      /**
       * EXERCISE
       *
       * Find the right location to insert `ZIO.uninterruptible` to make the
       * test succeed.
       */
      for {
        ref   <- Ref.make(0)
        latch <- Promise.make[Nothing, Unit]
        fiber <- (latch.succeed(()) *> Live.live(ZIO.sleep(10.millis)) *> ref.update(_ + 1)).forkDaemon
        _     <- latch.await *> fiber.interrupt
        value <- ref.get
      } yield assertTrue(value == 1)
    } +
      test("interruptible") {

        /**
         * EXERCISE
         *
         * Find the right location to insert `ZIO.interruptible` to make the test
         * succeed.
         */
        for {

          ref   <- Ref.make(0)
          latch <- Promise.make[Nothing, Unit]
          fiber <- (latch
                    .succeed(()) *> ZIO.never).ensuring(ref.update(_ + 1)).acquireRelease(ZIO.unit)(ZIO.unit).forkDaemon
          _     <- Live.live(latch.await *> fiber.interrupt.disconnect.timeout(1.second))
          value <- ref.get
        } yield assertTrue(value == 1)
      }
  }
}

/**
 * ZIO has resource-safe interruption, sometimes referred to as
 * "back-pressured" interruption. Interruption operators do not return
 * until whatever they are interrupting has been successfully interrupted.
 * This behavior minimizes the chance of leaking resources (including fibers),
 * but occassionally it is important to understand the implications of this
 * behavior and how to modify the default behavior.
 */
object Backpressuring extends DefaultRunnableSpec {
  def spec =
    suite("Backpressuring") {

      /**
       * EXERCISE
       *
       * This test looks like it should complete quickly. Discover what's
       * happening and change the condition until the test passes.
       */
      test("zipPar") {
        Live.live(for {
          start <- Clock.instant
          latch <- Promise.make[Nothing, Unit]
          left  = ZIO.uninterruptible(latch.succeed(()) *> UIO(Thread.sleep(10000)))
          right = latch.await *> ZIO.fail("Uh oh!")
          value <- left.zipPar(right).timeout(100.millis)
          end   <- Clock.instant
          delta = end.getEpochSecond() - start.getEpochSecond()
        } yield assertTrue(delta <= 1))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Find the appropriate place to add the `disconnect` operator to ensure
         * that even if an effect refuses to be interrupted in a timely fashion,
         * the fiber can be detatched and not delay interruption.
         */
        test("disconnect") {
          for {
            ref <- Ref.make(true)
            _   <- Live.live(((UIO(Thread.sleep(5000)) *> ref.set(false)).uninterruptible).timeout(10.millis))
            v   <- ref.get
          } yield assertTrue(v)
        } @@ ignore
    }
}
