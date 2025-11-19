plugins {
    `project-conventions`
    `maven-publish-conventions`
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":channels-core"))
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}
