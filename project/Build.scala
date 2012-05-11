import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "hallil"
    val appVersion      = "0.1-SNAPSHOT"

    val appDependencies = Seq(
      "redis.clients" % "jedis" % "2.0.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
