package com.onzo.dynamodb

import java.math.MathContext
import java.util.UUID

import cats.Monad
import com.amazonaws.services.dynamodbv2.model.AttributeValue

import scala.collection.generic.CanBuildFrom
import scala.util.Try

trait Decoder[A] {
  self =>
  def apply(c: AttributeValue): A

  def map[B](f: A => B): Decoder[B] = new Decoder[B] {
    def apply(c: AttributeValue): B = f(self(c))
  }

  def flatMap[B](f: A => Decoder[B]): Decoder[B] = new Decoder[B] {
    def apply(c: AttributeValue): B = {
      f(self(c))(c)
    }
  }
}

object Decoder {
  def apply[A](implicit d: Decoder[A]): Decoder[A] = d

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

  implicit def decodeCanBuildFrom[A, C[_]](implicit
                                           d: Decoder[A],
                                           cbf: CanBuildFrom[Nothing, A, C[A]]
                                          ): Decoder[C[A]] = instance { c =>
    import scala.collection.JavaConversions._

    val list = c.getL
    val builder = cbf()
    for {
      e <- list
    } yield {
      builder += d(e)
    }
    builder.result()
  }

  implicit def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] = instance { c =>
    Try(d(c)).toOption
  }

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

  implicit val monadDecode: Monad[Decoder] = new Monad[Decoder] {
    def pure[A](a: A): Decoder[A] = instance(_ => a)

    def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
  }
}