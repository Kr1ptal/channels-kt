import org.gradle.api.JavaVersion

object Constants {
    val compileJavaVersion = JavaVersion.VERSION_11
    val testJavaVersion = JavaVersion.VERSION_17

    const val ANDROID_COMPILE_SDK = 36
    const val ANDROID_MIN_SDK = 21

    fun androidNamespace(projectName: String) = "io.kriptal.channels.${projectName.replace("-", ".")}"
}
