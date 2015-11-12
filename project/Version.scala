import sbt.IO
import java.io.File

object Version {
  val appProperties = {
    val prop = new java.util.Properties
    IO.load(prop, new File("project/version.properties"))
    prop
  }
  val version = appProperties.getProperty("version")
}
