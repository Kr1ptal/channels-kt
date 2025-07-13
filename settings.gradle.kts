plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "channels-kt"
include("channels-core")
include("channels-coroutines")
include("channels-bom")