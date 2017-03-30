package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import shapeless._

object HlistHelper {

  object findPrimaryKey extends Poly1 {
    implicit def toCol2[A]: Case.Aux[PrimaryKey[A], PrimaryKey[A]] = {
      at[PrimaryKey[A]](identity)
    }
  }

  object findPrimaryKeyValue extends Poly1 {
    implicit def toCol2[A]: Case.Aux[(PrimaryKey[A], A), A] = {
      at[(PrimaryKey[A], A)](_._2)
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
    // Confusing name, R
    type R = (map, Names)

    // Badly named functions. The signature, while all you need, is still pretty unfriendly so I think
    // a better name like 'decodeFromKey' would be good
    implicit def toCol1[A]: Case.Aux[(Key[A], R), A] = {
      at[(Key[A], R)] {
        f => f._1.decode(f._2._1)
      }
    }

    implicit def toCol2[A]: Case.Aux[(PrimaryKey[A], R), A] = {
      at[(PrimaryKey[A], R)] { f => f._1.decode(f._2._1) // This tuple notation obsures what is going on, case classes might be better
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

  // This is not finding all key names, from the signatures it appears to be one step of an aggregation
  // Maybe rename this 'aggregateKeyName'
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
