import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

repositories {
    mavenCentral()
}

// disable runtime null call and argument checks for improved performance - they're left in tests to catch early bugs
val kotlinCompilerConfig: KotlinCommonCompilerOptions.(Boolean) -> Unit = { isTestTask ->
    val defaultArgs = listOf(
        "-progressive",
        // TODO re-add when this is fixed: https://youtrack.jetbrains.com/issue/KT-78923
        //"-Xbackend-threads=0", // use all available processors
    )

    val specificArgs = if (isTestTask) {
        listOf(
            "-opt-in=kotlin.RequiresOptIn,kotlin.ExperimentalStdlibApi,io.kotest.common.ExperimentalKotest",
        )
    } else {
        listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
        )
    }

    if (this is KotlinJvmCompilerOptions) {
        val version = if (isTestTask) Constants.testJavaVersion else Constants.compileJavaVersion
        jvmTarget = JvmTarget.fromTarget(version.majorVersion)
    }
    freeCompilerArgs.addAll(defaultArgs + specificArgs)
}

// need to do two separate checks for both cases, not ignoring case. Otherwise, we'd get a false positive for "kaptGenera`teSt`ubsKotlin"
fun isTestTask(name: String) = name.contains("test") || name.contains("Test")

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    val libs = the<LibrariesForLibs>()

    plugins {
        alias(libs.plugins.ksp)
        alias(libs.plugins.kotest)
    }

    configure<KotlinMultiplatformExtension> {
        // Define standard targets
        jvm()
        iosArm64()           // Physical devices (for release builds)
        iosSimulatorArm64()  // Simulator for Apple Silicon (enables testing)

        jvmToolchain {
            languageVersion = JavaLanguageVersion.of(Constants.testJavaVersion.majorVersion)
            vendor = JvmVendorSpec.ADOPTIUM
            implementation = JvmImplementation.VENDOR_SPECIFIC
        }

        // disable default KMP test task - we use `kotest` instead
        tasks.matching { it.name == "jvmTest" }.configureEach {
            enabled = false
        }

        targets.configureEach {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions.kotlinCompilerConfig(isTestTask(name))

                    compilerOptions {
                        val isTestTask = name.contains("test", ignoreCase = true)
                        val defaultArgs = listOf(
                            "-progressive",
                            "-Xjvm-default=all",
                            "-Xexpect-actual-classes",
                        )

                        val specificArgs = if (isTestTask) {
                            listOf("-opt-in=kotlin.RequiresOptIn,kotlin.ExperimentalStdlibApi,io.kotest.common.ExperimentalKotest")
                        } else {
                            listOf(
                                "-opt-in=kotlin.RequiresOptIn",
                                "-Xno-param-assertions",
                                "-Xno-call-assertions",
                                "-Xno-receiver-assertions",
                            )
                        }

                        freeCompilerArgs.addAll(defaultArgs + specificArgs)
                    }
                }
            }
        }

        // Configure standard source sets
        sourceSets {
            val commonTest by getting {
                dependencies {
                    implementation(libs.bundles.kotest)
                }
            }
        }
    }
}
