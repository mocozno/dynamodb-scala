package com.onzo.dynamodb

import java.util.UUID

import cats.functor.Contravariant
import com.amazonaws.services.dynamodbv2.model.AttributeValue

import scala.collection.generic.IsTraversableOnce

trait Encoder[A] extends (A => AttributeValue){
  //TODO: Understand how this is used vs the 2nd apply
  def apply(a: A): AttributeValue

  //This is the main apply method that is used in the tests
  //TODO:Why not return (String, AttributeValue) or Option[(String, AttributeValue)]
  def apply(name: String, a: A): Map[String, AttributeValue] = Map(name -> apply(a))

  def contramap[B](f: B => A): Encoder[B] = Encoder.instance(b => apply(f(b)))
}

object Encoder {
  def apply[A](implicit e: Encoder[A]): Encoder[A] = e

  def instance[A](f: A => AttributeValue): Encoder[A] = new Encoder[A] {
    def apply(a: A): AttributeValue = f(a)
  }

  implicit def encodeTraversableOnce[A0, C[_]](implicit
                                               e: Encoder[A0],
                                               is: IsTraversableOnce[C[A0]] {type A = A0}
                                              ): Encoder[C[A0]] =
    instance { list =>
      val items = new java.util.ArrayList[AttributeValue]()
      is.conversion(list).foreach(a => items add e(a))
      new AttributeValue().withL(items)
    }

  implicit val encodeAttributeValue: Encoder[AttributeValue] = instance(identity)
  implicit val encodeString: Encoder[String] = instance(new AttributeValue().withS(_))
  implicit val encodeBoolean: Encoder[Boolean] = instance(new AttributeValue().withBOOL(_))
  implicit val encodeFloat: Encoder[Float] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeDouble: Encoder[Double] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeByte: Encoder[Byte] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeShort: Encoder[Short] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeInt: Encoder[Int] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeLong: Encoder[Long] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeBigInt: Encoder[BigInt] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeBigDecimal: Encoder[BigDecimal] = instance(a => new AttributeValue().withN(a.toString))
  implicit val encodeUUID: Encoder[UUID] = instance(uuid => new AttributeValue().withS(uuid.toString))

  //DynamoDB can encode null, but previous implementation was throwing for None. Not sure yet if this
  implicit def encodeOption[A](implicit e: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {
    //TODO: Check the way the DynamoDB api treats nulls.
    override def apply(a: Option[A]): AttributeValue = a.map(e).getOrElse(new AttributeValue().withNULL(true))
    //TODO: Check what it means to give back empty Map for None.
    override def apply(name: String, a: Option[A]): Map[String, AttributeValue] =
      a.map(v => Map(name -> e(v))).getOrElse(Map.empty)
  }

  implicit def encodeMapLike[M[K, +V] <: Map[K, V], V](implicit
                                                       e: Encoder[V]
                                                      ): Encoder[M[String, V]] = Encoder.instance { m =>
    //Intellij is strugling with overloaded apply methods so I added explicit type signature on map
    val map: Map[String, AttributeValue] = m.mapValues(e)
    import scala.collection.JavaConverters._
    new AttributeValue().withM(map.asJava)
  }
  // There is no Decoder[Either]. To decode to Either we need to know the key.
  // I would instead avoid Either and use sealed trait with explicit subtype name, or use the class name as key.
  def encodeEither[A, B](leftKey: String, rightKey: String)(implicit ea: Encoder[A], eb: Encoder[B]): Encoder[Either[A, B]] =
  instance { a =>
    val map = new java.util.HashMap[String, AttributeValue]()
    a.fold(
      a => map.put(leftKey, ea(a)),
      b => map.put(rightKey, eb(b)))
    new AttributeValue().withM(map)
  }

  implicit val contravariantEncode: Contravariant[Encoder] = new Contravariant[Encoder] {
    def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }

}
