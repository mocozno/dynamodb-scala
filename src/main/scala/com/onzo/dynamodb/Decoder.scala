package com.onzo.dynamodb

import java.math.MathContext
import java.util.UUID

import cats.Monad
import com.amazonaws.services.dynamodbv2.model.AttributeValue

import scala.collection.generic.CanBuildFrom
import scala.util.Try

//Decoder is a actually just a function AttributeValue => A
trait Decoder[A] extends (AttributeValue => A) {
  def apply(c: AttributeValue): A

  //A safer method that does not throw.
  def get(c: AttributeValue): Option[A] = Try(apply(c)).toOption

  def apply(name: String, items: Map[String, AttributeValue]): A = items.get(name) match {
    //I find pattern matching on Option to convey intent better than fold
    case Some(v) => apply(v)
    case None => throw new Exception(s"Attribute '$name' not found in '$items'")
  }

  def map[B](f: A => B): Decoder[B] = Decoder.instance(this andThen f)

  /* This flatMap consumes once the AttributeValue to create a new decoder, then decodes the same AttributeValue with the new decoder.
     TODO: Check if this is indeed useful for decoding AttributeValue
    */
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = Decoder.instance(c => this.andThen(f)(c)(c))
}

object Decoder {
  def apply[A](implicit d: Decoder[A]): Decoder[A] = d

  def instance[A](f: AttributeValue => A): Decoder[A] = new Decoder[A] {
    def apply(c: AttributeValue): A = f(c)
  }

  implicit val decodeAttributeValue: Decoder[AttributeValue] = instance(identity)
  implicit val decodeString: Decoder[String] = instance(_.getS)
  implicit val decodeBoolean: Decoder[Boolean] = instance(_.getBOOL)
  implicit val decodeFloat: Decoder[Float] = instance(_.getN.toFloat)
  implicit val decodeDouble: Decoder[Double] = instance(_.getN.toDouble)
  implicit val decodeByte: Decoder[Byte] = instance(_.getN.toByte)
  implicit val decodeShort: Decoder[Short] = instance(_.getN.toShort)
  implicit val decodeInt: Decoder[Int] = instance(_.getN.toInt)
  implicit val decodeLong: Decoder[Long] = instance(_.getN.toLong)
  implicit val decodeBigInt: Decoder[BigInt] = instance(a => BigDecimal(a.getN, MathContext.UNLIMITED).toBigInt())
  implicit val decodeBigDecimal: Decoder[BigDecimal] = instance(a => BigDecimal(a.getN, MathContext.UNLIMITED))
  implicit val decodeUUID: Decoder[UUID] = instance(a => UUID.fromString(a.getS))

  //I would prefer to get a concrete List / Vector back and convert to desired collection explicitly if needed.
  implicit def decodeCanBuildFrom[A, C[_]](implicit
                                           d: Decoder[A],
                                           cbf: CanBuildFrom[Nothing, A, C[A]]
                                          ): Decoder[C[A]] = instance { c =>
    import scala.collection.JavaConverters._
    val builder = cbf()
    val list = c.getL.asScala
    list.foreach(e => builder += d(e))
    builder.result()
  }
  //This adds different semantics to 2nd apply, since it makes it tolerant to both missing name and wrong conversion.
  //I would like to know if the name was not found or the conversion failed and treat them differently
  implicit def decodeOption[A](implicit d: Decoder[A]): Decoder[Option[A]] = new Decoder[Option[A]] {
    override def apply(c: AttributeValue): Option[A] = Try(d(c)).toOption
    override def apply(name: String, items: Map[String, AttributeValue]): Option[A] = {
      items.get(name).flatMap(apply)
    }
  }
  //CanBuildFrom for creating a sub-class of Map sounds very far-fetched. I would stick to direct conversion to Map.
  implicit def decodeMap[M[K, +V] <: Map[K, V], V](implicit
                                                   d: Decoder[V],
                                                   cbf: CanBuildFrom[Nothing, (String, V), M[String, V]]
                                                  ): Decoder[M[String, V]] = instance { c =>
    import scala.collection.JavaConverters._
    val builder = cbf()
    val map = c.getM.asScala
    map.foreach { case (k, v) => builder += k -> d(v) }
    builder.result()
  }
  /*
    Decoder is a Functor, but I am not sure if it should be a monad too. (Check comment on flatMap above)
    Consider using just this instead.
    implicit val functorDecoder: Functor[Decoder] = new Functor[Decoder] {
      override def map[A, B](fa: Decoder[A])(f: (A) => B): Decoder[B] = fa.map(f)
    }
  */
  implicit val monadDecode: Monad[Decoder] = new Monad[Decoder] {
    def pure[A](a: A): Decoder[A] = instance(_ => a)

    def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
  }

}