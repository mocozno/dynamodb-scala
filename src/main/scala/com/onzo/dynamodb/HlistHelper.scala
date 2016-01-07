package com.onzo.dynamodb

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import shapeless._

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
  }

  object DecodeHlist extends Poly1 {
    type R = Map[String, AttributeValue]

    implicit def toCol1[A]: Case.Aux[(Key[A], R), A] = {
      at[(Key[A], R)] {
        f => f._1.decode(f._2)
      }
    }

    implicit def toCol2[A]: Case.Aux[(PrimaryKey[A], R), A] = {
      at[(PrimaryKey[A], R)] { f => f._1.decode(f._2)
      }
    }

    implicit def toCol3[A]: Case.Aux[(RangeKey[A], R), A] = {
      at[(RangeKey[A], R)] { f => f._1.decode(f._2)
      }
    }
  }

}
