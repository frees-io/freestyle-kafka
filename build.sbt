pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val commonDependencies: Seq[ModuleID] = Seq(
  %%("cats-core"),
  %%("classy-cats"),
  %%("classy-config-typesafe"),
  %%("classy-core"),
  %%("classy-generic"),
  %("kafka-clients"),
  %("kafka-streams"))

lazy val testDependencies: Seq[ModuleID] = Seq(
  %%("scalatest") % "test",
  %%("scalamockScalatest") % "test",
  %%("scalacheck") % "test")

lazy val root = project
  .in(file("."))
  .settings(name := "freestyle-kafka")
  .settings(noPublishSettings)
  .settings(scalaMetaSettings)
  .dependsOn(core)
  .aggregate(core)

lazy val core = project.in(file("core"))
  .settings(moduleName := "freestyle-kafka-core")
  .settings(scalaMetaSettings)
  .settings(libraryDependencies ++= commonDependencies)
  .settings(libraryDependencies ++= testDependencies)