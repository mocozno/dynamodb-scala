package com.onzo.dynamodb

import java.math.MathContext
import java.util.UUID

import cats.Monad
import com.amazonaws.services.dynamodbv2.model.AttributeValue

import scala.collection.generic.CanBuildFrom
import scala.util.Try

trait Decoder[A] {
  self =>

  // rename param name `c` of this public function to something meaningful
  def apply(c: AttributeValue): A

  /*
  I would remove this function. Pulling a value out of a `Map` is not a `Decoder` task.
  The user of this trait should pull the value out of the `Map` himself and use `apply(AttributeValue)`.
  */
  def apply(name: String, items: Map[String, AttributeValue]): A = {
    val vOpt = items.get(name)
    vOpt.fold(
      throw new Exception(s"Attribute '$name' not found in '$items'")
    )(
      v => apply(v)
    )
  }

  /*
  Transformation of a decoded value shouldn't be the concern of the Decoder
   */
  def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    def apply(c: AttributeValue): B = f(self(c))
  }

  /*
  Composition of Decoders seems odd. A fold over a sequence of Decoders would be more appropriate
   */
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    def apply(c: AttributeValue): B = {
      f(self(c))(c)
    }
  }
}

object Decoder {
  def apply[A](implicit d: Decoder[A]): Decoder[A] = d

  // should be probably private
  def instance[A](f: AttributeValue => A): Decoder[A] = new Decoder[A] {
    def apply(c: AttributeValue): A = f(c)
  }

  implicit val decodeAttributeValue: Decoder[AttributeValue] = instance(identity)
  implicit val decodeString: Decoder[String] = instance(_.getS)
  implicit val decodeBoolean: Decoder[Boolean] = instance(_.getBOOL)
  implicit val encodeFloat: Decoder[Float] = instance(_.getN.toFloat)
  implicit val encodeDouble: Decoder[Double] = instance(_.getN.toDouble)
  implicit val encodeByte: Decoder[Byte] = instance(_.getN.toByte)
  implicit val encodeShort: Decoder[Short] = instance(_.getN.toShort)
  implicit val encodeInt: Decoder[Int] = instance(_.getN.toInt)
  implicit val encodeLong: Decoder[Long] = instance(_.getN.toLong)
  implicit val encodeBigInt: Decoder[BigInt] = instance(a => BigDecimal(a.getN, MathContext.UNLIMITED).toBigInt())
  implicit val encodeBigDecimal: Decoder[BigDecimal] = instance(a => BigDecimal(a.getN, MathContext.UNLIMITED))
  implicit val encodeUUID: Decoder[UUID] = instance(a => UUID.fromString(a.getS))

  /*
  It should be possible to improve this implicit function to not use `Nothing` in the `CanBuildFrom`
   */
  implicit def decodeCanBuildFrom[A, C[_]](implicit
                                           d: Decoder[A],
                                           cbf: CanBuildFrom[Nothing, A, C[A]]
                                          ): Decoder[C[A]] = instance { c: AttributeValue =>
    import scala.collection.JavaConversions._

    val list = c.getL
    val builder = cbf()

    for(e <- list) {
      builder += d(e)
    }
    builder.result()
  }

  /*
   Conversion of a decoding error into an `Option`
   It should be possible to decode all stored values from the data store.
   If some values can't be restored from the database then there is a much deeper problem with the data model.
   */
  implicit def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]] {
    override def apply(c: AttributeValue): Option[A] = Try(d(c)).toOption
    override def apply(name: String, items: Map[String, AttributeValue]): Option[A] = {
      items.get(name).flatMap(apply)
    }
  }

  /*
  It should be possible to improve this implicit function to not use `Nothing` in the `CanBuildFrom`
   */
  /**
    * @group Decoding
    */
  implicit def decodeMap[M[K, +V] <: Map[K, V], V](implicit
                                                   d: Decoder[V],
                                                   cbf: CanBuildFrom[Nothing, (String, V), M[String, V]]
                                                  ): Decoder[M[String, V]] = instance { c =>
    import scala.collection.JavaConversions._
    val map = c.getM
    val builder = cbf()
    for {
      (k, v) <- map
    } yield {
      builder += k -> d(v)
    }
    builder.result()
  }

  /*
   Remove cats support. The main concern of this library is DynamoDB and it is sufficient to support the basic
   Scala types. Additional support for 3rd party libraries should be in a separate library, f.e. dynamodb-scala-cats
  */
  implicit val monadDecode: Monad[Decoder] = new Monad[Decoder] {
    def pure[A](a: A): Decoder[A] = instance(_ => a)

    def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
  }
}
