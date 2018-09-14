import java.net.URLEncoder

import sbt.util

organization := "com.eng.db"

name := "eng.db.dataaccess"

version := "0.1"

scalaVersion := "2.12.6"

//lazy val projectDir = System.getProperty("user.dir")

lazy val projectDir = sys.props.get("user.dir")

lazy val baseDir = {
    val os = sys.props.get("os.name")
    val localDir = projectDir
    
    val returnValue = (os, localDir) match {
        case (Some(operativeSystem), Some(dir)) if operativeSystem.contains("Windows") =>
            val path = dir.substring(0, dir.lastIndexOf('\\')).replaceAll(" ","%20")
            s"file:$path"
        case (_, Some(dir)) => s"file://${dir.substring(0, dir.lastIndexOf('/'))}"
        case _ => throw new Exception()
    }

    returnValue
}

assemblyOutputPath in assembly := new sbt.File(s"${projectDir.get}/out/artifacts/eng.db.dataaccess.jar")

logLevel in assembly := util.Level.Error

assemblyMergeStrategy in assembly := {
    case "defaults.conf" => MergeStrategy.concat
    case "reference.conf"=> MergeStrategy.concat
    case PathList("META-INF", _ @ _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
    //    case x =>
    //        val oldStrategy = (assemblyMergeStrategy in assembly).value
    //        oldStrategy(x)
}

fork in Test := true
javaOptions in Test += "-Dconfig.file=src/test/resources/defaults.conf"

libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.3",
    "org.scalactic" %% "scalactic" % "3.0.5",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",
    "com.typesafe" % "config" % "1.2.1",

    // Local dependenciesl
    "com.oracle" %% "oracle.jdbc" % "1.8" from s"$baseDir/jars/ojdbc8.jar",
    "com.oracle" %% "oracle.ucp" % "1.8" from s"$baseDir/jars/ucp.jar"
)