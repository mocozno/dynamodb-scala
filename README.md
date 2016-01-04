### Scala Helper/Mapper for dynamodb

##### Setup

```scala
resolvers += "dynamodb-scala" at "http://dl.bintray.com/onzo/maven"
libraryDependencies += "com.onzo" %% "dynamodb-scala" % "0.1.0"
```

##### Example

```scala
  implicit object forumSerializer extends Table[Forum](Forum.tableName) {
    import cats.syntax.monoidal._

    override def * : Column[Forum] = {
      Column[String]("Name", PrimaryKey) |@|
      Column[String]("Category") |@|
      Column[Long]("Threads") |@|
      Column[Long]("Messages") |@|
      Column[Long]("Views")
    }.imap(Forum.apply)(unlift(Forum.unapply))
  }
```


##### dependencies

cats 0.4 (Monoidal)

