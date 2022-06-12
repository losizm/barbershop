organization := "com.github.losizm"
name         := "barbershop"
version      := "0.6.2"
description  := "Example application using Scamper as the web framework"
maintainer   := "carlos.conyers@hotmail.com"

versionScheme := Some("early-semver")

scalaVersion  := "3.1.2"
scalacOptions := Seq("-deprecation", "-feature", "-new-syntax", "-Werror", "-Yno-experimental")

Compile / doc / scalacOptions := Seq(
  "-project", name.value.capitalize,
  "-project-version", version.value,
  "-project-logo", "images/logo.svg"
)

libraryDependencies := Seq(
  "ch.qos.logback"    %  "logback-classic" % "1.2.11",
  "com.github.losizm" %% "grapple"         % "12.0.0",
  "com.github.losizm" %% "little-config"   % "3.0.0",
  "com.github.losizm" %% "little-io"       % "8.0.0",
  "com.github.losizm" %% "scamper"         % "33.0.0",
  "com.typesafe"      %  "config"          % "1.4.1",
  "org.scalatest"     %% "scalatest"       % "3.2.12" % Test
)

enablePlugins(JavaAppPackaging)

Universal / mappings := {
  val universalMappings = (Universal / mappings).value

  universalMappings.filterNot {
    case (_, name) => name.endsWith(".swp")
  }
}

bashScriptExtraDefines += """addJava -Duser.dir="$app_home/../" """
bashScriptExtraDefines += """addJava -Dconfig.file="$app_home/../conf/application.conf" """
bashScriptExtraDefines += """addJava -Dlogback.configurationFile="$app_home/../conf/logback.xml" """

batScriptExtraDefines += """call :add_java -Duser.dir="%APP_HOME%" """
batScriptExtraDefines += """call :add_java -Dconfig.file="%APP_HOME%/conf/application.conf" """
batScriptExtraDefines += """call :add_java -Dlogback.configurationFile="%APP_HOME%/conf/logback.xml" """
