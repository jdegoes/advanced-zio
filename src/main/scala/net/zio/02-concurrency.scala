package advancedzio.concurrency

import zio._
import zio.stm._ 
import zio.test._ 
import zio.test.environment.Live 

object StmBasics extends DefaultRunnableSpec {
  def spec = 
    suite("StmBasics") {
      test("permits") {
        /**
          * EXERCISE
          * 
          * Implement `acquire` and `release` in a fashion the test passes.
          */
        final case class Permits(ref: TRef[Int]) {
          def acquire(howMany: Int): UIO[Unit] = ??? 

          def release(howMany: Int): UIO[Unit] = ???
        }

        def makePermits(max: Int): UIO[Permits] = TRef.make(max).map(Permits(_)).commit

        for {
          counter <- Ref.make(0)
          permits <- makePermits(100)
          _       <- ZIO.foreachPar(1 to 1000)(_ => Random.nextIntBetween(1, 2).flatMap(n => permits.acquire(n) *> permits.release(n)))
          latch   <- Promise.make[Nothing, Unit]
          fiber   <- (latch.succeed(()) *> permits.acquire(101) *> counter.set(1)).forkDaemon
          _       <- latch.await 
          _       <- Live.live(ZIO.sleep(1.second))
          _       <- fiber.interrupt
          count   <- counter.get 
          permits <- permits.ref.get.commit
        } yield assertTrue(count == 0 && permits == 100)
      }
    }
}