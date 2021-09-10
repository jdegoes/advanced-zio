/**
  * TESTING
  * 
  * To facilitate robust testing of ZIO applications, ZIO includes a testkit 
  * called _ZIO Test_. ZIO Test is a lightweight but rich testing environment
  * that seamlessly integrates with ZIO, providing access to all of ZIO's 
  * power from within unit tests and full testability for all ZIO services.
  * 
  * In this module, you will explore how you can use _ZIO Test_ to write 
  * powerful unit, integration, and system tests that ensure your ZIO 
  * applications perform correctly in production.
  */
package advancedzio.testing

import zio._
import zio.test._ 
import zio.test.TestAspect._ 
import zio.test.environment._

object SimplestSpec extends DefaultRunnableSpec {
  /**
    * EXERCISE 
    * 
    * Using sbt or your IDE, run `SpecBasics` by using its `main` function (not the test runner).
    */
  def spec = suite("SimplestSpec")()
}

object BasicAssertions extends DefaultRunnableSpec {
  def spec = suite("BasicAssertions") {
    trait Building {
      def contents: String
    }
    object House extends Building {
      def contents = "bed, coffee pot, kitchen"
    }
    object Barn extends Building {
      def contents = "hay, goats, feed"
    }
    object Shed extends Building {
      def contents = "needle, broom"
    }

    val buildings = List(House, Barn, Shed)

    test("2 + 2 == 4") {
      /**
        * EXERCISE
        * 
        * Using `assertTrue`, assert that 2 + 2 == 4.
        */
      assertTrue(false)
    } + 
    test("sherlock misspelling") {
      /**
        * EXERCISE
        * 
        * Examine the output of this failed test. Then fix the test so that it passes.
        */
      assertTrue("sherlock".contains("sure"))
    } + 
    test("multiple assertions") {
      val string = "cannac"

      /**
        * EXERCISE
        * 
        * Using the `&&` operator of `Assert`, verify the following properties 
        * about `string`:
        * 
        *  - The string is 6 letters in length 
        *  - the string starts with "can"
        *  - the reverse of the string is equal to itself
        */
      assertTrue(false)
    }
    /**
      * EXERCISE
      * 
      * Using `+`, add another test to the suite, which you can create with 
      * `test`, as above. This test should verify that the contents of one 
      * of the buildings in `buildings` contains a `needle`.
      */
  }
}

object BasicAssertionsZIO extends DefaultRunnableSpec {
  def spec = suite("BasicAssertionsZIO") {
    test("incrementing a ref") {
      /**
        * EXERCISE
        * 
        * Using `assertTrue`, assert that incrementing a zero-valued ref by one
        * results in 2.
        */
      for {
        ref <- Ref.make(0)
        v   <- ref.updateAndGet(_ + 1)
      } yield assertTrue(false)
    } + 
    test("multiple assertions") {
      /**
        * EXERCISE 
        * 
        * Using the `&&` operator of `Assert`, verify the following properties 
        * about `v`: 
        * 
        *  - It is an even number
        *  - It is greater than 0
        */
      for {
        ref <- Ref.make(0)
        rand <- Random.nextIntBetween(1, 4)
        v   <- ref.updateAndGet(_ + 1).repeatN(rand * 2)
      } yield assertTrue(false)
    }
  }
}

object BasicTestAspects extends DefaultRunnableSpec {
  import zio.test.TestAspect._ 

  def spec = suite("BasicTestAspects") {
    test("ignore") {
      /**
        * EXERCISE
        * 
        * Using `TestAspect.ignore`, add the `ignore` aspect to this test so that
        * the failure is ignored.
        */
      assertTrue(false)
    } + 
    test("flaky") {
      /**
        * EXERCISE
        * 
        * Using `TestAspect.flaky`, mark this test as flaky so that it will pass so 
        * long as it sometimes succeeds.
        */
      for {
        number <- Random.nextInt 
      } yield assertTrue(number % 2 == 0)
    } + 
    test("nonFlaky") {
      /**
        * EXERCISE 
        * 
        * Using `TestAspect.nonFlaky`, mark this test as non-flaky so that ZERO 
        * failures are permitted.
        */
      for {
        number <- Random.nextIntBetween(0, 100)
      } yield assertTrue(number * 2 % 2 == 0)
    }
  }
}

object TestFixtures extends DefaultRunnableSpec {
  val beforeRef = new java.util.concurrent.atomic.AtomicInteger(0)
  val aroundRef = new java.util.concurrent.atomic.AtomicInteger(0)


  val incBeforeRef: UIO[Any] = UIO(beforeRef.incrementAndGet())

  def spec = suite("TestFixtures") {
    /**
      * EXERCISE
      * 
      * Using `TestAspect.before`, ensure the `incBeforeRef` effect is executed
      * prior to the start of the test.
      */
    test("before") {
      for {
        value <- UIO(beforeRef.get)
      } yield assertTrue(value > 0)
    } + 
    /**
      * EXERCISE 
      * 
      * Using `TestAspect.after`, ensure the message `done with after` is printed
      * to the console using `ZIO.debug`.
      */
    test("after") {
      for {
        _ <- Console.printLine("after")
      } yield assertTrue(true)
    } + 
    /**
      * EXERCISE 
      * 
      * Using `TestAspect.around`, ensure the `aroundRef` is incremented before and 
      * decremented after the test.
      */
    test("around") {
      for {
        value <- UIO(aroundRef.get)
      } yield assertTrue(value == 1)
    }
  }
}


/**
  * By default, ZIO tests use test versions of all the standard services 
  * baked into ZIO, including Random, Clock, System, and Console.
  * These allow you to programmatically control the services, such as 
  * adjusting time, setting up fake environment variables, or inspecting
  * console output or providing console input.
  */
object TestServices extends DefaultRunnableSpec {
  def spec = 
    suite("TestServices") {
      /**
        * EXERCISE 
        * 
        * Using `TestClock.adjust`, ensure this test passes without timing out.
        */
      test("TestClock") {
        for {
          fiber <- Clock.sleep(1.second).as(42).fork 
          value <- fiber.join 
        } yield assertTrue(value == 42)
      } @@ ignore + 
      /**
        * EXERCISE
        * 
        * Using `TestSystem.setEnv`, set an environment variable to make the
        * test pass.
        */
      test("TestSystem") {
        for {
          name <- System.env("name").some 
        } yield assertTrue(name == "Sherlock Holmes")
      } @@ ignore +
      /**
       * EXERCISE 
       * 
       * Using `TestConsole.feedLines`, feed a name into the console such that 
       * the following test passes.
       */
      test("TestConsole") {
        for {
          _    <- Console.printLine("What is your name?")
          name <- Console.readLine 
        } yield assertTrue(name == "Sherlock Holmes")
      } @@ ignore
    }
}