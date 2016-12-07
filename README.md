### Scala Helper/Mapper for dynamodb

##### Setup

```scala
resolvers += "dynamodb-scala" at "http://dl.bintray.com/onzo/maven"
libraryDependencies += "com.onzo" %% "dynamodb-scala" % "0.4.0"

// This burdens the users of this library  with a shapeless dependency (and specific version)
// for shapeless snapshot 2.3.0
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
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

more examples in [test](src/test/scala/com/onzo/dynamodb/integration)

