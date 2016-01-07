package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue

trait TableMapper[A] {
  self =>

  def primaryKey: Option[(String, Encoder[A])]

  def rangeKey: Option[(String, Encoder[A])]

  def encode(t: A): Map[String, AttributeValue]

  def decode(items: Map[String, AttributeValue]): A
}
