package com.onzo.dynamodb

import java.util.UUID
import cats.functor.Contravariant
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import scala.collection.generic.IsTraversableOnce

trait Encoder[A] {

  def apply(a: A): AttributeValue

  /*
  I would remove this function. Creating a `Map` is not an `Encoder` task.
  The user of this trait should create the `Map` himself and use `apply(A)`.
  */
  def apply(name: String, a: A): Map[String, AttributeValue] = {
    Map(name -> apply(a))
  }

  def contramap[B](f: B => A): Encoder[B] = Encoder.instance(b => apply(f(b)))
}

object Encoder {

  def apply[A](implicit e: Encoder[A]): Encoder[A] = e

  // should be probably private
  def instance[A](f: A => AttributeValue): Encoder[A] = new Encoder[A] {
    def apply(a: A): AttributeValue = f(a)
  }

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

    // Usage of `a.get`. Will fail for a `None`. Should use `a.getOrElse` instead.
    override def apply(a: Option[A]): AttributeValue = e(a.get)

    // See comment for this function in `Encoder[A]`
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

  /*
  It makes no sense to encode an `Either` and store into a data store because the stored value can't be `Decoded`
  in a sound way later. This function should be removed.
   */
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

  /*
   Remove cats support. The main concern of this library is DynamoDB and it is sufficient to support the basic
   Scala types. Additional support for 3rd party libraries should be in a separate library, f.e. dynamodb-scala-cats
  */
  implicit val contravariantEncode: Contravariant[Encoder] = new Contravariant[Encoder] {
    def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }

}
