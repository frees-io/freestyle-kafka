pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val commonDependencies: Seq[ModuleID] = Seq(
  %%("cats-core"),
  %%("classy-cats"),
  %%("classy-config-typesafe"),
  %%("freestyle-async"),
  %%("classy-generic"),
  %("kafka-clients"),
  %("kafka-streams"))

lazy val testDependencies: Seq[ModuleID] = Seq(
  %%("scalatest")          % "test",
  %%("scalamockScalatest") % "test",
  %%("scalacheck")         % "test",
  "org.slf4j"              % "slf4j-api" % "1.7.21" % "test",
  "org.slf4j"              % "log4j-over-slf4j" % "1.7.21" % "test",
  "ch.qos.logback"         % "logback-classic" % "1.1.3" % "test",
  "net.manub"              %% "scalatest-embedded-kafka" % "0.15.0" % "test",
  "net.manub"              %% "scalatest-embedded-kafka-streams" % "0.15.0" % "test"
)

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-kafka")
  .settings(noPublishSettings)
  .settings(scalaMetaSettings)
  .dependsOn(core)
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(moduleName := "freestyle-kafka-core")
  .settings(scalaMetaSettings)
  .settings(parallelExecution in Test := false)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(libraryDependencies ++= testDependencies)
