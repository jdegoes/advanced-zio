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

package domain {
  sealed trait OrderStatus 
  object OrderStatus {
    case object Created extends OrderStatus
    case object ChargeSuccess extends OrderStatus
    case object ChargeFailure extends OrderStatus
    case object Shipped extends OrderStatus
    case object Delivered extends OrderStatus
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
    case object InvalidSecurityCode extends Exception("Invalid security code")
    case object InvalidAddress extends Exception("Invalid address")
    case object FraudSuspected extends Exception("Suspected fraudulent transaction")
    case object InsufficientBalance extends Exception("Insufficient balance")
    final case class Unknown(message: String) extends Exception(message)
  }
  trait PaymentProcessor {
    def charge(paymentMethod: PaymentMethod, amount: Double): IO[PaymentProcessError, Unit]
  }

  /**
    * EXERCISE
    * 
    * Create a mock implementation of `PaymentProcessor`, together with a 
    * layer to describe its creation.
    */
  final case class PaymentProcessorLive()
}

package db {
  final case class DbConfig(jdbcUrl: String)

  trait Database {
    def query(query: String): ZManaged[Any, java.io.IOException, java.sql.ResultSet]
  }

  /**
    * EXERCISE
    * 
    * Create a mock implementation of `Database`, together with a 
    * layer to describe its creation.
    */
  final case class DatabaseLive()
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
  final case class OrderRepoLive()
}

package comms {
  trait Emailer {
    def sendEmail(subject: String, body: String, to: String, from: String): Task[Unit]
  }

  /**
    * EXERCISE
    * 
    * Create a mock implementation of `Emailer`, together with a 
    * layer to describe its creation.
    */
  final case class EmailerLive()
}

object core {
  import domain._ 

  /**
    * EXERCISE
    * 
    * Mock out an implementation of this method, which should attempt to 
    * verify the order is in a state to be charged, then attempt to charge 
    * the order, then update the state of the order, then send an email 
    * to the user indicating the order was successful. If the charge was 
    * not successful, then an error needs to be created to indicate why.
    */
  def placeOrder(order: Order) = ???
}