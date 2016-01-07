### Scala Helper/Mapper for dynamodb

##### Setup

```scala
resolvers += "dynamodb-scala" at "http://dl.bintray.com/onzo/maven"
libraryDependencies += "com.onzo" %% "dynamodb-scala" % "0.1.0"
```

##### Example

```scala
  implicit object forumSerializer extends Table[Forum](Forum.tableName) {

    import shapeless._

    def name = PrimaryKey[String]("Name")

    def category = Key[String]("Category")

    def threads = Key[Long]("Threads")

    def messages = Key[Long]("Messages")

    def views = Key[Long]("Views")

    override val * = (name :: category :: threads :: messages :: views :: HNil).as[Forum]
  }
```


##### dependencies

cats 0.4 (Monoidal)

