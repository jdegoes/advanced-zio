/**
 * ZIO can help structure your application according to best practices.
 * By breaking up your application into services, each of which interacts
 * with other services through interfaces, and whose implementations are
 * wired up through layers, ZIO can provide a robust, type-safe, and
 * testable scaffolding on which your application can grow and thrive.
 */
package advancedzio.architecture

import zio._

import java.io.IOException
import _root_.advancedzio.architecture.orders.OrderRepo
import _root_.advancedzio.architecture.payments.PaymentProcessor
import _root_.advancedzio.architecture.comms.Emailer
import _root_.advancedzio.architecture.comms.Email

package domain {
  sealed trait OrderStatus
  object OrderStatus {
    case object Created       extends OrderStatus
    case object ChargeSuccess extends OrderStatus
    case object ChargeFailure extends OrderStatus
    case object Shipped       extends OrderStatus
    case object Delivered     extends OrderStatus
  }
  final case class Id(value: Long)

  final case class Order(id: Id, name: String, items: List[OrderItem], customer: Customer, payment: PaymentMethod)

  final case class Customer(name: String, address: Address, email: String)

  final case class OrderItem(name: String, cost: Double)

  final case class Address(street: String, city: String, postalCode: String, country: String)

  sealed trait PaymentMethod
  object PaymentMethod {
    final case class CreditCard(number: String, expDate: String, name: String, address: Address)
  }
}

package payments {
  import domain._

  sealed trait PaymentProcessError
  object PaymentProcessError {
    case object InvalidSecurityCode           extends Exception("Invalid security code")
    case object InvalidAddress                extends Exception("Invalid address")
    case object FraudSuspected                extends Exception("Suspected fraudulent transaction")
    case object InsufficientBalance           extends Exception("Insufficient balance")
    final case class Unknown(message: String) extends Exception(message)
  }
  trait PaymentProcessor {
    def charge(paymentMethod: PaymentMethod, amount: Double): IO[PaymentProcessError, Unit]
  }
  
  object Util {
    type Acceptor = (PaymentMethod, Double) => IO[PaymentProcessError, Unit]
  }
  import Util._ 


  /**
   * EXERCISE
   *
   * Create a mock implementation of `PaymentProcessor`, together with a
   * layer to describe its creation.
   */
  final case class PaymentProcessorTest(acceptors: Ref[List[Acceptor]], history: Ref[List[(PaymentMethod, Double)]]) extends PaymentProcessor {
    def acceptNextCharge(f: Acceptor): UIO[Unit] = 
      acceptors.update(_ :+ f)

    def output: UIO[List[(PaymentMethod, Double)]] = history.get

    override def charge(paymentMethod: PaymentMethod, amount: Double): IO[PaymentProcessError, Unit] =
      for {
        _ <- history.update((paymentMethod, amount) :: _)
        nextF <- acceptors.modify {
          case h :: t => (h, t)
          case Nil => ((_ : PaymentMethod, _ : Double) => ZIO.unit, Nil)
        }
        _ <- nextF(paymentMethod, amount)
      } yield ()
  }
  object PaymentProcessorTest {
    val layer: ZLayer[Any, Nothing, PaymentProcessor] = 
      ZLayer {
        for {
          acceptors <- Ref.make(List.empty[Acceptor])
          history <- Ref.make(List.empty[(PaymentMethod, Double)])
        } yield PaymentProcessorTest(acceptors, history)
      }
  }
}

package db {

  import java.sql.ResultSet
  final case class DbConfig(jdbcUrl: String)

  trait Database {
    def query(query: String): ZIO[Scope, java.io.IOException, java.sql.ResultSet]
  }

  /**
   * EXERCISE
   *
   * Create a mock implementation of `Database`, together with a
   * layer to describe its creation.
   */
  final case class DatabaseTest(config: DbConfig, ref: Ref[Map[String, Either[IOException, ResultSet]]]) extends Database {
    def setResult(query: String, rs: Either[IOException, ResultSet]): UIO[Unit] = 
      ref.update(_ + (query -> rs))

    override def query(query: String): ZIO[Scope, IOException, ResultSet] = 
      ref.get.map(_.getOrElse(query, Left(new IOException("Query not found")))).absolve
  }
  object DatabaseTest {
    val layer: ZLayer[DbConfig, Nothing, Database] = 
      ZLayer {
        for {
          ref <- Ref.make(Map.empty[String, Either[IOException, ResultSet]])
          config <- ZIO.service[DbConfig]
        } yield DatabaseTest(config, ref)
      }
  }
}

package orders {
  import domain._

  trait OrderRepo {
    def getOrderById(id: Id): IO[IOException, Option[Order]]

    def updateOrder(order: Order): IO[IOException, Unit]
  }

  /**
   * EXERCISE
   *
   * Create a mock implementation of `OrderRepo`, together with a
   * layer to describe its creation.
   */
  final case class OrderRepoTest(ref: Ref[Map[Id, Order]]) extends OrderRepo {
    def update(id: Id, order: Order): UIO[Unit] = ref.update(_.updated(id, order)) 

    def getOrderById(id: Id): IO[IOException, Option[Order]] = 
      ref.get.map(_.get(id))

    def updateOrder(order: Order): IO[IOException, Unit] = 
      ref.update(_ + (order.id -> order))
  }
  object OrderRepoTest {
    val layer: ZLayer[Any, Nothing, OrderRepoTest] = 
      ZLayer {
        for {
          ref <- Ref.make(Map.empty[Id, Order])
        } yield OrderRepoTest(ref)
      }
  }
}

package comms {
  final case class Email(subject: String, body: String, to: String, from: String)
  trait Emailer {
    def sendEmail(email: Email): Task[Unit]
  }

  /**
   * EXERCISE
   *
   * Create a mock implementation of `Emailer`, together with a
   * layer to describe its creation.
   */
  final case class EmailerTest(ref: Ref[List[Email]]) extends Emailer {
    def sendEmail(email: Email): Task[Unit] = 
      ref.update(_ :+ email).unit
  }
  object EmailerTest {
    val layer: ZLayer[Any, Nothing, Emailer] = 
      ZLayer {
        for {
          ref <- Ref.make(List.empty[Email])
        } yield EmailerTest(ref)
      }
  }
}

object core {
  import domain._

  final case class OrderProcessError() extends Exception("Order processing error")
  trait OrderProcess {
    def placeOrder(order: Order): ZIO[Any, OrderProcessError, Unit] 
  }

  final case class OrderProcessLive(processor: PaymentProcessor, orderRepo: OrderRepo, emailer: Emailer) extends OrderProcess {
    /**
     * EXERCISE
     *
     * Mock out an implementation of this method, which should attempt to
     * verify the order is in a state to be charged, then attempt to charge
     * the order, then update the state of the order, then send an email
     * to the user indicating the order was successful. If the charge was
     * not successful, then an error needs to be created to indicate why.
     */
    def placeOrder(order: Order): ZIO[Any, OrderProcessError, Unit] = {
      def validateOrder(order: Order): ZIO[Any, OrderProcessError, Unit] = 
        ZIO.unit

      for {
        _      <- validateOrder(order)
        either <- processor.charge(order.payment, order.items.map(_.cost).sum).either
        email = either match {
                  case Left(err) => Email("Order failed", s"Order failed: ${err}", order.customer.email, "store@amazon.com")
                  case Right(_) => Email("Order successful", s"Order successful", order.customer.email, "store@amazon.com")
                }
      } yield ()
    }
  }
  object OrderProcess {
    val layer: ZLayer[PaymentProcessor & OrderRepo & Emailer, Nothing, OrderProcess] = 
      ZLayer {
        for {
          processor <- ZIO.service[PaymentProcessor]
          orderRepo <- ZIO.service[OrderRepo]
          emailer   <- ZIO.service[Emailer]
        } yield OrderProcessLive(processor, orderRepo, emailer)
      }
  }
}
