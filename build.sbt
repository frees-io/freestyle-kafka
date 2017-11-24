pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val commonDependencies: Seq[ModuleID] = Seq(
  %%("cats-core", "1.0.0-MF") force(),
  %%("frees-async", "0.4.3"),
  %%("frees-config", "0.4.3"),
  %("kafka-clients"),
  %("kafka-streams"),
  ("com.github.zainab-ali" %% "fs2-reactive-streams" % "0.2.5").exclude("org.typelevel", "cats-effect"))

lazy val testDependencies: Seq[ModuleID] = Seq(
  %%("scalatest")                        % "test",
  %%("scalamockScalatest")               % "test",
  %%("scalacheck")                       % "test",
  %("slf4j-api")                         % "test",
  %("log4j-over-slf4j")                  % "test",
  %("logback-classic")                   % "test",
  %%("scalatest-embedded-kafka")         % "test",
  %%("scalatest-embedded-kafka-streams") % "test",
)

lazy val root = project
  .in(file("."))
  .settings(name := "frees-kafka")
  .settings(noPublishSettings)
  .settings(scalaMetaSettings)
  .dependsOn(core)
  .aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(moduleName := "frees-kafka-core")
  .settings(scalaMetaSettings)
  .settings(parallelExecution in Test := false)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(libraryDependencies ++= testDependencies)
