/*
 * Copyright 2012-2015 Pellucid Analytics
 * Copyright 2015 Daniel W. H. James
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onzo.dynamodb.integration

import com.amazonaws.services.dynamodbv2.model._
import com.github.dwhjames.awswrap.dynamodb._
import com.onzo.dynamodb._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


case class Reply(
                  id: String,
                  replyDateTime: DateTime,
                  message: String,
                  postedBy: String
                )

object Reply {

  val tableName = "Reply"
  val secondaryIndexName = "PostedByIndex"

  val tableRequest =
    new CreateTableRequest()
      .withTableName(Reply.tableName)
      .withProvisionedThroughput(Schema.provisionedThroughput(10L, 5L))
      .withAttributeDefinitions(
        Schema.stringAttribute(Attributes.id),
        Schema.stringAttribute(Attributes.replyDateTime),
        Schema.stringAttribute(Attributes.postedBy)
      )
      .withKeySchema(
        Schema.hashKey(Attributes.id),
        Schema.rangeKey(Attributes.replyDateTime)
      )
      .withLocalSecondaryIndexes(
        new LocalSecondaryIndex()
          .withIndexName(Reply.secondaryIndexName)
          .withKeySchema(
            Schema.hashKey(Attributes.id),
            Schema.rangeKey(Attributes.postedBy)
          )
          .withProjection(
            new Projection()
              .withProjectionType(ProjectionType.KEYS_ONLY)
          )
      )

  object Attributes {
    val id = "Id"
    val replyDateTime = "ReplyDateTime"
    val message = "Message"
    val postedBy = "PostedBy"
  }

  implicit object replySerializer extends Table[Reply](Reply.tableName) {

    import cats.syntax.monoidal._

    private val fmt = ISODateTimeFormat.dateTime

    override def * : Column[Reply] = {
      Column[String]("Id", PrimaryKey) |@|
        Column[DateTime]("ReplyDateTime", RangeKey)(Encoder[String].contramap { d: DateTime => fmt.print(d) }, Decoder[String].map(fmt.parseDateTime)) |@|
        Column[String]("Message") |@|
        Column[String]("PostedBy")
    }.imap(Reply.apply)(unlift(Reply.unapply))
  }

}
