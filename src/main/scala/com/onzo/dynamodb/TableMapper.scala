package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue

trait TableMapper[A] {
  self =>

  // Not 100% sure on the mechanics of dynamo db but I think these should be enforced and be non-optional
  // If you need to read tables without a primary key that is another matter, but in Table.scala you call .get on this
  def primaryKey: Option[(String, Encoder[A])]

  def rangeKey: Option[(String, Encoder[A])]

  def encode(t: A): Map[String, AttributeValue]

  def decode(items: Map[String, AttributeValue]): A
}
