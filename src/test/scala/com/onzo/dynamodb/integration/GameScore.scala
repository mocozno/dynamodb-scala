/*
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

import com.github.dwhjames.awswrap.dynamodb._

import com.amazonaws.services.dynamodbv2.model._
import com.onzo.dynamodb._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import shapeless.HNil

case class GameScore(
                      userId: String,
                      gameTitle: String,
                      topScore: Long,
                      topScoreDateTime: DateTime,
                      wins: Long,
                      losses: Long,
                      extra: Map[String, String] = Map.empty,
                      seq: Seq[String] = Seq.empty
                    )

object GameScore {

  val tableName = "GameScores"
  val globalSecondaryIndexName = "GameTitleIndex"

  val tableRequest =
    new CreateTableRequest()
      .withTableName(GameScore.tableName)
      .withProvisionedThroughput(Schema.provisionedThroughput(10L, 5L))
      .withAttributeDefinitions(
        Schema.stringAttribute(Attributes.userId),
        Schema.stringAttribute(Attributes.gameTitle),
        Schema.numberAttribute(Attributes.topScore)
      )
      .withKeySchema(
        Schema.hashKey(Attributes.userId),
        Schema.rangeKey(Attributes.gameTitle)
      )
      .withGlobalSecondaryIndexes(
        new GlobalSecondaryIndex()
          .withIndexName(GameScore.globalSecondaryIndexName)
          .withProvisionedThroughput(Schema.provisionedThroughput(10L, 5L))
          .withKeySchema(
            Schema.hashKey(Attributes.gameTitle),
            Schema.rangeKey(Attributes.topScore)
          )
          .withProjection(
            new Projection()
              .withProjectionType(ProjectionType.KEYS_ONLY)
          )
      )

  object Attributes {
    val userId = "UserId"
    val gameTitle = "GameTitle"
    val topScore = "TopScore"
    val topScoreDateTime = "TopScoreDateTime"
    val wins = "Wins"
    val losses = "Losses"
    val extra = "extra"
    val seq = "seq"
  }

  implicit object sameScoreSerializer extends Table[GameScore](GameScore.tableName) {

    private val fmt = ISODateTimeFormat.dateTime

    val * : TableMapper[GameScore] = {
      PrimaryKey[String]("UserId") ::
        RangeKey[String]("GameTitle") ::
        Key[Long]("TopScore") ::
        Key[DateTime]("TopScoreDateTime")(Encoder[String].contramap { d: DateTime => fmt.print(d) }, Decoder[String].map(fmt.parseDateTime)) ::
        Key[Long]("Wins") ::
        Key[Long]("Losses") ::
        Key[Map[String, String]]("extra") ::
        Key[Seq[String]]("seq") :: HNil
    }.as[GameScore]
  }

}
