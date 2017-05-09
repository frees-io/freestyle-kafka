pgpPassphrase := Some(getEnvVar("PGP_PASSPHRASE").getOrElse("").toCharArray)
pgpPublicRing := file(s"$gpgFolder/pubring.gpg")
pgpSecretRing := file(s"$gpgFolder/secring.gpg")

lazy val root = project.in(file("."))
  .settings(name := "freestyle-kafka")
  .settings(moduleName := "root")
  .settings(noPublishSettings: _*)
  .aggregate(`freestyle-kafkaJS`, `freestyle-kafkaJVM`)

lazy val `freestyle-kafka` = crossProject.in(file("freestyle-kafka"))
  .settings(moduleName := "freestyle-kafka")
  .jsSettings(sharedJsSettings: _*)
  .crossDepSettings(commonDeps ++ freestyleCoreDeps: _*)

lazy val `freestyle-kafkaJVM` = `freestyle-kafka`.jvm
lazy val `freestyle-kafkaJS`  = `freestyle-kafka`.js