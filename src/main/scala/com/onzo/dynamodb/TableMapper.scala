package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
//TODO: Think if we can create a com.amazonaws.services.dynamodbv2.model.CreateTableRequest automatically from TableMapper + a few more metadata
trait TableMapper[A] {
  self =>
  // Encoder is a type class. Why carry this information instead of resolving it. Reduces ad-hoc polymorphism opportunities.

  def primaryKey: Option[(String, Encoder[A])]

  def rangeKey: Option[(String, Encoder[A])]

  def encode(t: A): Map[String, AttributeValue]

  def decode(items: Map[String, AttributeValue]): A
}
