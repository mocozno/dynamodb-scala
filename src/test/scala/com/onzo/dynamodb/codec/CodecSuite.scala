package com.onzo.dynamodb.codec

import java.math.MathContext
import java.util.UUID

import algebra.Eq
import cats.std.AllInstances
import cats.syntax.AllSyntax
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import org.scalacheck.Arbitrary
import org.scalatest.{FunSuite, Matchers}
import org.typelevel.discipline.scalatest.Discipline

import scala.util.Try

trait EqInstances {
  implicit def eqBigDecimal: Eq[BigDecimal] = Eq.fromUniversalEquals

  implicit def eqUUID: Eq[UUID] = Eq.fromUniversalEquals

  implicit def eqAttributeValue: Eq[AttributeValue] = Eq.fromUniversalEquals
}

trait DynamoDBSuite extends FunSuite with Matchers with Discipline with AllInstances with AllSyntax
  with ArbitraryInstances with EqInstances {
  override def convertToEqualizer[T](left: T): Equalizer[T] = ???
}
//It is good to see most codecs tested
//TODO: Note which Encoders/Decoders are not tested
class AnyValCodecSuite extends DynamoDBSuite {

  checkAll("Codec[Boolean]", CodecTests[Boolean].codec)
  checkAll("Codec[Float]", CodecTests[Float].codec)
  checkAll("Codec[Double]", CodecTests[Double].codec)
  checkAll("Codec[Byte]", CodecTests[Byte].codec)
  checkAll("Codec[Short]", CodecTests[Short].codec)
  checkAll("Codec[Int]", CodecTests[Int].codec)
  checkAll("Codec[Long]", CodecTests[Long].codec)

  //TODO: Find why these needed to be commented out
  //checkAll("Codec[Char]", CodecTests[Char].codec)
  //checkAll("Codec[Unit]", CodecTests[Unit].codec)
}

class StdLibCodecSuite extends DynamoDBSuite {
  checkAll("Codec[String]", CodecTests[String].codec)
  checkAll("Codec[BigInt]", CodecTests[BigInt].codec)

  // reduce the scope of BigDecimal (some generated BigDecimal can't be parsed from a string again)
  implicit val bigDecimalArb: Arbitrary[BigDecimal] = Arbitrary(
    Arbitrary.arbBigDecimal.arbitrary.filter(d => Try(BigDecimal(d.toString(), MathContext.UNLIMITED)).isSuccess))

  checkAll("Codec[BigDecimal]", CodecTests[BigDecimal].codec)

  checkAll("Codec[UUID]", CodecTests[UUID].codec)
  checkAll("Codec[Option[Int]]", CodecTests[Option[Int]].codec)
  checkAll("Codec[List[Int]]", CodecTests[List[Int]].codec)
  checkAll("Codec[Map[String, Int]]", CodecTests[Map[String, Int]].codec)
  checkAll("Codec[Set[Int]]", CodecTests[Set[Int]].codec)
}

class DynamoDBLibCodecSuite extends DynamoDBSuite {
  checkAll("Codec[AttributeValue]", CodecTests[AttributeValue].codec)
}