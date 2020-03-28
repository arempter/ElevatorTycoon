import scalariform.formatter.preferences._

name := "ElevatorTycoon"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions += "-unchecked"
scalacOptions += "-deprecation"
scalacOptions += "-feature"
scalacOptions += "-Xlint"
scalacOptions += "-Xfatal-warnings"

val akkaHttpVersion = "10.1.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka"             %% "akka-http"              % akkaHttpVersion,
  "com.typesafe.akka"             %% "akka-stream"            % "2.6.4",
  "com.typesafe.scala-logging"    %% "scala-logging"          % "3.9.2",
  "ch.qos.logback"                % "logback-classic"         % "1.2.3",
  "com.typesafe.akka"             %% "akka-http-spray-json"   % akkaHttpVersion,
  "org.scalatest"                 %% "scalatest"              % "3.1.0"           % "test"
)

parallelExecution in Test:= true

enablePlugins(JavaAppPackaging)
dockerExposedPorts := Seq(8080)

scalariformPreferences := scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DanglingCloseParenthesis, Preserve)
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(DoubleIndentMethodDeclaration, true)
  .setPreference(NewlineAtEndOfFile, true)
  .setPreference(SingleCasePatternOnNewline, false)