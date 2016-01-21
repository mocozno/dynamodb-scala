package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue

trait KeyLike[A] {
  val name: String

  val encoder: Encoder[A]

  val decoder: Decoder[A]

  def encode(t: A): Map[String, AttributeValue] = Map(name -> encoder(t))

  def decode(items: Map[String, AttributeValue]): A = decoder(items(name))
}

case class PrimaryKey[A](name: String)(implicit val encoder: Encoder[A], val decoder: Decoder[A])
  extends KeyLike[A]

case class RangeKey[A](name: String)(implicit val encoder: Encoder[A], val decoder: Decoder[A])
  extends KeyLike[A]

case class Key[A](name: String)(implicit val encoder: Encoder[A], val decoder: Decoder[A])
  extends KeyLike[A]
