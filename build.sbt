name := "dynamodb-scala"

version := "0.1"

scalaVersion := "2.11.7"

organization := "com.onzo"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

// Cats:
lazy val catsVersion = "0.3.0"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.spire-math" %% "cats-core" % catsVersion
)

resolvers += Resolver.bintrayRepo("dwhjames", "maven")
libraryDependencies += "com.github.dwhjames" %% "aws-wrap" % "0.8.0"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.10.34" //% "provided" COMMENTED for sbt run

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.5" % "test",
  "org.scalatest" %% "scalatest" % "3.0.0-M9",
  "org.spire-math" %% "cats-laws" % catsVersion,
  "org.typelevel" %% "discipline" % "0.4",
  "com.chuusai" %% "shapeless" % "2.3.0-SNAPSHOT"
)
// Bintray:

licenses +=("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0"))

bintrayPackageLabels in bintray := Seq("scala", "dynamo", "dynamodb", "amazon", "utility")

bintrayRepository in bintray := "maven"
