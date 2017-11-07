import com.trueaccord.scalapb.compiler.Version.{grpcJavaVersion, scalapbVersion, protobufVersion}


lazy val root = (project in file(".")).aggregate(authservice, chatclient, chatservice)

lazy val commonSettings = Seq(
  scalaVersion := "2.12.3",
  organization := "grpc-chatroom",
  version      := "0.1",
  libraryDependencies ++= Seq(
    "com.auth0" % "java-jwt" % "3.2.0",
    "io.grpc" % "grpc-netty" % grpcJavaVersion,
    "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion  % "protobuf",
    "org.slf4j" % "slf4j-api" % "1.7.5",
    "org.slf4j" % "slf4j-simple" % "1.7.5",
    "io.zipkin.brave" % "brave-instrumentation-grpc" % "4.4.0",
    "io.zipkin.reporter" % "zipkin-sender-urlconnection" % "0.10.0",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"
  ),
  scalacOptions ++= Seq(
    "-feature",
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-unchecked",
    "-deprecation",
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )
)

lazy val authservice = (project in file("authservice"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name         := "authservice",
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    commonSettings,
    libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion
  )


lazy val chatservice = (project in file("chatservice"))
  .dependsOn(authservice)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name         := "chatservice",
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    commonSettings,
    libraryDependencies += "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion
  )

lazy val chatclient = (project in file("chatclient"))
   .dependsOn(chatservice)
  .enablePlugins(JavaAppPackaging)
  .settings(
    name         := "chatclient",
    commonSettings,
    libraryDependencies ++= Seq(
      "jline" % "jline" % "2.14.4"
    )
  )
