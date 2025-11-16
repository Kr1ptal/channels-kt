plugins {
    `project-conventions`
    `maven-publish-conventions`
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
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
