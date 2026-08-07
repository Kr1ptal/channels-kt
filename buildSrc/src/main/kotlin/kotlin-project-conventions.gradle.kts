import com.android.build.api.dsl.LibraryExtension
import io.kotest.framework.gradle.KotestGradleExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

repositories {
    google()
    mavenCentral()
}

pluginManager.withPlugin("com.android.library") {
    configure<LibraryExtension> {
        namespace = "io.kriptal.channels.${project.name.replace("-", ".")}"
        compileSdk = 36

        defaultConfig {
            minSdk = 21
        }
    }
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
        alias(libs.plugins.atomicfu)
    }

    configure<KotestGradleExtension> {
        customGradleTask.convention(false)
    }

    configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()

        // Define standard targets
        jvm()
        if (pluginManager.hasPlugin("com.android.library")) {
            androidTarget {
                publishLibraryVariants("release")
            }
        }
        macosArm64()
        iosArm64()           // Physical devices (for release builds)
        iosX64()
        iosSimulatorArm64()  // Simulator for Apple Silicon (enables testing)

        jvmToolchain {
            languageVersion = JavaLanguageVersion.of(Constants.testJavaVersion.majorVersion)
            vendor = JvmVendorSpec.ADOPTIUM
            implementation = JvmImplementation.VENDOR_SPECIFIC
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
            val commonMain by getting

            val commonTest by getting {
                dependencies {
                    implementation(libs.bundles.kotest)
                }
            }

            val jvmTest by getting {
                dependencies {
                    implementation(libs.kotest.runner.junit5)
                }
            }

            if (pluginManager.hasPlugin("com.android.library")) {
                val jvmAndroidMain by creating {
                    dependsOn(commonMain)
                }

                val jvmMain by getting {
                    dependsOn(jvmAndroidMain)
                }

                val androidMain by getting {
                    dependsOn(jvmAndroidMain)
                }
            }
        }
    }

    tasks.named<Test>("jvmTest") {
        useJUnitPlatform()
    }
}
