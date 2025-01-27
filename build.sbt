/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys.{parallelExecution, _}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import bloop.integrations.sbt.BloopDefaults

import scala.language.postfixOps

val compile = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.4.0",
  "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-26",
  "org.julienrf" %% "play-json-derived-codecs" % "6.0.0",
  "com.typesafe.play" %% "play-json" % "2.7.1",
  "uk.gov.hmrc" %% "http-metrics" % "1.10.0",
  "org.typelevel" %% "cats-core" % "2.1.0",
  "eu.timepit" %% "refined"                 % "0.9.13",
  "be.venneborg" %% "play26-refined" % "0.5.0",
  "commons-validator" % "commons-validator" % "1.6"
)

// we need to override the akka version for now as newer versions are not compatible with reactivemongo
lazy val akkaVersion = "2.5.23"
lazy val akkaHttpVersion = "10.0.15"

val overrides: Seq[ModuleID] = Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-protobuf" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion
)

def test(scope: String = "test,acceptance") = Seq(
  "uk.gov.hmrc" %% "reactivemongo-test" % "4.21.0-play-26" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
  "org.mockito" %% "mockito-scala-scalatest" % "1.14.4" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "com.typesafe.play" %% "play-test" % play.core.PlayVersion.current % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.26.3" % scope
)

val appName = "api-subscription-fields"

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases"), Resolver.jcenterRepo, Resolver.sonatypeRepo("releases"), Resolver.sonatypeRepo("snapshots"))

lazy val plugins: Seq[Plugins] = Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val AcceptanceTest = config("acceptance") extend Test

lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .configs(AcceptanceTest)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(acceptanceTestSettings: _*)
  .settings(scalaVersion := "2.12.11")
  .settings(
    inConfig(AcceptanceTest)(BloopDefaults.configSettings)
  )
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.apisubscriptionfields.model._",
      "uk.gov.hmrc.apisubscriptionfields.controller.Binders._"
    )
  )
  .settings(
    libraryDependencies ++= appDependencies,
    dependencyOverrides ++= overrides,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(scoverageSettings)
  .settings(
    fork in Test := false,
    addTestReportOption(Test, "test-reports"),
    parallelExecution in Test := false
  )
  .settings(majorVersion := 0)
  .settings(scalacOptions ++= Seq("-Ypartial-unification"))

lazy val acceptanceTestSettings =
  inConfig(AcceptanceTest)(Defaults.testSettings) ++
    Seq(
      unmanagedSourceDirectories in AcceptanceTest := Seq(baseDirectory.value / "acceptance"),
      fork in AcceptanceTest := false,
      parallelExecution in AcceptanceTest := false,
      addTestReportOption(AcceptanceTest, "acceptance-reports")
    )

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := "<empty>;Reverse.*;.*model.*;.*config.*;.*(AuthService|BuildInfo|Routes).*;.*.application;.*.definition",
  coverageMinimum := 94,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  parallelExecution in Test := false
)

def onPackageName(rootPackage: String): String => Boolean = { testName => testName startsWith rootPackage }

// Note that this task has to be scoped globally
bloopAggregateSourceDependencies in Global := true
