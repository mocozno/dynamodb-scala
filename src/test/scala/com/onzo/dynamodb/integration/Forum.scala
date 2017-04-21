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

case class Forum(
                  name: String,
                  category: String,
                  threads: Long,
                  messages: Long,
                  views: Long
                )

object Forum {

  val tableName = "Forum"

  val tableRequest =
    new CreateTableRequest()
      .withTableName(Forum.tableName)
      .withProvisionedThroughput(Schema.provisionedThroughput(10L, 5L))
      .withAttributeDefinitions(Schema.stringAttribute(Attributes.name))
      .withKeySchema(Schema.hashKey(Attributes.name))

  object Attributes {
    val name = "Name"
    val category = "Category"
    val threads = "Threads"
    val messages = "Messages"
    val views = "Views"
  }

  implicit object forumSerializer extends Table[Forum](Forum.tableName) {

    import shapeless._

    def name = PrimaryKey[String]("Name")

    def category = Key[String]("Category")

    def threads = Key[Long]("Threads")

    def messages = Key[Long]("Messages")

    def views = Key[Long]("Views")

    override val * = (name :: category :: threads :: messages :: views :: HNil).as[Forum]
  }
}
