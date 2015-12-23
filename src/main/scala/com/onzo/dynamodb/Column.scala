package com.onzo.dynamodb

import cats.functor.Invariant
import com.amazonaws.services.dynamodbv2.model.AttributeValue


import cats._

sealed trait ColumnOption

case object PrimaryKey extends ColumnOption

case object RangeKey extends ColumnOption

trait Column[A] {
  self =>

  def primaryKey: Option[(String, Encoder[A])]

  def rangeKey: Option[(String, Encoder[A])]

  def encode(t: A): Map[String, AttributeValue]

  def decode(items: Map[String, AttributeValue]): A
}

object Column {
  def apply[A](name: String)(implicit encoder: Encoder[A], decoder: Decoder[A]) = new Column[A] {
    override def primaryKey: Option[(String, Encoder[A])] = None

    override def rangeKey: Option[(String, Encoder[A])] = None

    override def encode(t: A): Map[String, AttributeValue] = Map(name -> encoder(t))

    override def decode(items: Map[String, AttributeValue]): A = decoder(items(name))
  }

  def apply[A](name: String, columnOption: ColumnOption)(implicit encoder: Encoder[A], decoder: Decoder[A]) = new Column[A] {
    override def primaryKey: Option[(String, Encoder[A])] = if (columnOption == PrimaryKey) Some(name -> encoder) else None

    override def rangeKey: Option[(String, Encoder[A])] = if (columnOption == RangeKey) Some(name -> encoder) else None

    override def encode(t: A): Map[String, AttributeValue] = Map(name -> encoder(t))

    override def decode(items: Map[String, AttributeValue]): A = decoder(items(name))
  }

  implicit val columnMonoidal: Monoidal[Column] = new Monoidal[Column] {
    override def product[A, B](fa: Column[A], fb: Column[B]): Column[(A, B)] = new Column[(A, B)] {
      override def primaryKey: Option[(String, Encoder[(A, B)])] = {
        mergeKey(fa.primaryKey, fb.primaryKey)
      }

      override def rangeKey: Option[(String, Encoder[(A, B)])] = {
        mergeKey(fa.rangeKey, fb.rangeKey)
      }

      def mergeKey(a: Option[(String, Encoder[A])], b: Option[(String, Encoder[B])]): Option[(String, Encoder[(A, B)])] = {
        a.map {
          case (key, encoder) => key -> encoder.contramap { x: (A, B) => x._1 }
        }.orElse {
          b.map {
            case (key, encoder) => key -> encoder.contramap { x: (A, B) => x._2 }
          }
        }
      }

      override def encode(t: (A, B)): Map[String, AttributeValue] = {
        fa.encode(t._1) ++ fb.encode(t._2)
      }

      override def decode(items: Map[String, AttributeValue]): (A, B) = {
        (fa.decode(items), fb.decode(items))
      }
    }
  }

  implicit val columnFunctor: Invariant[Column] =
    new Invariant[Column] {
      override def imap[A, B](fa: Column[A])(f: (A) => B)(g: (B) => A): Column[B] = new Column[B] {

        override def encode(t: B): Map[String, AttributeValue] = fa.encode(g(t))

        override def decode(items: Map[String, AttributeValue]): B = f(fa.decode(items))

        override def primaryKey: Option[(String, Encoder[B])] = fa.primaryKey.map { case (k, encoder) => k -> encoder.contramap(g)}

        override def rangeKey: Option[(String, Encoder[B])] = fa.rangeKey.map { case (k, encoder) => k -> encoder.contramap(g)}
      }
    }
}


