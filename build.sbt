ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "com.company"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := "dand-d-template-parser",

    // ── Dependencies ───────────────────────────────────────────
    libraryDependencies ++= Seq(
      "org.typelevel"  %% "cats-core"                       % "2.12.0",
      "org.typelevel"  %% "cats-effect"                     % "3.5.4",
      // Database
      "org.tpolecat"       %% "doobie-core"                  % "1.0.0-RC6",
      "org.tpolecat"       %% "doobie-hikari"                % "1.0.0-RC6",
      "com.mysql"           % "mysql-connector-j"            % "9.2.0",
      // Testing
      "org.scalatestplus.play" %% "scalatestplus-play"       % "7.0.1"  % Test,
      "org.typelevel"  %% "cats-effect-testing-scalatest"    % "1.5.0"  % Test,
    ),

    // ── Compiler options ───────────────────────────────────────
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Wunused:imports",
      "-Wunused:privates",
      "-Wunused:locals",
    ),

    // Suppress warnings in Play-generated routes sources
    scalacOptions += "-Wconf:src=routes/.*:s",

    // ── Play settings ──────────────────────────────────────────
    PlayKeys.playDefaultPort := 9000,
  )
