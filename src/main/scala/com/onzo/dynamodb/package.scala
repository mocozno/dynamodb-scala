package com.onzo

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import shapeless.LUBConstraint._
import shapeless._
import shapeless.ops.hlist._


package object dynamodb {

  // These HList types could be more helpfully named
  implicit class KeysHList[
    A <: HList : <<:[KeyLike[_]]#λ,
    M <: HList,
    N <: HList,
    T <: HList,
    Primary
  ](a: A) {

    // todo remove?
    // I don't know what this is meant to do but based purely on the line of code I would remove it
    val optionalRangeKey = RangeKey[Int]("rangeKeyCheat")

    def as[B](implicit entityGen: Generic.Aux[B, M]
              , zipper: Zip.Aux[A :: M :: HNil, N]
              , collectPrimaryKey: CollectFirst.Aux[A, HlistHelper.findPrimaryKey.type, PrimaryKey[Primary]]
              , collectFirst2: CollectFirst.Aux[N, HlistHelper.findPrimaryKeyValue.type, Primary]
              , foldLeftEncode: LeftFolder.Aux[N, Map[String, AttributeValue], HlistHelper.EncodeHlist.type, Map[String, AttributeValue]]
              , zipperMap: ZipConst.Aux[HlistHelper.DecodeHlist.R, A, T]
              , decodeMapper: Mapper.Aux[HlistHelper.DecodeHlist.type, T, M]
              , findAllKeyName: LeftFolder.Aux[A, List[String], HlistHelper.findAllKeyName.type, List[String]]
             ): TableMapper[B] = {

      val _primaryKey: PrimaryKey[Primary] = a.collectFirst(HlistHelper.findPrimaryKey)(collectPrimaryKey)
      // Again why runtimeList? Would using the fact a is an HList
      // allow us to eliminate this option, because all types are known at compile time?
      val _rangeKey = a.runtimeList.collectFirst({
        case r: RangeKey[_] => r
      })

      val names = a.foldLeft(List.empty[String])(HlistHelper.findAllKeyName)(findAllKeyName)

      new TableMapper[B] {
        override val primaryKey: Option[(String, Encoder[B])] = {
          // This Some should be unnecessary I think (See TableMapper.scala comment too)
          Some(_primaryKey.name -> Encoder.instance {
            b: B =>
              val zipped = a.zip(entityGen.to(b))(zipper)

              val v = zipped.collectFirst(HlistHelper.findPrimaryKeyValue)(collectFirst2)
              _primaryKey.encoder.apply(v)
          })
        }

        // I think this option is redundant due to my comment about runTimeList above
        override def rangeKey: Option[(String, Encoder[B])] = {
          _rangeKey.map { key =>
            key.name -> Encoder.instance {
              b: B =>
                val zipped = a.zip(entityGen.to(b))(zipper)

                // at this point we know there is a RangeKey and we know that entityGen exist
                // Why are we using runtimeList here? Could we findFirst in the HList (like is done elsewhere) and prevent
                // the compiler warning about erasure on a: A?
                // This would also enable us to get rid of the .get as I noted above
                zipped.runtimeList.collectFirst({
                  case (r: RangeKey[A], a: A) => r.encoder.apply(a)
                }).get
            }
          }
        }

        override def encode(b: B): Map[String, AttributeValue] = {
          val zipped = a.zip(entityGen.to(b))
          zipped.foldLeft(Map.empty[String, AttributeValue])(HlistHelper.EncodeHlist)(foldLeftEncode)
        }

        override def decode(items: Map[String, AttributeValue]): B = {
          val zipped = a.zipConst((items, names))(zipperMap)
          val hlist = zipped.map(HlistHelper.DecodeHlist)(decodeMapper)
          entityGen.from(hlist)
        }
      }
    }
  }

}
