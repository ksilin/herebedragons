name := "herebedragons"
organization := "com.example"
version := "0.1.1"
scalaVersion := "2.12.2"
scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8", "-language:postfixOps")

libraryDependencies ++= {
  val akkaStreamV   = "2.5.3"
  val akkaHttpV     = "10.0.9"
  val akkaHttpCirce = "1.17.0"
  val scalaTestV    = "3.0.3"
  val slickVersion  = "3.2.0"
  val Circe         = "0.8.0"
  Seq(
    "com.typesafe.akka"          %% "akka-stream"         % akkaStreamV,
    "com.typesafe.akka"          %% "akka-http"           % akkaHttpV,
    "de.heikoseeberger"          %% "akka-http-circe"     % akkaHttpCirce,
    "io.circe"                   %% "circe-generic"       % Circe,
    "io.circe"                   %% "circe-parser"        % Circe,
    "com.typesafe.slick"         %% "slick"               % slickVersion,
    "com.typesafe.slick"         %% "slick-hikaricp"      % slickVersion,
    "mysql"                      % "mysql-connector-java" % "6.0.6",
    "com.h2database"             % "h2"                   % "1.4.196",
    "com.zaxxer"                 % "HikariCP"             % "2.6.3",
    "ch.qos.logback"             % "logback-classic"      % "1.2.3",
    "org.codehaus.janino"        % "janino"               % "3.0.7",
    "com.typesafe.scala-logging" %% "scala-logging"       % "3.5.0",
    "org.scalatest"              %% "scalatest"           % scalaTestV % "it,test",
    "com.typesafe.akka"          %% "akka-http-testkit"   % akkaHttpV % "it,test"
  )
}

lazy val root = project.in(file(".")).configs(IntegrationTest)
Defaults.itSettings

Revolver.settings
enablePlugins(JavaAppPackaging)

parallelExecution in Test := false

fork in run := true
