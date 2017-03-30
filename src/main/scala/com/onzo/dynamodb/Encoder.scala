package com.onzo.dynamodb

import java.util.UUID
import scala.collection.generic.IsTraversableOnce

import cats.functor.Contravariant
import com.amazonaws.services.dynamodbv2.model.AttributeValue

trait Encoder[A] {

  def apply(a: A): AttributeValue

  def apply(name: String, a: A): Map[String, AttributeValue] = {
    Map(name -> apply(a))
  }

  def contramap[B](f: B => A): Encoder[B] = Encoder.instance(b => apply(f(b)))
}

object Encoder {
  def apply[A](implicit e: Encoder[A]): Encoder[A] = e

  def instance[A](f: A => AttributeValue): Encoder[A] = new Encoder[A] {
    def apply(a: A): AttributeValue = f(a)
  }

  // I would avoid Traversable Once as much as possible and enforce a List or Seq, or whatever works best.
  // Not something generic.
  implicit def encodeTraversableOnce[A0, C[_]](implicit
                                               e: Encoder[A0],
                                               is: IsTraversableOnce[C[A0]] {type A = A0}
                                              ): Encoder[C[A0]] =
    instance { list =>
      val items = new java.util.ArrayList[AttributeValue]()

      is.conversion(list).foreach { a =>
        items add e(a)
      }

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

  implicit def encodeOption[A](implicit e: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {

    /* The option.get is presumably unsafe?


      A solution may be to introduce a new layer of your own types wrapping AttributeValue
      This would essentially be a JSON tree of types, with a specific DynamoNull which you can turn `None` into
      These values would then be elided from insertion in dynamo since I believe you can't explicitly insert null
      into dynamodb

    */
    override def apply(a: Option[A]): AttributeValue = e(a.get)

    override def apply(name: String, a: Option[A]): Map[String, AttributeValue] = {
      if(a.isDefined)
        Map(name -> apply(a))
      else
        Map.empty[String,AttributeValue]
    }
  }


  implicit def encodeMapLike[M[K, +V] <: Map[K, V], V](implicit
                                                       e: Encoder[V]
                                                      ): Encoder[M[String, V]] = Encoder.instance { m =>
    val map = m.map {
      case (k, v) => (k, e(v))
    }
    import scala.collection.JavaConversions._

    new AttributeValue().withM(map)
  }

  // You can't decode Either's, so I would avoid encoding them
  // Wrapping the encoder/decoder pair in something like slick's MappedColumnType would prevent this problem
  def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
                                                            ea: Encoder[A],
                                                            eb: Encoder[B]
  ): Encoder[Either[A, B]] = instance { a =>
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
