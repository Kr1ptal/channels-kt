plugins {
    id("project-conventions")
    id("maven-publish-conventions")
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
