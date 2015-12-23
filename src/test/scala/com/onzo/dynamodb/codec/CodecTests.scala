package com.onzo.dynamodb.codec

import algebra.Eq
import cats.laws.discipline._
import com.onzo.dynamodb.{Decoder, Encoder}
import org.scalacheck.{Arbitrary, Prop}
import org.typelevel.discipline.Laws

trait CodecTests[A] extends Laws with ArbitraryInstances {
  def laws: CodecLaws[A]

  def codec(implicit A: Arbitrary[A], eq: Eq[A]): RuleSet = new DefaultRuleSet(
    name = "codec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.codecRoundTrip(a)
    }
  )
}

object CodecTests {
  def apply[A: Decoder : Encoder]: CodecTests[A] = new CodecTests[A] {
    val laws: CodecLaws[A] = CodecLaws[A]
  }
}
