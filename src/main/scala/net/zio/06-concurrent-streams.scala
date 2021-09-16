/**
 * The full power of ZIO Streams is best observed in its rich support for
 * concurrent operations. Through a small number of operators, you can
 * construct highly concurrent streams that efficiently utilize
 */
package advancedzio.concurrentstreams

import zio._

import zio.test._
import zio.test.TestAspect._
import zio.test.environment._

object ConcurrencyOps extends DefaultRunnableSpec {
  def spec =
    suite("ConcurrentOps")()
}
