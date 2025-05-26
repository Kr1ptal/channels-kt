plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "channels-kt"
include("channels-core")
include("channels-coroutines")
