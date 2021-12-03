organization := "com.github.losizm"
name         := "barbershop"
version      := "0.2.0"

scalaVersion  := "3.1.0"
scalacOptions := Seq("-deprecation", "-feature", "-new-syntax", "-Xfatal-warnings", "-Yno-experimental")

Compile / doc / scalacOptions := Seq(
  "-project", name.value.capitalize,
  "-project-version", version.value,
  "-project-logo", "images/logo.svg"
)

libraryDependencies := Seq(
  "ch.qos.logback"    %  "logback-classic" % "1.2.6",
  "com.github.losizm" %% "grapple"         % "9.0.0",
  "com.github.losizm" %% "little-config"   % "2.0.0",
  "com.github.losizm" %% "little-io"       % "6.0.0",
  "com.github.losizm" %% "scamper"         % "28.1.0",
  "com.typesafe"      %  "config"          % "1.4.1",
  "org.scalatest"     %% "scalatest"       % "3.2.10" % Test
)

enablePlugins(JavaAppPackaging)

bashScriptExtraDefines += """addJava -Duser.dir="$app_home/../" """
bashScriptExtraDefines += """addJava -Dconfig.file="$app_home/../conf/application.conf" """
bashScriptExtraDefines += """addJava -Dlogback.configurationFile="$app_home/../conf/logback.xml" """

batScriptExtraDefines += """call :add_java -Duser.dir="%APP_HOME%" """
batScriptExtraDefines += """call :add_java -Dconfig.file="%APP_HOME%/conf/application.conf" """
batScriptExtraDefines += """call :add_java -Dlogback.configurationFile="%APP_HOME%/conf/logback.xml" """