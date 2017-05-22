package com.onzo.dynamodb

import com.github.dwhjames.awswrap.dynamodb._
import scala.collection.mutable


abstract class Table[T](override val tableName: String) extends DynamoDBSerializer[T] {

  val * : TableMapper[T]

  // get on option?
  override def hashAttributeName: String = *.primaryKey.get._1

  override def rangeAttributeName: Option[String] = *.rangeKey.map(_._1)

  override def primaryKeyOf(obj: T): Map[String, AttributeValue] = {
    Map(encodeKey(obj, *.primaryKey).get) ++ encodeKey(obj, *.rangeKey)
  }

  private def encodeKey(obj: T, key: Option[(String, Encoder[T])]): Option[(String, AttributeValue)] = key.map {
    case (k, encoder) => k -> encoder(obj)
  }

  override def fromAttributeMap(item: mutable.Map[String, AttributeValue]): T = *.decode(item.toMap)

  override def toAttributeMap(obj: T): Map[String, AttributeValue] = *.encode(obj)
}
