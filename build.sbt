name          := """herebedragons"""
organization  := "com.example"
version       := "0.1.0"
scalaVersion  := "2.11.7"
scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8", "-language:postfixOps")

libraryDependencies ++= {
  val akkaStreamV      = "2.0.3"
  val scalaTestV       = "3.0.0-M15"
  val scalazScalaTestV = "0.2.3"
  val slickVersion     = "3.1.1"
  Seq(
    "com.typesafe.akka"  %% "akka-stream-experimental"             % akkaStreamV,
    "com.typesafe.akka"  %% "akka-http-core-experimental"          % akkaStreamV,
    "com.typesafe.akka"  %% "akka-http-spray-json-experimental"    % akkaStreamV,

    "com.typesafe.slick" %% "slick"                                % slickVersion,
    "mysql" % "mysql-connector-java" % "5.1.38",
    "com.h2database"      % "h2"              % "1.4.191",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
    "com.zaxxer" % "HikariCP" % "2.4.3",

    "ch.qos.logback" % "logback-classic" % "1.1.3",
    "org.codehaus.janino" % "janino" % "2.7.8",

    "org.scalatest"      %% "scalatest"                            % scalaTestV       % "it,test",
    "com.typesafe.akka"  %% "akka-http-testkit-experimental"       % akkaStreamV      % "it,test"
  )
}

lazy val root = project.in(file(".")).configs(IntegrationTest)
Defaults.itSettings

Revolver.settings
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)
//enablePlugins(JmhPlugin)

dockerExposedPorts := Seq(9000)

dockerEntrypoint := Seq("bin/%s" format executableScriptName.value, "-Dconfig.resource=docker.conf")

parallelExecution in Test := false

fork in run := true