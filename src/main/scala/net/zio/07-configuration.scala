/**
 * ZIO's runtime system is responsible for executing effects on fibers.
 * It is one of the darkest parts of ZIO, which users seldom need to see,
 * and which some users never even know exists. However, understanding
 * the runtime system, and being able to tweak it, is the key to
 * unlocking more advanced ZIO integrations and customizing ZIO for the
 * needs of well-established applications.
 *
 * In this section, you will explore the runtime system and how to configure
 * ZIO to meet the specialized needs that more complex applications possess.
 */
package zio.advancedzio.config

import zio._
import zio.internal.Platform

import zio.test._
import zio.test.TestAspect._

import scala.concurrent.ExecutionContext
import java.util.concurrent.atomic.AtomicReference

object RuntimeSpec extends ZIOSpecDefault {
  implicit def unsafe: Unsafe = null.asInstanceOf[zio.Unsafe]

  def spec =
    suite("RuntimeSpec") {

      /**
       * EXERCISE
       *
       * zio.Runtime is a bundle of an environment and a zio.RuntimeConfig,
       * which determines how the runtime system executes effects.
       *
       * When you use `ZIOApp`, you get a runtime for free, which contains
       * ZIO's standard services and default configuration.
       *
       * However, you can construct your own runtime and customize it
       * according to your application's needs. This is especially
       * desirable when integrating ZIO into existing applications, or
       * when building very large and complex applications with ZIO.
       *
       * In this exercise, construct a custom runtime with a carefully
       * chosen environment such that this unit test passes.
       */
      test("Runtime") {
        val effect: ZIO[Int, Nothing, Int] =
          for {
            integer <- ZIO.environment[Int]
            _       <- ZIO.debug(integer)
          } yield integer.get[Int] * 2

        val runtime = Runtime(ZEnvironment(19), FiberRefs.empty, RuntimeFlags.default)

        assertTrue(runtime.unsafe.run(effect) == Exit.succeed(42))
      } @@ ignore +
        /**
         * EXERCISE
         *
         * Anytime a fiber crashes with a catastrophic error (such stack overflow error),
         * the signal is sent to the `reportFatal` handler inside the runtime config.
         *
         * It is expected, indeed required, that the `reportFatal` handler either re-throw
         * the exception, or exit the JVM, either of which causes the fiber to immediately
         * stop executing and skip all finalization.
         *
         * Modify the report fatal handler to store the fatal error inside `fatalRef`,
         * by using the helper function `captureFatal`, so the test will pass.
         */
        test("reportFatal") {
          val fatalError = new StackOverflowError("Uh oh!")

          val fatalRef = new java.util.concurrent.atomic.AtomicReference[Option[Throwable]](None)

          def captureFatal(t: Throwable): Nothing = {
            fatalRef.set(Some(t))
            throw t
          }

          val runtime = Runtime(ZEnvironment.empty, FiberRefs.empty, RuntimeFlags.default)

          try runtime.unsafe.run(ZIO.succeed(throw fatalError))
          catch { case _: Throwable => () }

          assertTrue(fatalRef.get.get == fatalError)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * Some advanced functionality, such as converting a `FiberRef` into a JVM
         * `ThreadLocal`, requires that the `enableCurrentFiber` flag be set to
         * true inside a Runtime config. This imposes a performance penalty on
         * all fiber-based code. In many applications, especially those with long-
         * lived fibers, this will not be noticeable, but in some applications,
         * the difference can be measured.
         *
         * Create a Runtime where the current fiber is enabled so the following
         * unit test will pass.
         */
        test("enableCurrentFiber") {
          val runtime = Runtime(ZEnvironment.empty, FiberRefs.empty, RuntimeFlags.default)

          val option = runtime.unsafe.run(ZIO.attempt(Fiber.currentFiber()(unsafe))).getOrThrowFiberFailure()

          assertTrue(option.isDefined)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * ZIO executes all fibers using an `Executor`, which is a ZIO-defined
         * facade over task execution. Executors can be created from many sources,
         * including Java's executor services or Scala's execution context.
         *
         * Ordinarily, there is no need to tamper with ZIO's default executor,
         * since it uses a highly-optimiized, fiber-aware scheduler. However, for
         * some applications, it can be desirable to swap out the default
         * executor for one that an existing application is already using.
         *
         * In this case, the runtime configuration can be overriden to specify
         * the new executor.
         *
         * In this exercise, create a runtime that uses an Executor created from
         * Scala's global ExecutionContext.
         */
        test("executor") {
          import scala.concurrent.ExecutionContext.Implicits.global

          val ranOnEC = new java.util.concurrent.atomic.AtomicBoolean(false)

          val executor = zio.Executor.fromExecutionContext {
            new ExecutionContext {
              final def execute(runnable: Runnable): Unit = {
                ranOnEC.set(true)

                global.execute(runnable)
              }

              final def reportFailure(cause: Throwable): Unit = global.reportFailure(cause)
            }
          }

          val runtime = Runtime(ZEnvironment.empty, FiberRefs.empty, RuntimeFlags.default)

          runtime.unsafe.run(ZIO.yieldNow *> ZIO.unit)

          assertTrue(ranOnEC.get == true)
        } @@ ignore +
        /**
         * EXERCISE
         *
         * ZIO has special behavior for defects (thrown exceptions) that are
         * deemed fatal (_catastrophic_). Fatal errors are so severe that
         * ZIO does not attempt to continue executing in the presence of them.
         * Rather, it calls a special handler for fatal errors, which would
         * typically log them as quickly as possible without allocating memory
         * (since one class of fatal error is out of memory exception). Doing
         * anything too extravagant would result in yet another fatal error,
         * while handling a previous fatal error, so logging in a low-cost way is
         * essential to help diagnosing such fatal errors.
         *
         * Ordinarily, you don't have to worry about fatal errors, since by
         * default, ZIO considers subtypes of VirtualMachineError to be fatal.
         * However, some applications may wish to consider other errors fatal.
         * In fact, some applications may wish to consider any errors fatal,
         * if they are trying to strictly enforce the maxim that no pure code
         * should raise exceptions. To facilitate these behaviors, you can
         * override the `isFatal` field of runtime configuration.
         *
         * In this exercise, modify the `isFatal` field to consider all
         * defects as fatal, thus ensuring developers do not throw exceptions
         * from pure code.
         */
        test("isFatal") {
          val ioException = new java.io.IOException("Cannot read some bytes from somewhere")

          val fatalRef = new java.util.concurrent.atomic.AtomicReference[Option[Throwable]](None)

          def captureFatal(t: Throwable): Nothing = {
            fatalRef.set(Some(t))
            throw t
          }

          Runtime.default.unsafe.run {
            ZIO.succeed(throw ioException) // HERE
          }

          assertTrue(fatalRef.get.get == ioException)
        }
    } @@ jvmOnly @@ ignore +
      /**
       * EXERCISE
       *
       * ZIO's runtime configuration allows specification of a supervisor, which
       * is an incredibly powerful hook into the runtime system. Supervisors
       * have direct access into the lifecycle of all fibers launched by the
       * runtime. This allows them to peek into executing fibers, track metrics,
       * record timings, restart failed effects, or perform other advanced
       * functionality.
       *
       * NOTE: currently, all supervisors must live inside the ZIO package.
       *
       * In this exercise, install a supervisor into the runtime configuration
       * that allows you to restart all failed fibers.
       */
      test("supervisor") {
        val restartCount = new java.util.concurrent.atomic.AtomicInteger(0)
        val succeeded    = new java.util.concurrent.atomic.AtomicBoolean(false)

        val effect =
          for {
            count <- ZIO.succeed(restartCount.getAndIncrement())
            _ <- if (count <= 0) ZIO.dieMessage("Not time to live!")
                else ZIO.succeed("Time to live!") *> ZIO.succeed(succeeded.set(true))
          } yield count

        lazy val supervisor: Supervisor[Unit] = new Supervisor[Unit] {
          @volatile var lastEffect = ZIO.unit

          def value(implicit trace: zio.Trace): UIO[Unit] = ZIO.unit

          def onStart[R, E, A](
            environment: ZEnvironment[R],
            effect: ZIO[R, E, A],
            parent: Option[Fiber.Runtime[Any, Any]],
            fiber: Fiber.Runtime[E, A]
          )(implicit unsafe: zio.Unsafe): Unit = lastEffect = effect.asInstanceOf[UIO[Unit]]

          def onEnd[R, E, A](value: Exit[E, A], fiber: Fiber.Runtime[E, A])(implicit unsafe: zio.Unsafe): Unit =
            value.fold(_ => Runtime.default.unsafe.run(lastEffect), _ => ())
        }

        try Runtime.default.unsafe.run {
          effect // HERE
        } catch { case _: Throwable => () }

        assertTrue(succeeded.get)
      } @@ ignore
}
