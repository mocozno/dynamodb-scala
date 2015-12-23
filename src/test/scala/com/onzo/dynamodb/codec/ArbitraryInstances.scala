package com.onzo.dynamodb.codec

import java.util.UUID

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryInstances {
  private[this] def genBool: Gen[AttributeValue] = Arbitrary.arbBool.arbitrary.map(new AttributeValue().withBOOL(_))

  private[this] def genString: Gen[AttributeValue] = Arbitrary.arbString.arbitrary.map(new AttributeValue().withS(_))

  private[this] def genNumber: Gen[AttributeValue] = Arbitrary.arbLong.arbitrary.map(n => new AttributeValue().withN(n.toString))

  private[this] def genAttributeValue: Gen[AttributeValue] = Gen.const(new AttributeValue())

  implicit def arbitraryUUID: Arbitrary[UUID] = Arbitrary(Gen.uuid)

  implicit def arbitraryAttributeValue: Arbitrary[AttributeValue] = Arbitrary(genAttributeValue)

}
