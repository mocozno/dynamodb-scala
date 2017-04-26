name := "dynamodb-scala"

version := "0.4.2"

scalaVersion := "2.11.7"

organization := "com.onzo"
//compiler options are not used. Should revise the options and decide which to keep
//scalacOptions ++= compilerOptions //uncomment after revision
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
//canonical way to add sonatype snapshots: http://www.scala-sbt.org/0.13/docs/Resolvers.html
resolvers += Resolver.sonatypeRepo("snapshots")

resolvers += Resolver.bintrayRepo("dwhjames", "maven")

val dynamoDbDeps = Seq(
  //https://github.com/dwhjames/aws-wrap
  "com.github.dwhjames" %% "aws-wrap" % "0.8.0",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.10.34" % "provided") //Latest version: 1.11.122
val scalaUtilities = Seq(
  //No need to assign variable for version of single dependency
  "org.typelevel" %% "cats" % "0.4.1", //Latest version: "0.9.0"
  "com.chuusai" %% "shapeless" % "2.3.0" //Latest version is "2.3.2"
)
val testingDeps = Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.5", //Latest version: "1.13.4"
  "org.scalatest" %% "scalatest" % "3.0.0-M9", //Latest version: "3.0.1"
  "joda-time" % "joda-time" % "2.9", //Latest version: "2.9.9"
  "org.joda" % "joda-convert" % "1.8").map(_ % "test")

libraryDependencies ++= dynamoDbDeps ++ scalaUtilities ++ testingDeps

// Bintray:
licenses += ("Apache-2.0", url("http://apache.org/licenses/LICENSE-2.0"))

bintrayPackageLabels in bintray := Seq("scala", "dynamo", "dynamodb", "amazon", "utility")

bintrayRepository in bintray := "maven"
