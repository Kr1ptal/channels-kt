import com.android.build.api.dsl.LibraryExtension
import io.kotest.framework.gradle.KotestGradleExtension
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
        namespace = Constants.androidNamespace(project.name)
        compileSdk = Constants.ANDROID_COMPILE_SDK

        defaultConfig {
            minSdk = Constants.ANDROID_MIN_SDK
        }
    }
}

val kotlinCompilerConfig: KotlinCommonCompilerOptions.(Boolean) -> Unit = { isTestTask ->
    val defaultArgs = listOf(
        "-progressive",
        // TODO re-add when this is fixed: https://youtrack.jetbrains.com/issue/KT-78923
        // "-Xbackend-threads=0", // use all available processors
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

fun isTestTask(name: String) = name.contains("test") || name.contains("Test")

pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    pluginManager.apply("com.google.devtools.ksp")
    pluginManager.apply("io.kotest")
    pluginManager.apply("org.jetbrains.kotlinx.atomicfu")

    configure<KotestGradleExtension> {
        customGradleTask.convention(false)
    }

    configure<KotlinMultiplatformExtension> {
        applyDefaultHierarchyTemplate()

        jvm()
        androidTarget {
            publishLibraryVariants("release")
        }
        macosArm64()
        iosArm64()
        iosX64()
        iosSimulatorArm64()

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

        sourceSets {
            val commonMain by getting

            val commonTest by getting {
                dependencies {
                    implementation(libs.findBundle("kotest").get())
                }
            }

            val jvmTest by getting {
                dependencies {
                    implementation(libs.findLibrary("kotest-runner-junit5").get())
                }
            }

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

    tasks.named<Test>("jvmTest") {
        useJUnitPlatform()
    }
}
