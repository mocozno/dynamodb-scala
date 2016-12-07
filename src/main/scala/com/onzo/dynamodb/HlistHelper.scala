package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import shapeless._

/*
I've not much experience with Shapeless. But this code is very hard to read because of the ._1 and ._2.
Should be refactored to use pair pattern matching.
 */

object HlistHelper {

  object findPrimaryKey extends Poly1 {
    implicit def toCol2[A]: Case.Aux[PrimaryKey[A], PrimaryKey[A]] = {
      at[PrimaryKey[A]] { f => f
      }
    }
  }

  object findPrimaryKeyValue extends Poly1 {
    implicit def toCol2[A]: Case.Aux[(PrimaryKey[A], A), A] = {
      at[(PrimaryKey[A], A)] { f => f._2
      }
    }
  }

  object EncodeHlist extends Poly2 {
    type R = Map[String, AttributeValue]

    implicit def caseMapStringAttributeValue1[A]: Case.Aux[R, (Key[A], A), R] = at[R, (Key[A], A)]((m, a) => m ++ a._1.encode(a._2))

    implicit def caseMapStringAttributeValue2[A]: Case.Aux[R, (PrimaryKey[A], A), R] = at[R, (PrimaryKey[A], A)]((m, a) => m ++ a._1.encode(a._2))

    implicit def caseMapStringAttributeValue3[A]: Case.Aux[R, (RangeKey[A], A), R] = at[R, (RangeKey[A], A)]((m, a) => m ++ a._1.encode(a._2))

    implicit def caseMapStringAttributeValue4[A]: Case.Aux[R, (MapKey[A], Map[String, A]), R] = at[R, (MapKey[A], Map[String, A])]((m, a) => m ++ a._1.encode(a._2))
  }

  object DecodeHlist extends Poly1 {
    type map = Map[String, AttributeValue]
    type Names = List[String]
    type R = (map, Names)

    implicit def toCol1[A]: Case.Aux[(Key[A], R), A] = {
      at[(Key[A], R)] {
        f => f._1.decode(f._2._1)
      }
    }

    implicit def toCol2[A]: Case.Aux[(PrimaryKey[A], R), A] = {
      at[(PrimaryKey[A], R)] { f => f._1.decode(f._2._1)
      }
    }

    implicit def toCol3[A]: Case.Aux[(RangeKey[A], R), A] = {
      at[(RangeKey[A], R)] { f => f._1.decode(f._2._1)
      }
    }

    implicit def toCol4[A]: Case.Aux[(MapKey[A], R), Map[String, A]] = {
      at[(MapKey[A], R)] { f =>
        val filtered = f._2._1.filterKeys(!f._2._2.contains(_))
        f._1.decode(filtered)
      }
    }
  }

  object findAllKeyName extends Poly2 {

    implicit def toCol1[Repr <: HList, A]: Case.Aux[List[String], Key[A], List[String]] = {
      at[List[String], Key[A]] {
        (names, k) => k.name :: names
      }
    }

    implicit def toCol2[Repr <: HList, A]: Case.Aux[List[String], PrimaryKey[A], List[String]] = {
      at[List[String], PrimaryKey[A]] {
        (names, k) => k.name :: names
      }
    }

    implicit def toCol3[Repr <: HList, A]: Case.Aux[List[String], RangeKey[A], List[String]] = {
      at[List[String], RangeKey[A]] {
        (names, k) => k.name :: names
      }
    }

    implicit def toCol4[Repr <: HList, A]: Case.Aux[List[String], MapKey[A], List[String]] = {
      at[List[String], MapKey[A]] {
        (names, k) => names
      }
    }
  }

}
