package com.onzo.dynamodb

import cats._, cats.syntax.monoidal._
import org.scalatest.FreeSpec

case class Test(a: String, b: String, c: String)

object Test {
  implicit val testColumn: Column[Test] = {
    Column[String]("a") |@|
      Column[String]("b") |@|
      Column[String]("c")
  }.imap(Test.apply)(unlift(Test.unapply))

  def unlift[A, B](func: A => Option[B]): A => B =
    (value: A) => func(value).get

  implicit object taskSerializer extends Table[Test]("test") {
    import com.github.dwhjames.awswrap.dynamodb._
    import cats.syntax.monoidal._

    def id = Column[String]("id")

    override def * : Column[Test] = {
      Column[String]("a", PrimaryKey) |@|
        Column[String]("b", RangeKey) |@|
        Column[String]("c")
    }.imap(Test.apply)(unlift(Test.unapply))
  }
}


class SetSpec extends FreeSpec {


  "A Set" - {
    "when empty" - {
      "should have size 0" in {
        assert(Set.empty.size == 0)
      }

      "should produce NoSuchElementException when head is invoked" in {
        intercept[NoSuchElementException] {
          Set.empty.head
        }
      }
    }
  }
}