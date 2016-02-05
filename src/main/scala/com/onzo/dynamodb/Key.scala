package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue

trait KeyLike[A] {
  def encode(t: A): Map[String, AttributeValue]

  def decode(items: Map[String, AttributeValue]): A
}

trait NamedKeyLike[A] extends KeyLike[A] {
  val name: String

  val encoder: Encoder[A]

  val decoder: Decoder[A]

  def encode(t: A): Map[String, AttributeValue] = encoder.apply(name, t)

  def decode(items: Map[String, AttributeValue]): A = decoder(name, items)
}

case class PrimaryKey[A](name: String)(implicit val encoder: Encoder[A], val decoder: Decoder[A])
  extends NamedKeyLike[A]

case class RangeKey[A](name: String)(implicit val encoder: Encoder[A], val decoder: Decoder[A])
  extends NamedKeyLike[A]

case class Key[A](name: String)(implicit val encoder: Encoder[A], val decoder: Decoder[A])
  extends NamedKeyLike[A]

/**
  * This key return the rest of the dynamodb columns not mapped by other the keys
  * This key write it's Map[String, A] as column in dynamodb
  * @param encoder
  * @param decoder
  * @tparam A
  */
case class MapKey[A](implicit val encoder: Encoder[A], val decoder: Decoder[A])
  extends KeyLike[Map[String, A]] {
  def encode(t: Map[String, A]): Map[String, AttributeValue] = {
    val mapB = Map.newBuilder[String, AttributeValue]
    t.foreach {
      case (key, v) => mapB ++= encoder(key, v)
    }
    mapB.result()
  }

  def decode(items: Map[String, AttributeValue]): Map[String, A] = {
    val mapB = Map.newBuilder[String, A]
    items.foreach {
      case (key, v) => mapB += key -> decoder(key, items)
    }
    mapB.result()
  }
}